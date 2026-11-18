package com.arenafit.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val sampleCommunity = listOf(
    "Maya: Hit a 100 kg squat today after 4 months. Streak: 23 days 🔥",
    "Kabir: Shared my Push/Pull/Legs template. Steal it!",
    "Zoya: Morning 5 km run done. Weekly XP arena, here I come."
)

@Composable
fun FeedScreen(vm: AppViewModel) {
    val posts by vm.posts.collectAsState()
    var text by remember { mutableStateOf("") }
    val fmt = DateTimeFormatter.ofPattern("d MMM, HH:mm")
    val zone = ZoneId.systemDefault()

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            label = { Text("Share progress or a PR") }, modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { vm.post(text); text = "" }, modifier = Modifier.fillMaxWidth()) { Text("Post") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(posts) { p ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("You · " + Instant.ofEpochMilli(p.ts).atZone(zone).format(fmt), style = MaterialTheme.typography.labelSmall)
                        Text(p.text)
                    }
                }
            }
            items(sampleCommunity) { t ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Community (sample)", style = MaterialTheme.typography.labelSmall)
                        Text(t)
                    }
                }
            }
        }
    }
}

@Composable
fun CoachScreen(vm: AppViewModel) {
    val chat by vm.chat.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(chat.size) { if (chat.isNotEmpty()) listState.animateScrollToItem(chat.size - 1) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(chat) { m ->
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        (if (m.fromUser) "You: " else "Coach: ") + m.text,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text("Ask your coach") }, modifier = Modifier.weight(1f), singleLine = true
            )
            Button(onClick = { vm.askCoach(text); text = "" }) { Text("Send") }
        }
    }
}
