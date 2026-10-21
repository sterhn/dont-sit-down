package com.dontsitdown.ui.chain

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dontsitdown.chain.MessageGenerator
import com.dontsitdown.ui.components.QuietAction

/** Optional, one tap. The answer gets compared with reality afterwards. */
@Composable
fun DreadScreen(onPick: (Int) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("how much are you\ndreading this?", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(40.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (1..3).forEach { level ->
                Column(
                    Modifier
                        .weight(1f)
                        .height(150.dp)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.medium)
                        .clickable { onPick(level) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(MessageGenerator.dreadSkulls(level), fontSize = if (level == 3) 22.sp else 26.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "\"${MessageGenerator.dreadLabels[level - 1]}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        QuietAction("skip", onClick = { onPick(0) }, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
