package com.arenafit.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arenafit.app.domain.Exercise
import com.arenafit.app.domain.Exercises
import com.arenafit.app.domain.Muscle
import kotlinx.coroutines.delay

@Composable
fun TrainScreen(vm: AppViewModel) {
    val session by vm.session.collectAsState()
    val summary by vm.summary.collectAsState()

    summary?.let { SummaryDialog(it, vm) }

    if (session == null) TrainHome(vm) else ActiveWorkout(vm, session!!)
}

@Composable
private fun TrainHome(vm: AppViewModel) {
    val templates by vm.templates.collectAsState()
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<Muscle?>(null) }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { vm.startBlank() }, modifier = Modifier.fillMaxWidth()) { Text("Start empty workout") }

        if (templates.isNotEmpty()) {
            Text("Templates", style = MaterialTheme.typography.titleMedium)
            templates.forEach { t ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                t.exerciseIds.split(",").mapNotNull { Exercises.byId(it)?.name }.joinToString(),
                                style = MaterialTheme.typography.bodySmall, maxLines = 2
                            )
                        }
                        TextButton(onClick = { vm.startFromTemplate(t) }) { Text("Start") }
                        TextButton(onClick = { vm.deleteTemplate(t.id) }) { Text("Delete") }
                    }
                }
            }
        }

        Text("Exercise library (${Exercises.all.size})", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Search") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(Muscle.values().toList()) { m ->
                FilterChip(
                    selected = muscle == m,
                    onClick = { muscle = if (muscle == m) null else m },
                    label = { Text(m.label) }
                )
            }
        }
        val list = Exercises.all.filter {
            (muscle == null || it.muscle == muscle) && it.name.contains(query, ignoreCase = true)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(list) { ex ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${ex.muscle.label} · ${ex.equipment.label}" + if (ex.compound) " · compound" else "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkout(vm: AppViewModel, s: ActiveSession) {
    var showPicker by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }
    var restKey by remember { mutableIntStateOf(0) }
    var rest by remember { mutableIntStateOf(0) }

    LaunchedEffect(restKey) {
        if (restKey == 0) return@LaunchedEffect
        rest = 90
        while (rest > 0) {
            delay(1000)
            rest--
        }
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(s.name, style = MaterialTheme.typography.headlineSmall)
        if (rest > 0) {
            Text("Rest ${rest / 60}:${(rest % 60).toString().padStart(2, '0')}", color = MaterialTheme.colorScheme.primary)
        }

        s.items.forEachIndexed { idx, item ->
            ExerciseCard(
                item = item,
                onLog = { reps, kg -> vm.logSet(idx, reps, kg); restKey++ },
                onDeleteSet = { vm.deleteSet(idx, it) },
                onRemove = { vm.removeExercise(idx) }
            )
        }

        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Add exercise") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { vm.discardSession() }, modifier = Modifier.weight(1f)) { Text("Discard") }
            Button(onClick = { showFinish = true }, modifier = Modifier.weight(1f)) { Text("Finish") }
        }
    }

    if (showPicker) {
        ExercisePicker(onPick = { vm.addExercise(it); showPicker = false }, onDismiss = { showPicker = false })
    }
    if (showFinish) {
        var share by remember { mutableStateOf(true) }
        var tplName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFinish = false },
            title = { Text("Finish workout?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        FilterChip(selected = share, onClick = { share = !share }, label = { Text("Share to feed") })
                    }
                    OutlinedTextField(
                        value = tplName, onValueChange = { tplName = it },
                        label = { Text("Save as template (optional name)") }, singleLine = true
                    )
                    TextButton(onClick = { vm.saveSessionAsTemplate(tplName.ifBlank { s.name }) }) {
                        Text("Save template now")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFinish = false; vm.finishSession(share) }) { Text("Finish") }
            },
            dismissButton = { TextButton(onClick = { showFinish = false }) { Text("Keep going") } }
        )
    }
}

@Composable
private fun ExerciseCard(
    item: ActiveExercise,
    onLog: (Int, Float) -> Unit,
    onDeleteSet: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val lastSet = item.sets.lastOrNull()
    var reps by remember(item.exercise.id, item.sets.size) {
        mutableStateOf((lastSet?.reps ?: item.targetReps).toString())
    }
    var kg by remember(item.exercise.id, item.sets.size) {
        mutableStateOf(trimKg(lastSet?.weightKg ?: item.suggestedKg))
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Target ${item.plannedSets}×${item.targetReps}" +
                            if (item.suggestedKg > 0f) " @ ${trimKg(item.suggestedKg)} kg" else "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = onRemove) { Text("Remove") }
            }
            item.sets.forEachIndexed { i, set ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Set ${i + 1}: ${set.reps} × ${trimKg(set.weightKg)} kg")
                    Text("✕", modifier = Modifier.clickable { onDeleteSet(i) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = reps, onValueChange = { reps = it.filter(Char::isDigit) },
                    label = { Text("Reps") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = kg, onValueChange = { kg = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("kg") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = {
                    val r = reps.toIntOrNull() ?: 0
                    val w = kg.toFloatOrNull() ?: 0f
                    if (r > 0) onLog(r, w)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Log set") }
        }
    }
}

@Composable
private fun ExercisePicker(onPick: (Exercise) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    label = { Text("Search") }, singleLine = true
                )
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(Exercises.all.filter { it.name.contains(query, ignoreCase = true) }) { ex ->
                        Text(
                            "${ex.name}  (${ex.muscle.label})",
                            modifier = Modifier.fillMaxWidth().clickable { onPick(ex) }.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun SummaryDialog(s: WorkoutSummary, vm: AppViewModel) {
    AlertDialog(
        onDismissRequest = { vm.dismissSummary() },
        title = { Text(if (s.levelUp) "Level up! 🎉" else "Workout complete") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("+${s.xp} XP")
                Text("${s.sets} sets · ${s.volumeKg.toInt()} kg volume")
                if (s.prs.isNotEmpty()) Text("New PRs: ${s.prs.joinToString()}")
            }
        },
        confirmButton = { TextButton(onClick = { vm.dismissSummary() }) { Text("Nice") } }
    )
}

fun trimKg(v: Float): String = if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()
