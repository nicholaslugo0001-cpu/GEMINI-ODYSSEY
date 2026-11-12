package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.GeminiCyan
import kotlin.math.hypot

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    onMove: (Float, Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .testTag("virtual_joystick")
            .pointerInput(Unit) {
                val maxRadius = (this.size.width / 2f) * 0.72f
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val center = Offset(this.size.width / 2f, this.size.height / 2f)
                        val delta = startOffset - center
                        val dist = hypot(delta.x, delta.y)
                        val clamped = if (dist > maxRadius) delta * (maxRadius / dist) else delta
                        knobOffset = clamped
                        val nx = (clamped.x / maxRadius) * 1.65f
                        val ny = (clamped.y / maxRadius) * 1.65f
                        val nMag = hypot(nx, ny)
                        if (nMag > 1f) onMove(nx / nMag, ny / nMag) else onMove(nx, ny)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(this.size.width / 2f, this.size.height / 2f)
                        val delta = change.position - center
                        val dist = hypot(delta.x, delta.y)
                        val clamped = if (dist > maxRadius) delta * (maxRadius / dist) else delta
                        knobOffset = clamped
                        val nx = (clamped.x / maxRadius) * 1.65f
                        val ny = (clamped.y / maxRadius) * 1.65f
                        val nMag = hypot(nx, ny)
                        if (nMag > 1f) onMove(nx / nMag, ny / nMag) else onMove(nx, ny)
                    },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.minDimension / 2f - 4f

            drawCircle(
                color = CyberSurface.copy(alpha = 0.55f),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = GeminiCyan.copy(alpha = 0.45f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 3f)
            )

            // Crosshair guides
            drawLine(
                color = GeminiCyan.copy(alpha = 0.18f),
                start = Offset(center.x - outerRadius * 0.6f, center.y),
                end = Offset(center.x + outerRadius * 0.6f, center.y),
                strokeWidth = 2f
            )
            drawLine(
                color = GeminiCyan.copy(alpha = 0.18f),
                start = Offset(center.x, center.y - outerRadius * 0.6f),
                end = Offset(center.x, center.y + outerRadius * 0.6f),
                strokeWidth = 2f
            )

            val knobCenter = center + knobOffset
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GeminiCyan, GeminiCyan.copy(alpha = 0.4f)),
                    center = knobCenter,
                    radius = outerRadius * 0.45f
                ),
                radius = outerRadius * 0.36f,
                center = knobCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = outerRadius * 0.36f,
                center = knobCenter,
                style = Stroke(width = 2.5f)
            )
        }
    }
}

@Composable
fun AbilityButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    readyFraction: Float,
    size: Dp = 66.dp,
    testTag: String,
    onClick: () -> Unit
) {
    val isReady = readyFraction >= 0.99f
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    if (isReady) {
                        Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.52f), CyberSurface.copy(alpha = 0.88f))
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(CyberSurface.copy(alpha = 0.72f), CyberSurface.copy(alpha = 0.72f))
                        )
                    }
                )
                .border(
                    width = if (isReady) 2.dp else 1.dp,
                    color = if (isReady) accentColor else accentColor.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable(enabled = isReady, onClick = onClick)
                .testTag(testTag)
        ) {
            if (!isReady) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = accentColor.copy(alpha = 0.75f),
                        startAngle = -90f,
                        sweepAngle = 360f * readyFraction,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isReady) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(size * 0.44f)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isReady) Color.White else Color.White.copy(alpha = 0.55f)
        )
    }
}

@Composable
fun HoldableGuardButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    isGuarding: Boolean,
    size: Dp = 64.dp,
    testTag: String,
    onGuardChanged: (Boolean) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    if (isGuarding) accentColor.copy(alpha = 0.45f)
                    else CyberSurface.copy(alpha = 0.78f)
                )
                .border(
                    width = if (isGuarding) 3.dp else 2.dp,
                    color = accentColor,
                    shape = CircleShape
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val anyPressed = event.changes.any { it.pressed }
                            onGuardChanged(anyPressed)
                        }
                    }
                }
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(size * 0.44f)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isGuarding) accentColor else Color.White
        )
    }
}
