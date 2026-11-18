package com.arenafit.app.domain

enum class Muscle(val label: String) {
    CHEST("Chest"), BACK("Back"), SHOULDERS("Shoulders"), BICEPS("Biceps"),
    TRICEPS("Triceps"), QUADS("Quads"), HAMSTRINGS("Hamstrings"), GLUTES("Glutes"),
    CALVES("Calves"), CORE("Core"), FOREARMS("Forearms")
}

enum class Equipment(val label: String) {
    BARBELL("Barbell"), DUMBBELL("Dumbbell"), MACHINE("Machine"),
    CABLE("Cable"), BODYWEIGHT("Bodyweight")
}

data class Exercise(
    val id: String,
    val name: String,
    val muscle: Muscle,
    val equipment: Equipment,
    val compound: Boolean,
    val secondary: List<Muscle> = emptyList()
)

/**
 * Starter library (~60 exercises). Add more here, or load a bigger JSON dataset
 * you have the rights to use. Original names only; no third-party media.
 */
object Exercises {
    private fun e(
        id: String, name: String, m: Muscle, eq: Equipment, compound: Boolean,
        vararg sec: Muscle
    ) = Exercise(id, name, m, eq, compound, sec.toList())

    val all: List<Exercise> = listOf(
        // Chest
        e("bench_press", "Barbell Bench Press", Muscle.CHEST, Equipment.BARBELL, true, Muscle.TRICEPS, Muscle.SHOULDERS),
        e("incline_db_press", "Incline Dumbbell Press", Muscle.CHEST, Equipment.DUMBBELL, true, Muscle.SHOULDERS, Muscle.TRICEPS),
        e("pushup", "Push-Up", Muscle.CHEST, Equipment.BODYWEIGHT, true, Muscle.TRICEPS, Muscle.SHOULDERS),
        e("chest_press_machine", "Machine Chest Press", Muscle.CHEST, Equipment.MACHINE, true, Muscle.TRICEPS),
        e("cable_fly", "Cable Fly", Muscle.CHEST, Equipment.CABLE, false),
        e("db_fly", "Dumbbell Fly", Muscle.CHEST, Equipment.DUMBBELL, false),
        e("dips", "Chest Dip", Muscle.CHEST, Equipment.BODYWEIGHT, true, Muscle.TRICEPS),
        // Back
        e("deadlift", "Conventional Deadlift", Muscle.BACK, Equipment.BARBELL, true, Muscle.HAMSTRINGS, Muscle.GLUTES),
        e("pullup", "Pull-Up", Muscle.BACK, Equipment.BODYWEIGHT, true, Muscle.BICEPS),
        e("barbell_row", "Barbell Row", Muscle.BACK, Equipment.BARBELL, true, Muscle.BICEPS),
        e("db_row", "One-Arm Dumbbell Row", Muscle.BACK, Equipment.DUMBBELL, true, Muscle.BICEPS),
        e("lat_pulldown", "Lat Pulldown", Muscle.BACK, Equipment.CABLE, true, Muscle.BICEPS),
        e("seated_row", "Seated Cable Row", Muscle.BACK, Equipment.CABLE, true, Muscle.BICEPS),
        e("machine_row", "Machine Row", Muscle.BACK, Equipment.MACHINE, true, Muscle.BICEPS),
        e("face_pull", "Face Pull", Muscle.BACK, Equipment.CABLE, false, Muscle.SHOULDERS),
        // Shoulders
        e("ohp", "Overhead Press", Muscle.SHOULDERS, Equipment.BARBELL, true, Muscle.TRICEPS),
        e("db_shoulder_press", "Dumbbell Shoulder Press", Muscle.SHOULDERS, Equipment.DUMBBELL, true, Muscle.TRICEPS),
        e("machine_shoulder_press", "Machine Shoulder Press", Muscle.SHOULDERS, Equipment.MACHINE, true, Muscle.TRICEPS),
        e("lateral_raise", "Lateral Raise", Muscle.SHOULDERS, Equipment.DUMBBELL, false),
        e("cable_lateral_raise", "Cable Lateral Raise", Muscle.SHOULDERS, Equipment.CABLE, false),
        e("rear_delt_fly", "Rear Delt Fly", Muscle.SHOULDERS, Equipment.DUMBBELL, false),
        e("pike_pushup", "Pike Push-Up", Muscle.SHOULDERS, Equipment.BODYWEIGHT, true, Muscle.TRICEPS),
        // Biceps
        e("barbell_curl", "Barbell Curl", Muscle.BICEPS, Equipment.BARBELL, false, Muscle.FOREARMS),
        e("db_curl", "Dumbbell Curl", Muscle.BICEPS, Equipment.DUMBBELL, false, Muscle.FOREARMS),
        e("hammer_curl", "Hammer Curl", Muscle.BICEPS, Equipment.DUMBBELL, false, Muscle.FOREARMS),
        e("cable_curl", "Cable Curl", Muscle.BICEPS, Equipment.CABLE, false),
        e("chinup", "Chin-Up", Muscle.BICEPS, Equipment.BODYWEIGHT, true, Muscle.BACK),
        // Triceps
        e("close_grip_bench", "Close-Grip Bench Press", Muscle.TRICEPS, Equipment.BARBELL, true, Muscle.CHEST),
        e("tricep_pushdown", "Triceps Pushdown", Muscle.TRICEPS, Equipment.CABLE, false),
        e("overhead_ext", "Overhead Triceps Extension", Muscle.TRICEPS, Equipment.DUMBBELL, false),
        e("skull_crusher", "Skull Crusher", Muscle.TRICEPS, Equipment.BARBELL, false),
        e("bench_dip", "Bench Dip", Muscle.TRICEPS, Equipment.BODYWEIGHT, false),
        // Quads
        e("squat", "Back Squat", Muscle.QUADS, Equipment.BARBELL, true, Muscle.GLUTES, Muscle.HAMSTRINGS),
        e("front_squat", "Front Squat", Muscle.QUADS, Equipment.BARBELL, true, Muscle.GLUTES, Muscle.CORE),
        e("goblet_squat", "Goblet Squat", Muscle.QUADS, Equipment.DUMBBELL, true, Muscle.GLUTES),
        e("leg_press", "Leg Press", Muscle.QUADS, Equipment.MACHINE, true, Muscle.GLUTES),
        e("leg_extension", "Leg Extension", Muscle.QUADS, Equipment.MACHINE, false),
        e("lunge", "Walking Lunge", Muscle.QUADS, Equipment.DUMBBELL, true, Muscle.GLUTES),
        e("bw_squat", "Bodyweight Squat", Muscle.QUADS, Equipment.BODYWEIGHT, true, Muscle.GLUTES),
        e("bulgarian_split", "Bulgarian Split Squat", Muscle.QUADS, Equipment.DUMBBELL, true, Muscle.GLUTES),
        // Hamstrings
        e("rdl", "Romanian Deadlift", Muscle.HAMSTRINGS, Equipment.BARBELL, true, Muscle.GLUTES, Muscle.BACK),
        e("db_rdl", "Dumbbell Romanian Deadlift", Muscle.HAMSTRINGS, Equipment.DUMBBELL, true, Muscle.GLUTES),
        e("leg_curl", "Lying Leg Curl", Muscle.HAMSTRINGS, Equipment.MACHINE, false),
        e("nordic_curl", "Nordic Curl", Muscle.HAMSTRINGS, Equipment.BODYWEIGHT, false),
        // Glutes
        e("hip_thrust", "Barbell Hip Thrust", Muscle.GLUTES, Equipment.BARBELL, true, Muscle.HAMSTRINGS),
        e("glute_bridge", "Glute Bridge", Muscle.GLUTES, Equipment.BODYWEIGHT, false, Muscle.HAMSTRINGS),
        e("cable_kickback", "Cable Kickback", Muscle.GLUTES, Equipment.CABLE, false),
        e("step_up", "Dumbbell Step-Up", Muscle.GLUTES, Equipment.DUMBBELL, true, Muscle.QUADS),
        // Calves
        e("standing_calf", "Standing Calf Raise", Muscle.CALVES, Equipment.MACHINE, false),
        e("db_calf", "Dumbbell Calf Raise", Muscle.CALVES, Equipment.DUMBBELL, false),
        e("bw_calf", "Bodyweight Calf Raise", Muscle.CALVES, Equipment.BODYWEIGHT, false),
        // Core
        e("plank", "Plank (reps = seconds/10)", Muscle.CORE, Equipment.BODYWEIGHT, false),
        e("hanging_leg_raise", "Hanging Leg Raise", Muscle.CORE, Equipment.BODYWEIGHT, false),
        e("cable_crunch", "Cable Crunch", Muscle.CORE, Equipment.CABLE, false),
        e("ab_wheel", "Ab Wheel Rollout", Muscle.CORE, Equipment.BODYWEIGHT, false),
        e("russian_twist", "Russian Twist", Muscle.CORE, Equipment.DUMBBELL, false),
        // Forearms
        e("wrist_curl", "Wrist Curl", Muscle.FOREARMS, Equipment.DUMBBELL, false),
        e("farmer_carry", "Farmer's Carry", Muscle.FOREARMS, Equipment.DUMBBELL, true, Muscle.CORE),
        e("reverse_curl", "Reverse Curl", Muscle.FOREARMS, Equipment.BARBELL, false, Muscle.BICEPS)
    )

    private val index = all.associateBy { it.id }
    fun byId(id: String): Exercise? = index[id]
}
