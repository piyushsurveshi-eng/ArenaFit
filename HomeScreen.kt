package com.arenafit.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arenafit.app.domain.Calc
import com.arenafit.app.domain.Recovery
import com.arenafit.app.domain.Streaks
import com.arenafit.app.domain.Xp

@Composable
fun HomeScreen(vm: AppViewModel, onStartWorkout: () -> Unit, nav: (String) -> Unit) {
    val stats by vm.stats.collectAsState()
    val sets by vm.allSets.collectAsState()
    val plan by vm.plan.collectAsState()
    val minutes by vm.minutes.collectAsState()
    val s = stats
    val recovery = Recovery.compute(sets, System.currentTimeMillis())

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (s != null) {
            val level = Xp.levelFor(s.xp)
            SectionCard("Level $level · ${Xp.tierFor(level)}") {
                LinearProgressIndicator(progress = { Xp.progress(s.xp) }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${s.xp} XP")
                    Text("🔥 ${Streaks.displayStreak(s, Calc.today())} day streak")
                    Text("🪙 ${s.coins}")
                }
                Text("❄ ${s.freezes} streak freeze(s)", style = MaterialTheme.typography.bodySmall)
            }
        }

        SectionCard("Today's workout") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 45, 60).forEach { m ->
                    FilterChip(
                        selected = minutes == m,
                        onClick = { vm.minutes.value = m },
                        label = { Text("$m min") }
                    )
                }
            }
            val p = plan
            if (p == null) {
                Button(onClick = { vm.generatePlan() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Build my workout")
                }
            } else {
                Text(p.title, style = MaterialTheme.typography.titleSmall)
                p.items.forEach {
                    val w = if (it.suggestedKg > 0f) " @ ${it.suggestedKg} kg" else ""
                    Text("• ${it.exercise.name}  ${it.sets}×${it.targetReps}$w")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.generatePlan(+1) }) { Text("Too easy") }
                    OutlinedButton(onClick = { vm.generatePlan(-1) }) { Text("Too hard") }
                    OutlinedButton(onClick = { vm.generatePlan(0) }) { Text("Reroll") }
                }
                Button(
                    onClick = { vm.startFromPlan(p); onStartWorkout() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Start workout") }
            }
        }

        SectionCard("Muscle recovery") {
            RecoveryMap(recovery)
            Text(
                "Green = ready to train, red = still recovering.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { nav("activity") }, modifier = Modifier.weight(1f)) { Text("Walk / Run") }
            OutlinedButton(onClick = { nav("coach") }, modifier = Modifier.weight(1f)) { Text("Coach") }
            OutlinedButton(onClick = { nav("profile") }, modifier = Modifier.weight(1f)) { Text("Profile") }
        }
    }
}
