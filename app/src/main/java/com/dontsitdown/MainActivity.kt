package com.dontsitdown

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dontsitdown.ui.AppNav
import com.dontsitdown.ui.theme.AppTheme
import com.dontsitdown.ui.theme.isDark

class MainActivity : ComponentActivity() {
    /** Where a notification or the takeover asked us to land. Consumed by [AppNav]. */
    private var openTarget by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openTarget = intent.getStringExtra(EXTRA_OPEN)
        setContent {
            val settings by app.settings.state.collectAsStateWithLifecycle()
            val dark = isDark(settings.theme)
            enableEdgeToEdge(
                statusBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                navigationBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            )
            AppTheme(settings.theme) {
                AppNav(
                    onboarded = settings.onboarded,
                    openTarget = openTarget,
                    onTargetConsumed = { openTarget = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTarget = intent.getStringExtra(EXTRA_OPEN)
    }

    override fun onResume() {
        super.onResume()
        app.settings.lastOpenedAt = System.currentTimeMillis()
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_CHAIN = "chain"
        const val OPEN_SUMMARY = "summary"
    }
}
