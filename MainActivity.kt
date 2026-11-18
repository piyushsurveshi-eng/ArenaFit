package com.arenafit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arenafit.app.ui.ActivityScreen
import com.arenafit.app.ui.AppViewModel
import com.arenafit.app.ui.ArenaScreen
import com.arenafit.app.ui.CoachScreen
import com.arenafit.app.ui.FeedScreen
import com.arenafit.app.ui.HomeScreen
import com.arenafit.app.ui.ProfileScreen
import com.arenafit.app.ui.ProgressScreen
import com.arenafit.app.ui.TrainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ArenaFitApp() }
    }
}

private data class Tab(val route: String, val label: String, val icon: String)

private val tabs = listOf(
    Tab("home", "Home", "🏠"),
    Tab("train", "Train", "🏋"),
    Tab("progress", "Progress", "📈"),
    Tab("arena", "Arena", "🏆"),
    Tab("feed", "Feed", "💬")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArenaFitApp(vm: AppViewModel = viewModel()) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFFFF7043), secondary = Color(0xFFFFB300))
    } else {
        lightColorScheme(primary = Color(0xFFE64A19), secondary = Color(0xFFFFA000))
    }
    MaterialTheme(colorScheme = scheme) {
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route ?: "home"
        val isTab = tabs.any { it.route == route }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ArenaFit") },
                    navigationIcon = {
                        if (!isTab) TextButton(onClick = { nav.popBackStack() }) { Text("‹ Back") }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Text(t.icon) },
                            label = { Text(t.label) }
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad)) {
                NavHost(nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(vm, onStartWorkout = { nav.navigate("train") { launchSingleTop = true } }, nav = { nav.navigate(it) })
                    }
                    composable("train") { TrainScreen(vm) }
                    composable("progress") { ProgressScreen(vm) }
                    composable("arena") { ArenaScreen(vm) }
                    composable("feed") { FeedScreen(vm) }
                    composable("coach") { CoachScreen(vm) }
                    composable("activity") { ActivityScreen(vm) }
                    composable("profile") { ProfileScreen(vm) }
                }
            }
        }
    }
}
