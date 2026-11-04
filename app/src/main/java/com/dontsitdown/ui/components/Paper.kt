package com.dontsitdown.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dontsitdown.R
import com.dontsitdown.checkin.Checkin
import com.dontsitdown.checkin.Face
import com.dontsitdown.ui.theme.HandText
import com.dontsitdown.ui.theme.LocalPaper
import kotlinx.coroutines.launch

fun Face.drawable(): Int = when (this) {
    Face.NEUTRAL -> R.drawable.char_neutral
    Face.HAPPY -> R.drawable.char_happy
    Face.GREAT -> R.drawable.char_great
    Face.LOW -> R.drawable.char_low
    Face.ROUGH -> R.drawable.char_rough
    Face.SLEEPY -> R.drawable.char_sleepy
    Face.PROUD -> R.drawable.char_proud
    Face.COFFEE -> R.drawable.char_coffee
    Face.THINKING -> R.drawable.char_thinking
    Face.COZY -> R.drawable.char_cozy
    Face.SIGH -> R.drawable.char_sigh
    Face.NICE -> R.drawable.char_nice
    Face.ANNOYED -> R.drawable.char_annoyed
    Face.SURPRISED -> R.drawable.char_surprised
    Face.FLUSTERED -> R.drawable.char_flustered
    Face.AWAY -> R.drawable.char_away
}

/** A hard offset shadow, like paper stuck on paper. */
fun Modifier.paperShadow(shape: Dp = 6.dp, offset: Dp = 3.dp, color: Color? = null): Modifier = this.then(
    Modifier.drawBehind {
        val o = offset.toPx()
        drawRoundRect(color ?: Color.Black.copy(alpha = 0.35f), topLeft = Offset(o, o), size = size, cornerRadius = CornerRadius(shape.toPx()))
    }
)

/** A strip of washi tape. */
@Composable
fun Tape(modifier: Modifier = Modifier, width: Dp = 54.dp, tilt: Float = -3f) {
    val paper = LocalPaper.current
    Box(
        modifier
            .rotate(tilt)
            .size(width, 16.dp)
            .drawBehind {
                drawRect(paper.tapeB)
                val stripe = 6.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    val p = androidx.compose.ui.graphics.Path().apply {
                        moveTo(x, size.height); lineTo(x + size.height, 0f); lineTo(x + size.height + stripe / 2, 0f); lineTo(x + stripe / 2, size.height); close()
                    }
                    drawPath(p, paper.tapeA)
                    x += stripe
                }
            },
    )
}

/** A card cut from paper and stuck on a little crooked. The check-in's basic block. */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    tilt: Float = 0f,
    tape: Boolean = false,
    color: Color? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val paper = LocalPaper.current
    val shape = RoundedCornerShape(6.dp)
    Box(modifier.padding(top = if (tape) 8.dp else 0.dp).rotate(tilt)) {
        Column(
            Modifier
                .fillMaxWidth()
                .paperShadow(color = paper.shadow)
                .background(color ?: paper.card, shape)
                .border(1.dp, paper.cardLine, shape)
                .padding(padding),
            content = content,
        )
        if (tape) Tape(Modifier.align(Alignment.TopCenter).offset(y = (-9).dp))
    }
}

/**
 * The character in a taped-in polaroid, captioned in his handwriting. The photo develops like
 * instant film the first time it appears; faces swap with a little squish; tap him and he reacts.
 */
@Composable
fun Polaroid(
    face: Face,
    caption: String,
    modifier: Modifier = Modifier,
    width: Dp = 132.dp,
    tilt: Float = -3f,
    tape: Boolean = true,
    develop: Boolean = false,
    onPoke: (() -> Unit)? = null,
) {
    val paper = LocalPaper.current
    val developed = remember { Animatable(if (develop) 0f else 1f) }
    LaunchedEffect(Unit) { if (develop) developed.animateTo(1f, tween(1800, easing = FastOutSlowInEasing)) }
    val wiggle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val pad = width * 0.06f
    val photoH = width * 0.82f

    Box(
        modifier
            .padding(top = if (tape) 10.dp else 0.dp)
            .graphicsLayer { rotationZ = tilt + wiggle.value }
            .semantics { contentDescription = "the character: $caption" },
    ) {
        Column(
            Modifier
                .width(width)
                .paperShadow(shape = 1.dp, color = paper.shadow)
                .background(paper.polaroid)
                .border(1.dp, paper.cardLine)
                .then(
                    if (onPoke != null) Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {
                        onPoke()
                        scope.launch {
                            for (r in listOf(-5f, 4f, -2f, 0f)) wiggle.animateTo(r, tween(110))
                        }
                    } else Modifier
                )
                .padding(start = pad, end = pad, top = pad, bottom = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(photoH)
                    .background(lerp(Color(0xFFF4F1EA), paper.photo, developed.value))
                    .graphicsLayer { clip = true },
                contentAlignment = Alignment.BottomCenter,
            ) {
                AnimatedContent(
                    targetState = face,
                    transitionSpec = { (fadeIn(tween(160)) + scaleIn(spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), initialScale = 0.88f)) togetherWith fadeOut(tween(120)) },
                    label = "face",
                ) { f ->
                    val sat = developed.value
                    Image(
                        painterResource(f.drawable()),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = if (sat < 1f) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(sat) }) else null,
                        modifier = Modifier
                            .requiredSize(photoH * 1.1f)
                            .offset(y = photoH * 0.06f)
                            .graphicsLayer { alpha = 0.15f + 0.85f * developed.value },
                    )
                }
            }
            Text(
                caption,
                style = HandText.small.copy(fontSize = HandText.small.fontSize * (if (width < 90.dp) 0.8f else 1f) * (if (caption.length > 11) 0.82f else 1f)),
                color = paper.polaroidInk,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp),
            )
        }
        if (tape) Tape(Modifier.align(Alignment.TopCenter).offset(y = (-10).dp), width = width * 0.38f, tilt = 4f)
    }
}

/** A 1–10 scale of little boxes you colour in: tap a box or slide across. Tapping the current value clears it. */
@Composable
fun Scale(
    label: String,
    value: Int?,
    color: Color,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    ends: Pair<String, String>? = null,
    moodWord: Boolean = false,
) {
    val paper = LocalPaper.current
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    fun set(v: Int?) {
        if (v != current) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            change(v)
        }
    }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                value?.toString() ?: "–",
                style = HandText.large,
                color = if (value == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    stateDescription = value?.let { if (moodWord) "$it, ${Checkin.moodWord(it)}" else "$it of 10" } ?: "not set"
                },
        ) {
            val w = constraints.maxWidth.toFloat()
            fun at(x: Float) = ((x / w) * 10).toInt().plus(1).coerceIn(1, 10)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .pointerInput(Unit) { detectTapGestures { o -> at(o.x).let { set(if (it == current) null else it) } } }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(onDragStart = { o -> set(at(o.x)) }) { change, _ -> set(at(change.position.x)) }
                    },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (i in 1..10) {
                    val on = value != null && i <= value
                    val tip = i == value
                    val shape = RoundedCornerShape(4.dp)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(38.dp)
                            .graphicsLayer { scaleY = if (tip) 1.1f else 1f }
                            .background(
                                when {
                                    tip -> color
                                    on -> color.copy(alpha = 0.4f)
                                    else -> Color.Transparent
                                },
                                shape,
                            )
                            .border(1.dp, if (tip) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else paper.cardLine, shape),
                    )
                }
            }
        }
        if (moodWord || ends != null) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                val (a, b) = ends ?: ("drained" to "radiant")
                Text(a, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (moodWord) {
                        AnimatedContent(value, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(100)) }, label = "word") { v ->
                            if (v != null) Text(Checkin.moodWord(v), style = HandText.small)
                            else Text("slide to pick", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text(b, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A small toggle chip; [on] fills it with [onColor]. */
@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, onColor: Color? = null, shape: Shape = RoundedCornerShape(4.dp)) {
    val paper = LocalPaper.current
    val haptics = LocalHapticFeedback.current
    val fill = onColor ?: MaterialTheme.colorScheme.primary
    val filledText = if (onColor == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    Box(
        modifier
            .heightIn(min = 40.dp)
            .background(if (on) fill else paper.card, shape)
            .border(1.dp, if (on) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else paper.cardLine, shape)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (on) filledText else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** This week as a row of dots: filled for days with a check-in, ringed for today. */
@Composable
fun WeekDots(done: List<Boolean>, todayIndex: Int, letters: List<String>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        done.forEachIndexed { i, d ->
            val future = i > todayIndex
            val bounce = remember { Animatable(1f) }
            var wasDone by remember { mutableStateOf(d) }
            LaunchedEffect(d) {
                if (d && !wasDone) {
                    bounce.snapTo(0.6f)
                    bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow))
                }
                wasDone = d
            }
            val ink = MaterialTheme.colorScheme.onBackground
            Box(
                Modifier
                    .size(22.dp)
                    .graphicsLayer { scaleX = bounce.value; scaleY = bounce.value; alpha = if (future) 0.5f else 1f }
                    .then(
                        when {
                            d -> Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                            i == todayIndex -> Modifier.border(2.dp, ink, CircleShape)
                            else -> Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (d) "✓" else letters[i],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (d) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A thin progress bar. */
@Composable
fun ThinBar(fraction: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, height: Dp = 6.dp) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fraction) { anim.animateTo(fraction.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing)) }
    Box(modifier.height(height).border(1.dp, LocalPaper.current.cardLine, CircleShape).padding(1.dp)) {
        Box(Modifier.fillMaxWidth(anim.value).height(height).background(color, CircleShape))
    }
}

/** Counter to restart one-shot animations. */
@Composable
fun rememberPulse() = remember { mutableIntStateOf(0) }

/** A note field: dashed-feeling outline that firms up on focus. Holds its own text so typing never fights the store. */
@Composable
fun NoteField(
    initial: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 3,
    key: Any? = null,
    onDone: (() -> Unit)? = null,
) {
    val paper = LocalPaper.current
    var text by remember(key) { mutableStateOf(initial) }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    androidx.compose.foundation.text.BasicTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it)
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
            imeAction = if (onDone != null) androidx.compose.ui.text.input.ImeAction.Done else androidx.compose.ui.text.input.ImeAction.Default,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = modifier
            .fillMaxWidth()
            .androidxFocus { focused = it },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(if (focused) paper.card else Color.Transparent, shape)
                    .border(1.dp, if (focused) MaterialTheme.colorScheme.primary else paper.cardLine, shape)
                    .padding(horizontal = 13.dp, vertical = 11.dp),
            ) {
                if (text.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
            }
        },
    )
}

private fun Modifier.androidxFocus(onFocus: (Boolean) -> Unit) = this.then(
    Modifier.onFocusChanged { onFocus(it.isFocused) }
)

/** Hand-lettered card heading. */
@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = HandText.medium, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** A small number tile: big hand-written value, mono label. */
@Composable
fun Tile(value: String, label: String, modifier: Modifier = Modifier, tint: Color? = null, extra: (@Composable () -> Unit)? = null) {
    val paper = LocalPaper.current
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier
            .background(tint ?: paper.card, shape)
            .border(1.dp, paper.cardLine, shape)
            .padding(horizontal = 11.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = HandText.large)
            extra?.invoke()
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
