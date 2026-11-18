package com.arenafit.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startedAt: Long,
    val endedAt: Long,
    val xp: Int
)

@Entity(tableName = "sets", indices = [Index("workoutId"), Index("exerciseId")])
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: String,
    val reps: Int,
    val weightKg: Float,
    val ts: Long
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** comma separated exercise ids */
    val exerciseIds: String
)

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val startedAt: Long,
    val distanceM: Float,
    val durationS: Long,
    val xp: Int
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val xp: Int = 0,
    val weeklyXp: Int = 0,
    /** epoch day of the Monday of the week weeklyXp belongs to */
    val weekStartDay: Long = 0,
    val streak: Int = 0,
    val lastActiveDay: Long = -1,
    val freezes: Int = 1,
    val coins: Int = 50,
    val isPro: Boolean = false
)

@Entity(tableName = "submissions")
data class SubmissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val challengeId: String,
    val videoUri: String,
    val claimedReps: Int,
    /** PENDING, APPROVED, REJECTED */
    val status: String,
    val ts: Long
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val ts: Long
)

@Dao
interface AppDao {
    @Insert suspend fun insertWorkout(w: WorkoutEntity): Long
    @Insert suspend fun insertSets(s: List<SetEntity>)
    @Query("SELECT * FROM workouts ORDER BY startedAt DESC") fun workouts(): Flow<List<WorkoutEntity>>
    @Query("SELECT * FROM sets ORDER BY ts ASC") fun allSets(): Flow<List<SetEntity>>

    @Insert suspend fun insertTemplate(t: TemplateEntity): Long
    @Query("SELECT * FROM templates ORDER BY id DESC") fun templates(): Flow<List<TemplateEntity>>
    @Query("DELETE FROM templates WHERE id = :id") suspend fun deleteTemplate(id: Long)

    @Insert suspend fun insertActivity(a: ActivityEntity): Long
    @Query("SELECT * FROM activities ORDER BY startedAt DESC") fun activities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM user_stats WHERE id = 1") fun stats(): Flow<UserStatsEntity?>
    @Query("SELECT * FROM user_stats WHERE id = 1") suspend fun statsOnce(): UserStatsEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertStats(s: UserStatsEntity)

    @Insert suspend fun insertSubmission(s: SubmissionEntity): Long
    @Query("SELECT * FROM submissions ORDER BY ts DESC") fun submissions(): Flow<List<SubmissionEntity>>
    @Query("SELECT * FROM submissions WHERE id = :id") suspend fun submissionById(id: Long): SubmissionEntity?
    @Query("UPDATE submissions SET status = :status WHERE id = :id")
    suspend fun setSubmissionStatus(id: Long, status: String)

    @Insert suspend fun insertPost(p: PostEntity): Long
    @Query("SELECT * FROM posts ORDER BY ts DESC") fun posts(): Flow<List<PostEntity>>
}

@Database(
    entities = [
        WorkoutEntity::class, SetEntity::class, TemplateEntity::class, ActivityEntity::class,
        UserStatsEntity::class, SubmissionEntity::class, PostEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "arenafit.db"
            ).build().also { instance = it }
        }
    }
}
