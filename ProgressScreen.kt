package com.arenafit.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arenafit.app.domain.Calc
import com.arenafit.app.domain.Exercises
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProgressScreen(vm: AppViewModel) {
    val sets by vm.allSets.collectAsState()
    val workouts by vm.workouts.collectAsState()
    val activities by vm.activities.collectAsState()
    val zone = ZoneId.systemDefault()
    val fmt = DateTimeFormatter.ofPattern("d MMM")

    val loggedIds = sets.map { it.exerciseId }.distinct()
    var selected by remember { mutableStateOf<String?>(null) }
    val current = selected ?: loggedIds.firstOrNull()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard("Totals") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${workouts.size} workouts")
                Text("${(sets.sumOf { (it.reps * it.weightKg).toDouble() } / 1000).let { "%.1f".format(it) }} t lifted")
                Text("${"%.1f".format(activities.sumOf { it.distanceM.toDouble() } / 1000)} km")
            }
        }

        SectionCard("Strength trend (estimated 1RM)") {
            if (loggedIds.isEmpty()) {
                Text("Finish a workout to see charts.")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(loggedIds) { id ->
                        FilterChip(
                            selected = id == current,
                            onClick = { selected = id },
                            label = { Text(Exercises.byId(id)?.name ?: id) }
                        )
                    }
                }
                val byDay = sets.filter { it.exerciseId == current }
                    .groupBy { Instant.ofEpochMilli(it.ts).atZone(zone).toLocalDate() }
                    .entries.sortedBy { it.key.toEpochDay() }
                    .map { e -> e.value.maxOf { Calc.est1rm(it.weightKg, it.reps) } }
                LineChart(byDay)
                byDay.lastOrNull()?.let { Text("Latest: ${"%.1f".format(it)} kg", style = MaterialTheme.typography.bodySmall) }
            }
        }

        SectionCard("Weekly volume (kg)") {
            val today = LocalDate.now()
            val weeks = (7 downTo 0).map { today.minusWeeks(it.toLong()) }
            val values = weeks.map { w ->
                val start = w.with(java.time.DayOfWeek.MONDAY)
                val end = start.plusDays(7)
                sets.filter {
                    val d = Instant.ofEpochMilli(it.ts).atZone(zone).toLocalDate()
                    !d.isBefore(start) && d.isBefore(end)
                }.sumOf { (it.reps * it.weightKg).toDouble() }.toFloat()
            }
            BarChart(values, weeks.map { it.with(java.time.DayOfWeek.MONDAY).format(DateTimeFormatter.ofPattern("d/M")) })
        }

        SectionCard("History") {
            if (workouts.isEmpty()) Text("No workouts yet.")
            workouts.take(30).forEach { w ->
                val date = Instant.ofEpochMilli(w.startedAt).atZone(zone).toLocalDate().format(fmt)
                val mins = ((w.endedAt - w.startedAt) / 60000).coerceAtLeast(1)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$date · ${w.name}")
                    Text("${mins} min · +${w.xp} XP")
                }
            }
        }
    }
}
