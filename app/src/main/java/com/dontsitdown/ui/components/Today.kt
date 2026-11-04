package com.dontsitdown.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.dontsitdown.util.TimeUtils
import java.time.LocalDate

/** Today's date, re-read whenever the app comes back to the front, so a screen left open overnight moves on. */
@Composable
fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(TimeUtils.today()) }
    LifecycleResumeEffect(Unit) {
        val now = TimeUtils.today()
        if (now != today) today = now
        onPauseOrDispose { }
    }
    return today
}
