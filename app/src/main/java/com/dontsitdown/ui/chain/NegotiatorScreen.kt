package com.dontsitdown.ui.chain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dontsitdown.chain.MessageGenerator
import com.dontsitdown.data.model.Task
import com.dontsitdown.ui.components.BigButton
import com.dontsitdown.ui.components.OutlineButton
import com.dontsitdown.ui.components.QuietAction

private sealed interface Step {
    data object Ghost : Step
    data class Offer(val task: Task, val first: Boolean) : Step
}

/**
 * What happens when you try to get out of it: ghost of tomorrow-you (if on), then the
 * negotiator's two small offers (if on), then "alright. noted." — no guilt after that.
 */
@Composable
fun RefusalFlow(
    ghostTask: String?,
    offers: List<Task>,
    ghost: Boolean,
    negotiator: Boolean,
    onAcceptChain: () -> Unit,
    onAcceptTask: (Task) -> Unit,
    onFinalRefusal: suspend () -> String?,
    onFinished: () -> Unit,
    autoFinishMs: Long? = null,
) {
    val steps = remember(offers, ghost, negotiator) {
        buildList {
            if (ghost) add(Step.Ghost)
            if (negotiator && offers.isNotEmpty()) {
                add(Step.Offer(offers[0], first = true))
                add(Step.Offer(offers.getOrElse(1) { offers[0] }, first = false))
            }
        }
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps.getOrNull(index)

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        when (step) {
            Step.Ghost -> {
                val o = MessageGenerator.ghost(ghostTask)
                Prompt(o.text)
                Choices(o.yes, o.no, onYes = onAcceptChain, onNo = { index++ })
            }
            is Step.Offer -> {
                val m = step.task.effectiveMinutes
                val o = if (step.first) MessageGenerator.negotiatorFirst(step.task.name, m)
                else MessageGenerator.negotiatorSecond(step.task.name, m)
                Prompt(o.text)
                Choices(o.yes, o.no, onYes = { onAcceptTask(step.task) }, onNo = { index++ })
            }
            null -> {
                var extra by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    extra = onFinalRefusal()
                    if (autoFinishMs != null) {
                        kotlinx.coroutines.delay(autoFinishMs)
                        onFinished()
                    }
                }
                Prompt(MessageGenerator.NOTED)
                extra?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(40.dp))
                QuietAction("ok", onClick = onFinished)
            }
        }
    }
}

@Composable
private fun Prompt(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium)
}

@Composable
private fun Choices(yes: String, no: String, onYes: () -> Unit, onNo: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    BigButton(yes, onClick = onYes)
    Spacer(Modifier.height(12.dp))
    OutlineButton(no, onClick = onNo, modifier = Modifier.fillMaxWidth())
}
