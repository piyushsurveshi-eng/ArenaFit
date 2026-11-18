package com.arenafit.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.arenafit.app.domain.Calc
import com.arenafit.app.domain.Equipment
import com.arenafit.app.domain.Streaks
import com.arenafit.app.domain.Xp
import com.arenafit.app.gps.GpsTracker
import kotlinx.coroutines.delay

@Composable
fun ActivityScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val tracker = remember { GpsTracker(context) }
    val distance by tracker.distanceM.collectAsState()
    val points by tracker.points.collectAsState()
    val past by vm.activities.collectAsState()

    var type by remember { mutableStateOf("Run") }
    var running by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }

    fun begin() {
        tracker.start()
        elapsed = 0
        running = true
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) begin()
    }

    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            elapsed++
        }
    }
    DisposableEffect(Unit) { onDispose { tracker.stop() } }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Walk", "Run").forEach {
                FilterChip(selected = type == it, onClick = { if (!running) type = it }, label = { Text(it) })
            }
        }
        SectionCard("$type") {
            val km = distance / 1000f
            val pace = if (km > 0.05f) (elapsed / 60f) / km else 0f
            Text("%.2f km".format(km), style = MaterialTheme.typography.displaySmall)
            Text("Time %d:%02d   Pace %s".format(elapsed / 60, elapsed % 60,
                if (pace > 0) "%d:%02d /km".format(pace.toInt(), ((pace % 1) * 60).toInt()) else "--"))
            RouteCanvas(points)
        }
        if (!running) {
            Button(onClick = {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) begin() else permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }, modifier = Modifier.fillMaxWidth()) { Text("Start") }
        } else {
            Button(onClick = {
                running = false
                tracker.stop()
                vm.saveActivity(type, distance, elapsed)
            }, modifier = Modifier.fillMaxWidth()) { Text("Stop & save") }
        }

        SectionCard("Past activities") {
            if (past.isEmpty()) Text("None yet.")
            past.take(15).forEach {
                Text("${it.type} · ${"%.2f".format(it.distanceM / 1000f)} km · ${it.durationS / 60} min · +${it.xp} XP")
            }
        }
    }
}

@Composable
fun ProfileScreen(vm: AppViewModel) {
    val stats by vm.stats.collectAsState()
    val equipment by vm.equipment.collectAsState()
    val s = stats
    var msg by remember { mutableStateOf("") }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (s != null) {
            SectionCard("You") {
                Text("Level ${Xp.levelFor(s.xp)} (${Xp.tierFor(Xp.levelFor(s.xp))}) · ${s.xp} XP")
                Text("Streak ${Streaks.displayStreak(s, Calc.today())} days · ${s.freezes} freeze(s) · ${s.coins} coins")
            }
            SectionCard("Store") {
                OutlinedButton(onClick = {
                    msg = if (vm.buyStreakFreeze()) "Streak Freeze bought" else "Need 30 coins"
                }) { Text("Buy Streak Freeze (30 🪙)") }
                if (msg.isNotEmpty()) Text(msg, style = MaterialTheme.typography.bodySmall)
            }
            SectionCard("Pro (demo switch)") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Unlimited adaptive programs + AI coach")
                    Switch(checked = s.isPro, onCheckedChange = { vm.setPro(it) })
                }
                Text(
                    "Wire this to Google Play Billing before release.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        SectionCard("My equipment") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Equipment.values().take(3).forEach {
                    FilterChip(selected = it in equipment, onClick = { vm.toggleEquipment(it) }, label = { Text(it.label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Equipment.values().drop(3).forEach {
                    FilterChip(selected = it in equipment, onClick = { vm.toggleEquipment(it) }, label = { Text(it.label) })
                }
            }
            Text("The workout generator only picks exercises you can do.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
