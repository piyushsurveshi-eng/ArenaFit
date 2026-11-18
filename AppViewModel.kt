package com.arenafit.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arenafit.app.data.ActivityEntity
import com.arenafit.app.data.AppDatabase
import com.arenafit.app.data.PostEntity
import com.arenafit.app.data.SetEntity
import com.arenafit.app.data.SubmissionEntity
import com.arenafit.app.data.TemplateEntity
import com.arenafit.app.data.UserStatsEntity
import com.arenafit.app.data.WorkoutEntity
import com.arenafit.app.domain.Calc
import com.arenafit.app.domain.Coach
import com.arenafit.app.domain.CoachContext
import com.arenafit.app.domain.Equipment
import com.arenafit.app.domain.Exercise
import com.arenafit.app.domain.Exercises
import com.arenafit.app.domain.LocalCoach
import com.arenafit.app.domain.Plan
import com.arenafit.app.domain.Recovery
import com.arenafit.app.domain.Streaks
import com.arenafit.app.domain.WorkoutGenerator
import com.arenafit.app.domain.Xp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoggedSet(val reps: Int, val weightKg: Float)

data class ActiveExercise(
    val exercise: Exercise,
    val sets: List<LoggedSet> = emptyList(),
    val targetReps: Int = 10,
    val suggestedKg: Float = 0f,
    val plannedSets: Int = 3
)

data class ActiveSession(
    val name: String,
    val startedAt: Long,
    val items: List<ActiveExercise>
)

data class WorkoutSummary(
    val xp: Int,
    val sets: Int,
    val volumeKg: Float,
    val prs: List<String>,
    val levelUp: Boolean,
    val sessionName: String,
    val exerciseIds: List<String>
)

data class ChatMessage(val fromUser: Boolean, val text: String)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val coach: Coach = LocalCoach()

    private fun <T> kotlinx.coroutines.flow.Flow<T>.hot(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    val stats: StateFlow<UserStatsEntity?> = dao.stats().hot(null)
    val workouts = dao.workouts().hot(emptyList())
    val allSets = dao.allSets().hot(emptyList())
    val templates = dao.templates().hot(emptyList())
    val activities = dao.activities().hot(emptyList())
    val submissions = dao.submissions().hot(emptyList())
    val posts = dao.posts().hot(emptyList())

    // Generator settings
    val equipment = MutableStateFlow(Equipment.values().toSet())
    val minutes = MutableStateFlow(45)
    val plan = MutableStateFlow<Plan?>(null)

    // Active workout
    val session = MutableStateFlow<ActiveSession?>(null)
    val summary = MutableStateFlow<WorkoutSummary?>(null)

    // Coach chat
    val chat = MutableStateFlow(
        listOf(ChatMessage(false, "Hi! Ask me about recovery, nutrition, plateaus, sleep or warm-ups."))
    )

    init {
        viewModelScope.launch {
            if (dao.statsOnce() == null) dao.upsertStats(UserStatsEntity())
        }
    }

    // ------------------------------------------------------------ plan generation

    fun generatePlan(feedback: Int = 0) {
        val now = System.currentTimeMillis()
        plan.value = WorkoutGenerator.generate(
            history = allSets.value,
            equipment = equipment.value,
            minutes = minutes.value,
            feedback = feedback,
            now = now,
            seed = now / 60_000L
        )
    }

    fun toggleEquipment(e: Equipment) = equipment.update { cur ->
        if (e in cur) (if (cur.size > 1) cur - e else cur) else cur + e
    }

    fun recoveryMap() = Recovery.compute(allSets.value, System.currentTimeMillis())

    // ------------------------------------------------------------ active workout

    fun startFromPlan(p: Plan) {
        session.value = ActiveSession(
            name = p.title,
            startedAt = System.currentTimeMillis(),
            items = p.items.map {
                ActiveExercise(it.exercise, emptyList(), it.targetReps, it.suggestedKg, it.sets)
            }
        )
    }

    fun startBlank() {
        session.value = ActiveSession("Custom workout", System.currentTimeMillis(), emptyList())
    }

    fun startFromTemplate(t: TemplateEntity) {
        val items = t.exerciseIds.split(",").mapNotNull { Exercises.byId(it) }.map { ex ->
            val last = lastTopSet(ex.id)
            ActiveExercise(
                ex, emptyList(), if (ex.compound) 8 else 12,
                last?.weightKg ?: 0f
            )
        }
        session.value = ActiveSession(t.name, System.currentTimeMillis(), items)
    }

    private fun lastTopSet(exerciseId: String): SetEntity? {
        val past = allSets.value.filter { it.exerciseId == exerciseId }
        val wid = past.lastOrNull()?.workoutId ?: return null
        return past.filter { it.workoutId == wid }.maxByOrNull { it.weightKg }
    }

    fun addExercise(ex: Exercise) = session.update { s ->
        s?.copy(
            items = s.items + ActiveExercise(
                ex, emptyList(), if (ex.compound) 8 else 12, lastTopSet(ex.id)?.weightKg ?: 0f
            )
        )
    }

    fun removeExercise(index: Int) = session.update { s ->
        s?.copy(items = s.items.filterIndexed { i, _ -> i != index })
    }

    fun logSet(index: Int, reps: Int, kg: Float) = session.update { s ->
        s?.copy(items = s.items.mapIndexed { i, it ->
            if (i == index) it.copy(sets = it.sets + LoggedSet(reps, kg)) else it
        })
    }

    fun deleteSet(exIndex: Int, setIndex: Int) = session.update { s ->
        s?.copy(items = s.items.mapIndexed { i, it ->
            if (i == exIndex) it.copy(sets = it.sets.filterIndexed { j, _ -> j != setIndex }) else it
        })
    }

    fun discardSession() {
        session.value = null
    }

    fun saveSessionAsTemplate(name: String) {
        val s = session.value ?: return
        if (s.items.isEmpty()) return
        viewModelScope.launch {
            dao.insertTemplate(
                TemplateEntity(name = name.ifBlank { s.name }, exerciseIds = s.items.joinToString(",") { it.exercise.id })
            )
        }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { dao.deleteTemplate(id) }
    }

    fun finishSession(share: Boolean) {
        val s = session.value ?: return
        if (s.items.all { it.sets.isEmpty() }) {
            session.value = null
            return
        }
        viewModelScope.launch {
            val prior = allSets.value
            val now = System.currentTimeMillis()
            var xp = Xp.WORKOUT_BONUS
            var setCount = 0
            var volume = 0f
            val prs = mutableListOf<String>()

            for (item in s.items) {
                val prevBest = prior.filter { it.exerciseId == item.exercise.id }
                    .maxOfOrNull { Calc.est1rm(it.weightKg, it.reps) } ?: 0f
                val best = item.sets.maxOfOrNull { Calc.est1rm(it.weightKg, it.reps) } ?: 0f
                if (item.sets.isNotEmpty() && prevBest > 0f && best > prevBest) {
                    prs += item.exercise.name
                    xp += Xp.PR_BONUS
                }
                for (set in item.sets) {
                    xp += Xp.forSet(set.reps, set.weightKg)
                    setCount++
                    volume += set.reps * set.weightKg
                }
            }

            val workoutId = dao.insertWorkout(WorkoutEntity(name = s.name, startedAt = s.startedAt, endedAt = now, xp = xp))
            val rows = s.items.flatMap { item ->
                item.sets.mapIndexed { i, set ->
                    SetEntity(
                        workoutId = workoutId, exerciseId = item.exercise.id,
                        reps = set.reps, weightKg = set.weightKg, ts = now + i
                    )
                }
            }
            dao.insertSets(rows)

            val before = dao.statsOnce() ?: UserStatsEntity()
            val after = Streaks.award(before, xp, Calc.today())
            dao.upsertStats(after)

            if (share) {
                val prText = if (prs.isEmpty()) "" else " New PR: ${prs.joinToString()}."
                dao.insertPost(
                    PostEntity(
                        text = "Finished ${s.name}: $setCount sets, ${volume.toInt()} kg total.$prText +$xp XP",
                        ts = now
                    )
                )
            }

            summary.value = WorkoutSummary(
                xp = xp, sets = setCount, volumeKg = volume, prs = prs,
                levelUp = Xp.levelFor(after.xp) > Xp.levelFor(before.xp),
                sessionName = s.name,
                exerciseIds = s.items.map { it.exercise.id }
            )
            session.value = null
            plan.value = null
        }
    }

    fun dismissSummary() {
        summary.value = null
    }

    // ------------------------------------------------------------ cardio

    fun saveActivity(type: String, distanceM: Float, durationS: Long) {
        viewModelScope.launch {
            val xp = Xp.forActivity(distanceM).coerceAtLeast(if (durationS > 60) 5 else 0)
            dao.insertActivity(
                ActivityEntity(
                    type = type, startedAt = System.currentTimeMillis() - durationS * 1000,
                    distanceM = distanceM, durationS = durationS, xp = xp
                )
            )
            val before = dao.statsOnce() ?: UserStatsEntity()
            dao.upsertStats(Streaks.award(before, xp, Calc.today()))
        }
    }

    // ------------------------------------------------------------ arena

    fun submitChallenge(challengeId: String, videoUri: String, reps: Int) {
        viewModelScope.launch {
            dao.insertSubmission(
                SubmissionEntity(
                    challengeId = challengeId, videoUri = videoUri,
                    claimedReps = reps, status = "PENDING", ts = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * DEMO ONLY. In production a referee/moderation backend sets the status; the client
     * must never be able to approve its own submission.
     */
    fun demoApprove(id: Long) {
        viewModelScope.launch {
            val sub = dao.submissionById(id) ?: return@launch
            if (sub.status != "PENDING") return@launch
            dao.setSubmissionStatus(id, "APPROVED")
            val before = dao.statsOnce() ?: UserStatsEntity()
            dao.upsertStats(Streaks.award(before, Xp.CHALLENGE_BONUS, Calc.today()))
        }
    }

    // ------------------------------------------------------------ social

    fun post(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { dao.insertPost(PostEntity(text = text.trim(), ts = System.currentTimeMillis())) }
    }

    // ------------------------------------------------------------ coach

    fun askCoach(message: String) {
        if (message.isBlank()) return
        chat.update { it + ChatMessage(true, message) }
        viewModelScope.launch {
            val s = stats.value
            val lastTs = workouts.value.firstOrNull()?.endedAt
            val days = lastTs?.let { ((System.currentTimeMillis() - it) / 86_400_000L).toInt() }
            val ctx = CoachContext(
                streak = s?.let { Streaks.displayStreak(it, Calc.today()) } ?: 0,
                level = Xp.levelFor(s?.xp ?: 0),
                recovery = recoveryMap(),
                daysSinceLastWorkout = days
            )
            val reply = coach.reply(message, ctx)
            chat.update { it + ChatMessage(false, reply) }
        }
    }

    // ------------------------------------------------------------ store (coins, pro)

    fun buyStreakFreeze(): Boolean {
        val s = stats.value ?: return false
        if (s.coins < 30) return false
        viewModelScope.launch {
            val cur = dao.statsOnce() ?: return@launch
            if (cur.coins >= 30) dao.upsertStats(cur.copy(coins = cur.coins - 30, freezes = cur.freezes + 1))
        }
        return true
    }

    /** DEMO toggle. Replace with Google Play Billing entitlement checks. */
    fun setPro(enabled: Boolean) {
        viewModelScope.launch {
            val cur = dao.statsOnce() ?: UserStatsEntity()
            dao.upsertStats(cur.copy(isPro = enabled))
        }
    }
}
