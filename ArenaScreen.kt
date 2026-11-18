package com.arenafit.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arenafit.app.domain.Calc
import com.arenafit.app.domain.Challenge
import com.arenafit.app.domain.Challenges
import com.arenafit.app.domain.League
import com.arenafit.app.domain.Xp

@Composable
fun ArenaScreen(vm: AppViewModel) {
    val stats by vm.stats.collectAsState()
    val subs by vm.submissions.collectAsState()
    var target by remember { mutableStateOf<Challenge?>(null) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var reps by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedUri = uri
    }

    val s = stats
    val weekStart = Calc.weekStart(Calc.today())
    val myWeekly = if (s != null && s.weekStartDay == weekStart) s.weeklyXp else 0
    val rows = League.standings(weekStart, myWeekly)
    val myRank = rows.indexOfFirst { it.isYou } + 1
    val tier = Xp.tierFor(Xp.levelFor(s?.xp ?: 0))

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard("$tier League · you are #$myRank of ${rows.size}") {
            Text(
                "Top 5 promote, bottom 5 demote on Sunday. (Demo standings; connect a backend for real rivals.)",
                style = MaterialTheme.typography.bodySmall
            )
            rows.take(10).forEachIndexed { i, r ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${i + 1}. ${r.name}",
                        fontWeight = if (r.isYou) FontWeight.Bold else FontWeight.Normal
                    )
                    Text("${r.xp} XP", fontWeight = if (r.isYou) FontWeight.Bold else FontWeight.Normal)
                }
            }
            if (myRank > 10) Text("…  $myRank. You — $myWeekly XP", fontWeight = FontWeight.Bold)
        }

        Text("Arena challenges", style = MaterialTheme.typography.titleMedium)
        Challenges.all.forEach { c ->
            SectionCard(c.title) {
                Text(c.rule, style = MaterialTheme.typography.bodySmall)
                Button(onClick = { target = c; pickedUri = null; reps = "" }) { Text("Submit video") }
            }
        }

        SectionCard("My submissions") {
            if (subs.isEmpty()) Text("Nothing submitted yet.")
            subs.forEach { sub ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${Challenges.byId(sub.challengeId)?.title ?: sub.challengeId}: ${sub.claimedReps}")
                    Text(sub.status)
                }
                if (sub.status == "PENDING") {
                    TextButton(onClick = { vm.demoApprove(sub.id) }) { Text("Demo: simulate referee approval") }
                }
            }
        }
    }

    val t = target
    if (t != null) {
        AlertDialog(
            onDismissRequest = { target = null },
            title = { Text(t.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch("video/*") }) {
                        Text(if (pickedUri == null) "Choose video" else "Video selected ✓")
                    }
                    OutlinedTextField(
                        value = reps, onValueChange = { reps = it.filter(Char::isDigit) },
                        label = { Text("Your result (reps / seconds)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pickedUri != null && (reps.toIntOrNull() ?: 0) > 0,
                    onClick = {
                        vm.submitChallenge(t.id, pickedUri.toString(), reps.toInt())
                        target = null
                    }
                ) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { target = null }) { Text("Cancel") } }
        )
    }
}
