package com.arenafit.app.domain

import com.arenafit.app.data.SetEntity
import com.arenafit.app.data.UserStatsEntity
import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

// ---------------------------------------------------------------- XP / levels / streaks

object Calc {
    /** Epley one-rep-max estimate. */
    fun est1rm(weightKg: Float, reps: Int): Float = weightKg * (1f + reps / 30f)
    fun today(): Long = LocalDate.now().toEpochDay()
    fun weekStart(epochDay: Long): Long =
        epochDay - (LocalDate.ofEpochDay(epochDay).dayOfWeek.value - 1)
}

object Xp {
    const val WORKOUT_BONUS = 25
    const val PR_BONUS = 20
    const val CHALLENGE_BONUS = 50

    fun forSet(reps: Int, weightKg: Float): Int =
        5 + ((reps * weightKg) / 50f).toInt().coerceAtMost(15)

    fun forActivity(distanceM: Float): Int = (distanceM / 1000f * 20f).toInt()

    fun levelFor(xp: Int): Int = floor(sqrt(xp / 100.0)).toInt() + 1
    fun xpForLevel(level: Int): Int = 100 * (level - 1) * (level - 1)
    fun progress(xp: Int): Float {
        val l = levelFor(xp)
        val a = xpForLevel(l)
        val b = xpForLevel(l + 1)
        return (xp - a).toFloat() / (b - a)
    }

    fun tierFor(level: Int): String = when {
        level < 5 -> "Bronze"
        level < 10 -> "Silver"
        level < 20 -> "Gold"
        else -> "Platinum"
    }
}

object Streaks {
    /** Resets weekly XP when a new week started. */
    fun rollWeek(s: UserStatsEntity, today: Long): UserStatsEntity {
        val ws = Calc.weekStart(today)
        return if (s.weekStartDay != ws) s.copy(weeklyXp = 0, weekStartDay = ws) else s
    }

    /** Awards XP and updates streak + coins. Call once per completed activity. */
    fun award(stats: UserStatsEntity, xp: Int, today: Long): UserStatsEntity {
        var s = rollWeek(stats, today)
        val gap = if (s.lastActiveDay < 0) Long.MAX_VALUE else today - s.lastActiveDay
        s = when {
            gap == 0L -> s
            gap == 1L -> s.copy(streak = s.streak + 1)
            gap == 2L && s.freezes > 0 -> s.copy(streak = s.streak + 1, freezes = s.freezes - 1)
            else -> s.copy(streak = 1)
        }
        return s.copy(
            xp = s.xp + xp,
            weeklyXp = s.weeklyXp + xp,
            coins = s.coins + xp / 10,
            lastActiveDay = today
        )
    }

    /** Streak shown on screen: if the user missed too many days it displays as 0. */
    fun displayStreak(s: UserStatsEntity, today: Long): Int {
        if (s.lastActiveDay < 0) return 0
        val gap = today - s.lastActiveDay
        return when {
            gap <= 1L -> s.streak
            gap == 2L && s.freezes > 0 -> s.streak
            else -> 0
        }
    }
}

// ---------------------------------------------------------------- recovery heat map

object Recovery {
    private const val DECAY_HOURS = 36.0
    private const val SATURATION = 8.0 // weighted sets that mean "fully fatigued"

    /** Returns 0..1 per muscle, 1 = fully recovered. */
    fun compute(sets: List<SetEntity>, now: Long): Map<Muscle, Float> {
        val fatigue = Muscle.values().associateWith { 0.0 }.toMutableMap()
        for (s in sets) {
            val hours = (now - s.ts) / 3_600_000.0
            if (hours < 0 || hours > 24 * 7) continue
            val ex = Exercises.byId(s.exerciseId) ?: continue
            val decay = exp(-hours / DECAY_HOURS)
            fatigue[ex.muscle] = fatigue.getValue(ex.muscle) + 1.0 * decay
            for (m in ex.secondary) fatigue[m] = fatigue.getValue(m) + 0.4 * decay
        }
        return fatigue.mapValues { (1.0 - it.value / SATURATION).coerceIn(0.0, 1.0).toFloat() }
    }
}

// ---------------------------------------------------------------- adaptive generator

data class PlannedExercise(
    val exercise: Exercise,
    val sets: Int,
    val targetReps: Int,
    val suggestedKg: Float
)

data class Plan(val title: String, val focus: List<Muscle>, val items: List<PlannedExercise>)

object WorkoutGenerator {
    private val majors = listOf(
        Muscle.CHEST, Muscle.BACK, Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.SHOULDERS, Muscle.GLUTES
    )
    private val minors = listOf(
        Muscle.BICEPS, Muscle.TRICEPS, Muscle.CALVES, Muscle.CORE, Muscle.FOREARMS
    )

    private fun defaultStart(eq: Equipment): Float = when (eq) {
        Equipment.BARBELL -> 20f
        Equipment.DUMBBELL -> 8f
        Equipment.MACHINE -> 20f
        Equipment.CABLE -> 15f
        Equipment.BODYWEIGHT -> 0f
    }

    private fun roundHalf(x: Float): Float = (x * 2f).roundToInt() / 2f

    /**
     * @param feedback -1 = last workout felt too hard, 0 = neutral, +1 = too easy
     */
    fun generate(
        history: List<SetEntity>,
        equipment: Set<Equipment>,
        minutes: Int,
        feedback: Int,
        now: Long,
        seed: Long
    ): Plan {
        val rec = Recovery.compute(history, now)
        val rnd = Random(seed)

        val ranked = majors.sortedByDescending { rec[it] ?: 1f }
        val ready = ranked.filter { (rec[it] ?: 1f) >= 0.5f }.ifEmpty { ranked }
        val focus = ready.take(if (minutes >= 45) 3 else 2)
        val maxEx = (minutes / 9).coerceIn(3, 8)

        val pool = Exercises.all.filter {
            it.equipment in equipment || it.equipment == Equipment.BODYWEIGHT
        }
        val chosen = mutableListOf<Exercise>()
        fun pick(m: Muscle, compound: Boolean?) {
            val c = pool.filter {
                it.muscle == m && (compound == null || it.compound == compound) && it !in chosen
            }.shuffled(rnd)
            c.firstOrNull()?.let { chosen += it }
        }
        focus.forEach { pick(it, true) }
        focus.forEach { pick(it, false) }
        for (m in minors.filter { (rec[it] ?: 1f) >= 0.6f }.shuffled(rnd)) {
            if (chosen.size >= maxEx) break
            pick(m, null)
        }

        val items = chosen.take(maxEx).mapIndexed { idx, ex ->
            val past = history.filter { it.exerciseId == ex.id }
            val lastWorkoutId = past.lastOrNull()?.workoutId
            val last = past.filter { it.workoutId == lastWorkoutId }
            val targetReps = if (ex.compound) 8 else 12
            val sets = if (idx == 0 && minutes >= 45) 4 else 3

            val top = last.maxByOrNull { it.weightKg }
            var kg = when {
                top == null -> defaultStart(ex.equipment)
                top.reps >= targetReps -> top.weightKg + if (ex.equipment == Equipment.BODYWEIGHT) 0f else 2.5f
                else -> top.weightKg
            }
            if (ex.equipment != Equipment.BODYWEIGHT) {
                if (feedback > 0) kg += 2.5f
                if (feedback < 0) kg *= 0.95f
            }
            PlannedExercise(ex, sets, targetReps, roundHalf(kg))
        }

        val title = focus.joinToString(" + ") { it.label } + " day"
        return Plan(title, focus, items)
    }
}

// ---------------------------------------------------------------- league (simulated)

data class LeagueRow(val name: String, val xp: Int, val isYou: Boolean)

/**
 * Offline stand-in for a server leaderboard. Replace [standings] with an API call:
 * GET /league/current -> list of {name, weeklyXp}. Bots are deterministic per week.
 */
object League {
    private val names = listOf(
        "Aarav", "Maya", "Kabir", "Zoya", "Ishaan", "Nia", "Vihaan", "Tara", "Rohan", "Anya",
        "Dev", "Sana", "Arjun", "Meera", "Veer", "Kiara", "Neil", "Riya", "Om", "Leela",
        "Yash", "Diya", "Karan", "Esha", "Sam", "Pia", "Reyansh", "Ira", "Dhruv"
    )

    fun standings(weekStartDay: Long, myXp: Int): List<LeagueRow> {
        val bots = names.mapIndexed { i, n ->
            LeagueRow(n, Random(weekStartDay * 31 + i).nextInt(40, 700), false)
        }
        return (bots + LeagueRow("You", myXp, true)).sortedByDescending { it.xp }
    }
}

// ---------------------------------------------------------------- arena challenges

data class Challenge(val id: String, val title: String, val rule: String)

object Challenges {
    val all = listOf(
        Challenge("pullups", "Max Pull-Ups", "Strict dead-hang pull-ups, chin over bar, no kipping. One continuous set, full body in frame."),
        Challenge("squats", "Max Bodyweight Squats (60s)", "Hip crease below knee. Film from the side with a visible clock."),
        Challenge("pushups", "Max Push-Ups (60s)", "Chest to a fist-height mark, full lockout. Film from the side."),
        Challenge("plank", "Longest Plank", "Forearm plank, straight line. Timer visible throughout.")
    )
    fun byId(id: String) = all.firstOrNull { it.id == id }
}

// ---------------------------------------------------------------- coach

data class CoachContext(
    val streak: Int,
    val level: Int,
    val recovery: Map<Muscle, Float>,
    val daysSinceLastWorkout: Int?
)

interface Coach {
    suspend fun reply(message: String, ctx: CoachContext): String
}

/**
 * Rule-based coach so the app works offline. To use an LLM, implement [Coach] with an
 * HTTPS call to YOUR backend (never ship an API key inside the APK) and pass [CoachContext]
 * as the system prompt context.
 */
class LocalCoach : Coach {
    override suspend fun reply(message: String, ctx: CoachContext): String {
        val q = message.lowercase()
        val fresh = ctx.recovery.entries.sortedByDescending { it.value }.take(3).joinToString { it.key.label }
        val tired = ctx.recovery.entries.sortedBy { it.value }.take(2).joinToString { it.key.label }
        return when {
            "recover" in q || "sore" in q || "rest" in q ->
                "Most fatigued right now: $tired. Give those 48 hours, sleep 7-9h, and train $fresh instead."
            "protein" in q || "eat" in q || "nutrition" in q || "diet" in q ->
                "A common target is about 1.6-2.2 g of protein per kg of bodyweight daily, spread over 3-4 meals, " +
                    "with carbs around training. A dietitian can tailor this to you."
            "plateau" in q || "stuck" in q || "progress" in q ->
                "Try: add one rep per set before adding weight, take a deload week every 6-8 weeks, and check sleep. " +
                    "Log every set so the generator can progress you."
            "streak" in q || "motivat" in q ->
                "You're on a ${ctx.streak}-day streak at level ${ctx.level}. A short 20-minute session still keeps it alive."
            "sleep" in q ->
                "Aim for a steady bedtime and 7-9 hours. Poor sleep slows recovery, so expect the heat map to stay red longer."
            "warm" in q ->
                "Warm up 5 minutes easy cardio, then 2 light ramp-up sets of your first lift."
            else -> buildString {
                append("Fresh muscles: $fresh. ")
                ctx.daysSinceLastWorkout?.let { append("Last workout was $it day(s) ago. ") }
                append("Ask me about recovery, nutrition, plateaus, sleep or warm-ups.")
            }
        }
    }
}
