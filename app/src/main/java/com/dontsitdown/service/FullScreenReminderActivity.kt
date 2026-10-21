package com.dontsitdown.service

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.dontsitdown.MainActivity
import com.dontsitdown.app
import com.dontsitdown.chain.MessageGenerator
import com.dontsitdown.chain.SessionMode
import com.dontsitdown.chain.SummaryCalculator
import com.dontsitdown.data.model.ReminderEventType
import com.dontsitdown.data.model.Task
import com.dontsitdown.ui.chain.RefusalFlow
import com.dontsitdown.ui.components.BigButton
import com.dontsitdown.ui.components.QuietAction
import com.dontsitdown.ui.theme.Nums
import com.dontsitdown.ui.theme.TakeoverTheme
import kotlinx.coroutines.launch

/**
 * The takeover. Lights up the lock screen like an alarm clock:
 * dark, large white text, one amber button. Nothing else.
 */
class FullScreenReminderActivity : ComponentActivity() {
    private var startRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        startRequested = intent.getBooleanExtra(EXTRA_START_NOW, false)
        setContent { TakeoverTheme { Takeover() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_START_NOW, false)) startRequested = true
    }

    @Composable
    private fun Takeover() {
        val app = app
        val settings = app.settings.current
        var chain by remember { mutableStateOf<List<Task>?>(null) }
        var offers by remember { mutableStateOf<List<Task>>(emptyList()) }
        var refusing by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            chain = app.repository.tasksByIds(app.settings.pendingChain).ifEmpty { app.generateChain() }
            offers = Reminders.negotiationOffers(app.repository.activeTasks())
        }
        LaunchedEffect(startRequested, chain) {
            val c = chain
            if (startRequested && c != null) start(c)
        }

        // Plain dismiss is just noted. Ghost and negotiator, if on, get their say first.
        val refuse: () -> Unit = {
            if (settings.ghost || settings.negotiator) {
                refusing = true
            } else {
                lifecycleScope.launch {
                    Reminders.refused(this@FullScreenReminderActivity, ReminderEventType.DISMISSED)
                    finish()
                }
            }
        }
        BackHandler(enabled = !refusing) { refuse() }

        Box(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
            val c = chain ?: return@Box
            if (refusing) {
                RefusalFlow(
                    ghostTask = c.firstOrNull()?.name,
                    offers = offers,
                    ghost = settings.ghost,
                    negotiator = settings.negotiator,
                    onAcceptChain = { start(c) },
                    onAcceptTask = { startSingle(it) },
                    onFinalRefusal = {
                        Reminders.refused(this@FullScreenReminderActivity, ReminderEventType.REFUSED)
                        if (settings.spite) SummaryCalculator.yearScore(app.repository).let { MessageGenerator.skipped(true, it.couch, it.you) }
                        else null
                    },
                    onFinished = { finish() },
                    autoFinishMs = 4_000,
                )
                return@Box
            }

            val snoozes = app.settings.snoozesToday()
            val snoozeAllowed = snoozes < settings.snoozeLimit
            val minutes = c.sumOf { it.effectiveMinutes.toDouble() }.toFloat()
            Column(
                Modifier.align(Alignment.Center).padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(MessageGenerator.HEADLINE, style = MaterialTheme.typography.displayLarge, color = Color.White)
                Spacer(Modifier.height(36.dp))
                Text(
                    c.chunked(2).joinToString("\n") { row -> row.joinToString(" · ") { it.name } },
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White.copy(alpha = 0.82f),
                )
                Spacer(Modifier.height(36.dp))
                Text(MessageGenerator.takeoverTail(minutes), style = Nums.large, color = Color.White)
                if (!snoozeAllowed && snoozes > 0) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        MessageGenerator.snoozeLimit(snoozes, c.firstOrNull()?.name),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(52.dp))
                BigButton("start", onClick = { start(c) })
                Spacer(Modifier.height(12.dp))
                if (snoozeAllowed) {
                    QuietAction("snooze ${ReminderScheduler.SNOOZE_MINUTES} min", color = Color.White.copy(alpha = 0.6f), onClick = {
                        lifecycleScope.launch {
                            Reminders.snooze(this@FullScreenReminderActivity)
                            finish()
                        }
                    })
                }
                QuietAction("not tonight", color = Color.White.copy(alpha = 0.35f), onClick = { refuse() })
            }
        }
    }

    private fun start(chain: List<Task>) = lifecycleScope.launch {
        if (chain.isEmpty()) return@launch finish()
        Reminders.started(this@FullScreenReminderActivity)
        app.session.plan(chain, SessionMode.CHAIN)
        app.session.begin()
        openAppAndFinish()
    }

    private fun startSingle(task: Task) = lifecycleScope.launch {
        Reminders.started(this@FullScreenReminderActivity, ReminderEventType.NEGOTIATED)
        app.session.plan(listOf(task), SessionMode.SINGLE)
        app.session.begin()
        openAppAndFinish()
    }

    private fun openAppAndFinish() {
        val main = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_CHAIN)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val km = getSystemService(KeyguardManager::class.java)
        if (!km.isKeyguardLocked) {
            startActivity(main)
            finish()
            return
        }
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                startActivity(main)
                finish()
            }

            override fun onDismissCancelled() = finish()

            override fun onDismissError() {
                startActivity(main)
                finish()
            }
        })
    }

    companion object {
        const val EXTRA_START_NOW = "start_now"
    }
}
