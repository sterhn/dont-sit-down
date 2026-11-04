package com.dontsitdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.dontsitdown.checkin.Checkin
import com.dontsitdown.chain.Phase
import com.dontsitdown.ui.chain.ChainScreen
import com.dontsitdown.ui.checkin.AddGoalScreen
import com.dontsitdown.ui.checkin.GoalDetailScreen
import com.dontsitdown.ui.checkin.GoalsScreen
import com.dontsitdown.ui.checkin.JournalScreen
import com.dontsitdown.ui.checkin.StatsScreen
import com.dontsitdown.ui.checkin.TodayScreen
import com.dontsitdown.ui.checkin.WeekStoryScreen
import com.dontsitdown.ui.components.LocalToaster
import com.dontsitdown.ui.components.Rule
import com.dontsitdown.ui.components.paperShadow
import com.dontsitdown.ui.components.ToastLayer
import com.dontsitdown.ui.components.Toaster
import com.dontsitdown.ui.onboarding.OnboardingScreen
import com.dontsitdown.ui.settings.SettingsScreen
import com.dontsitdown.ui.single.SingleTaskScreen
import com.dontsitdown.ui.summary.WeeklySummaryScreen
import com.dontsitdown.ui.tasks.AddEditTaskScreen
import com.dontsitdown.ui.tasks.TaskDetailScreen
import com.dontsitdown.ui.tasks.TaskListScreen
import com.dontsitdown.util.TimeUtils
import com.dontsitdown.ui.theme.CheckinWorld
import com.dontsitdown.ui.theme.World
import androidx.compose.foundation.border

object Routes {
    const val ONBOARDING = "onboarding"
    const val TODAY = "today"
    const val CHAIN = "chain"
    const val GOALS = "goals"
    const val STATS = "stats?chores={chores}"
    const val SETTINGS = "settings"

    const val TASKS = "tasks"
    const val TASK = "task/{id}"
    const val EDIT = "edit?id={id}"
    const val SINGLE = "single"
    const val WEEK = "week"
    const val GOAL = "goal/{id}"
    const val ADD_GOAL = "goal-add"
    const val JOURNAL = "journal"
    const val STORY = "story"

    fun task(id: String) = "task/$id"
    fun edit(id: String? = null) = if (id == null) "edit" else "edit?id=$id"
    fun goal(id: String) = "goal/$id"
    fun stats(chores: Boolean = false) = "stats?chores=$chores"
}

private data class Tab(val route: String, val nav: String, val label: String, val world: World)

/** The gentle half first, then the stark one. */
private val tabs = listOf(
    Tab(Routes.TODAY, Routes.TODAY, "Today", World.CHECKIN),
    Tab(Routes.GOALS, Routes.GOALS, "Goals", World.CHECKIN),
    Tab(Routes.STATS, Routes.stats(), "Progress", World.CHECKIN),
    Tab(Routes.CHAIN, Routes.CHAIN, "chain", World.CHAIN),
    Tab(Routes.SETTINGS, Routes.SETTINGS, "settings", World.CHAIN),
)

private val checkinRoutes = setOf(Routes.TODAY, Routes.GOALS, Routes.STATS, Routes.GOAL, Routes.ADD_GOAL, Routes.JOURNAL, Routes.STORY)

fun worldOf(route: String?): World = if (route in checkinRoutes) World.CHECKIN else World.CHAIN

@Composable
fun AppNav(onboarded: Boolean, openTarget: String?, onTargetConsumed: () -> Unit) {
    val nav = rememberNavController()
    val app = LocalContext.current.app
    val session by app.session.state.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val toaster = remember { Toaster() }

    LaunchedEffect(openTarget, onboarded) {
        if (!onboarded || openTarget == null) return@LaunchedEffect
        when (openTarget) {
            MainActivity.OPEN_CHAIN -> nav.switchTab(Routes.CHAIN)
            MainActivity.OPEN_SUMMARY -> nav.navigate(Routes.WEEK) { launchSingleTop = true }
            MainActivity.OPEN_TODAY -> nav.switchTab(Routes.TODAY)
            MainActivity.OPEN_SKIP -> {
                com.dontsitdown.service.Reminders.skip(app)
                nav.switchTab(Routes.CHAIN)
                toaster.show("not home today. noted.")
            }
        }
        onTargetConsumed()
    }

    val chainDark = com.dontsitdown.ui.theme.isDark(app.settings.state.collectAsStateWithLifecycle().value.theme)
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    com.dontsitdown.ui.theme.SystemBarIcons(light = if (worldOf(route) == World.CHECKIN) !systemDark else !chainDark)

    // Mid-chain, the chain screen is the whole world: no tab bar to wander off with.
    val focused = route == Routes.CHAIN && session?.phase.let { it != null && it != Phase.READY }
    val showTabs = onboarded && route in tabs.map { it.route } && !focused

    CompositionLocalProvider(LocalToaster provides toaster) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    NavHost(nav, startDestination = if (onboarded) Routes.TODAY else Routes.ONBOARDING) {
                        composable(Routes.ONBOARDING) {
                            OnboardingScreen(onDone = {
                                nav.navigate(Routes.TODAY) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                            })
                        }
                        composable(Routes.TODAY) { CheckinWorld {
                            TodayScreen(
                                onOpenChain = { nav.switchTab(Routes.CHAIN) },
                                onOpenGoal = { nav.navigate(Routes.goal(it)) },
                                onOpenGoals = { nav.switchTab(Routes.GOALS) },
                            )
                        } }
                        composable(Routes.CHAIN) {
                            Box(Modifier.statusBarsPadding().then(if (focused) Modifier.navigationBarsPadding() else Modifier)) {
                                ChainScreen(
                                    onJustOne = { nav.navigate(Routes.SINGLE) },
                                    onTasks = { nav.navigate(Routes.TASKS) },
                                    onWeek = { nav.navigate(Routes.WEEK) },
                                    onCloseDay = {
                                        com.dontsitdown.ui.checkin.TodayIntent.openEvening()
                                        nav.switchTab(Routes.TODAY)
                                    },
                                )
                            }
                        }
                        composable(Routes.GOALS) { CheckinWorld {
                            GoalsScreen(onOpen = { nav.navigate(Routes.goal(it)) }, onAdd = { nav.navigate(Routes.ADD_GOAL) })
                        } }
                        composable(Routes.STATS, arguments = listOf(navArgument("chores") { type = NavType.BoolType; defaultValue = false })) { CheckinWorld {
                            StatsScreen(
                                onWeekStory = { nav.navigate(Routes.STORY) },
                                onJournal = { nav.navigate(Routes.JOURNAL) },
                            )
                        } }
                        composable(Routes.SETTINGS) { SettingsScreen(onTasks = { nav.navigate(Routes.TASKS) }) }

                        composable(Routes.TASKS) {
                            TaskListScreen(onOpen = { nav.navigate(Routes.task(it)) }, onAdd = { nav.navigate(Routes.edit()) }, onBack = { nav.popBackStack() })
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
                        composable(Routes.WEEK) { WeeklySummaryScreen(onBack = { if (!nav.popBackStack()) nav.switchTab(Routes.TODAY) }) }
                        composable(Routes.GOAL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { CheckinWorld {
                            GoalDetailScreen(it.arguments?.getString("id")!!, onBack = { nav.popBackStack() })
                        } }
                        composable(Routes.ADD_GOAL) { CheckinWorld { AddGoalScreen(onBack = { nav.popBackStack() }) } }
                        composable(Routes.JOURNAL) { CheckinWorld { JournalScreen(onBack = { nav.popBackStack() }) } }
                        composable(Routes.STORY) { CheckinWorld { WeekStoryScreen(onBack = { nav.popBackStack() }) } }
                    }
                }
                if (showTabs && worldOf(route) == World.CHAIN) TabBar(route, onSelect = { nav.switchTab(it) })
            }
            val world = worldOf(route)
            if (showTabs && world == World.CHECKIN) {
                CheckinWorld(background = false) { Box(Modifier.align(Alignment.BottomCenter)) { NotebookTabBar(route, onSelect = { nav.switchTab(it) }) } }
            }
            if (world == World.CHECKIN) CheckinWorld(background = false) { ToastLayer(toaster, bottom = if (showTabs) 84.dp else 24.dp) }
            else ToastLayer(toaster, bottom = if (showTabs) 64.dp else 24.dp)
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** The notebook's floating pill nav, sitting on the grid. A dot when today's check-in has something in it. */
@Composable
private fun NotebookTabBar(current: String?, onSelect: (String) -> Unit) {
    val app = LocalContext.current.app
    val data by app.checkin.state.collectAsStateWithLifecycle()
    val today = com.dontsitdown.ui.components.rememberToday()
    val paper = com.dontsitdown.ui.theme.LocalPaper.current
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .fillMaxWidth()
            .paperShadow(shape = 999.dp, color = paper.shadow)
            .background(paper.card, CircleShape)
            .border(paper.border, paper.cardLine, CircleShape)
            .padding(4.dp),
    ) {
        tabs.forEach { tab ->
            val on = current == tab.route
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(if (on) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                    .clickable { onSelect(tab.nav) },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tab.label.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    if (tab.route == Routes.TODAY && Checkin.dayFilled(data, today)) {
                        Box(Modifier.padding(start = 4.dp).size(5.dp).background(paper.good, CircleShape))
                    }
                }
            }
        }
    }
}

/** Text tabs, mono, amber when active. No icons, no pills. A dot when that half is done for today. */
@Composable
private fun TabBar(current: String?, onSelect: (String) -> Unit) {
    val app = LocalContext.current.app
    val data by app.checkin.state.collectAsStateWithLifecycle()
    val today = com.dontsitdown.ui.components.rememberToday()
    val start = TimeUtils.startOfDay(today)
    val chores by remember(start) { app.repository.observeCompletionsBetween(start, start + TimeUtils.DAY) }.collectAsStateWithLifecycle(emptyList())
    val dots = mapOf(Routes.TODAY to Checkin.dayFilled(data, today), Routes.CHAIN to chores.isNotEmpty())
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        Rule()
        Row(Modifier.fillMaxWidth().height(56.dp)) {
            tabs.forEach { tab ->
                val on = current == tab.route
                Box(
                    Modifier.weight(1f).fillMaxSize().clickable { onSelect(tab.nav) }.padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tab.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    if (dots[tab.route] == true) {
                        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp).size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    }
                }
            }
        }
    }
}
