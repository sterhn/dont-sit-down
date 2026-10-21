package com.dontsitdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dontsitdown.MainActivity
import com.dontsitdown.app
import com.dontsitdown.chain.Phase
import com.dontsitdown.ui.chain.ChainScreen
import com.dontsitdown.ui.components.Rule
import com.dontsitdown.ui.onboarding.OnboardingScreen
import com.dontsitdown.ui.settings.SettingsScreen
import com.dontsitdown.ui.single.SingleTaskScreen
import com.dontsitdown.ui.summary.WeeklySummaryScreen
import com.dontsitdown.ui.tasks.AddEditTaskScreen
import com.dontsitdown.ui.tasks.TaskDetailScreen
import com.dontsitdown.ui.tasks.TaskListScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val CHAIN = "chain"
    const val TASKS = "tasks"
    const val TASK = "task/{id}"
    const val EDIT = "edit?id={id}"
    const val SINGLE = "single"
    const val WEEK = "week"
    const val SETTINGS = "settings"

    fun task(id: String) = "task/$id"
    fun edit(id: String? = null) = if (id == null) "edit" else "edit?id=$id"
}

private val tabs = listOf(Routes.CHAIN to "chain", Routes.TASKS to "tasks", Routes.WEEK to "week", Routes.SETTINGS to "settings")

@Composable
fun AppNav(onboarded: Boolean, openTarget: String?, onTargetConsumed: () -> Unit) {
    val nav = rememberNavController()
    val session by LocalContext.current.app.session.state.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    LaunchedEffect(openTarget, onboarded) {
        if (!onboarded || openTarget == null) return@LaunchedEffect
        when (openTarget) {
            MainActivity.OPEN_CHAIN -> nav.switchTab(Routes.CHAIN)
            MainActivity.OPEN_SUMMARY -> nav.switchTab(Routes.WEEK)
        }
        onTargetConsumed()
    }

    // Mid-chain, the chain screen is the whole world: no tab bar to wander off with.
    val focused = route == Routes.CHAIN && session?.phase.let { it != null && it != Phase.READY }
    val showTabs = onboarded && route in tabs.map { it.first } && !focused

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.weight(1f)) {
            NavHost(nav, startDestination = if (onboarded) Routes.CHAIN else Routes.ONBOARDING) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onDone = {
                        nav.navigate(Routes.CHAIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    })
                }
                composable(Routes.CHAIN) {
                    Box(Modifier.statusBarsPadding().then(if (focused) Modifier.navigationBarsPadding() else Modifier)) {
                        ChainScreen(onJustOne = { nav.navigate(Routes.SINGLE) })
                    }
                }
                composable(Routes.TASKS) {
                    TaskListScreen(onOpen = { nav.navigate(Routes.task(it)) }, onAdd = { nav.navigate(Routes.edit()) })
                }
                composable(Routes.TASK, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    TaskDetailScreen(
                        id = it.arguments?.getString("id")!!,
                        onBack = { nav.popBackStack() },
                        onEdit = { id -> nav.navigate(Routes.edit(id)) },
                        onStart = { nav.switchTab(Routes.CHAIN) },
                    )
                }
                composable(Routes.EDIT, arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true })) {
                    AddEditTaskScreen(
                        id = it.arguments?.getString("id"),
                        onBack = { nav.popBackStack() },
                        onDeleted = { nav.popBackStack(Routes.TASKS, inclusive = false) },
                    )
                }
                composable(Routes.SINGLE) {
                    SingleTaskScreen(onBack = { nav.popBackStack() }, onStarted = { nav.switchTab(Routes.CHAIN) })
                }
                composable(Routes.WEEK) { WeeklySummaryScreen() }
                composable(Routes.SETTINGS) { SettingsScreen() }
            }
        }
        if (showTabs) TabBar(route, onSelect = { nav.switchTab(it) })
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Text tabs, mono, amber when active. No icons, no pills. */
@Composable
private fun TabBar(current: String?, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        Rule()
        Row(Modifier.fillMaxWidth().height(56.dp)) {
            tabs.forEach { (route, label) ->
                val on = current == route
                Box(
                    Modifier.weight(1f).fillMaxSize().clickable { onSelect(route) }.padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
