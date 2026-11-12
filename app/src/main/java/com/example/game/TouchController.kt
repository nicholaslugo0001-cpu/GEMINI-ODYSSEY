package com.example.game

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.components.screenToWorld
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Normalized 2D vector from the bottom directional pad (-1f..1f per axis).
 */
data class DirectionalPadInput(
    val x: Float = 0f,
    val y: Float = 0f
) {
    val magnitude: Float get() = hypot(x, y).coerceAtMost(1f)
    val isActive: Boolean get() = magnitude > 0.08f
}

/**
 * Dedicated controller layer that translates raw touch events (directional pad,
 * swipes, taps, and touch-hold drags) into high-level RPG actions on [GameEngine].
 */
class ArenaTouchController(
    private val engine: GameEngine,
    private val swipeDistanceThresholdPx: Float = 45f,
    private val gestureMaxDurationMs: Long = 280L,
    private val holdMoveDelayMs: Long = 75L
) {
    var dpadInput: DirectionalPadInput = DirectionalPadInput()
        private set

    fun onDirectionalPadMove(rawX: Float, rawY: Float) {
        val mag = hypot(rawX, rawY)
        dpadInput = if (mag > 1f) {
            DirectionalPadInput(rawX / mag, rawY / mag)
        } else {
            DirectionalPadInput(rawX, rawY)
        }
    }

    /**
     * Classifies a completed pointer gesture from [startX],[startY] to [endX],[endY]
     * over [durationMs] into either a directional swipe attack or a tap-to-focus.
     */
    fun handlePointerUp(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long,
        viewWidth: Float,
        viewHeight: Float,
        wasHoldMoving: Boolean
    ) {
        val dx = endX - startX
        val dy = endY - startY
        val dist = hypot(dx, dy)

        when {
            durationMs < gestureMaxDurationMs && dist > swipeDistanceThresholdPx -> {
                val swipeAngle = atan2(dy, dx)
                engine.triggerSwordSlash(swipeAngle)
            }
            durationMs < gestureMaxDurationMs && dist <= swipeDistanceThresholdPx -> {
                val (wx, wy) = screenToWorld(
                    sx = endX,
                    sy = endY,
                    viewWidth = viewWidth,
                    viewHeight = viewHeight,
                    camX = engine.player.x,
                    camY = engine.player.y
                )
                engine.focusEnemyAt(wx, wy)
            }
            wasHoldMoving -> {
                engine.setMoveTarget(null, null)
            }
        }
    }

    fun handleHoldMove(
        currentX: Float,
        currentY: Float,
        viewWidth: Float,
        viewHeight: Float
    ) {
        val (wx, wy) = screenToWorld(
            sx = currentX,
            sy = currentY,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            camX = engine.player.x,
            camY = engine.player.y
        )
        engine.setMoveTarget(wx, wy)
    }

    fun shouldTriggerHoldMove(elapsedMs: Long): Boolean = elapsedMs >= holdMoveDelayMs
}

/**
 * Modifier that binds [ArenaTouchController] to the 3D playfield for
 * touch-to-move, swipe-to-slash, and tap-to-focus gestures.
 */
fun Modifier.arenaTouchController(
    controller: ArenaTouchController,
    key: Any? = controller
): Modifier = this.pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startPos = down.position
        val startTime = System.currentTimeMillis()
        var isHoldMove = false

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            val elapsed = System.currentTimeMillis() - startTime

            if (!change.pressed) {
                controller.handlePointerUp(
                    startX = startPos.x,
                    startY = startPos.y,
                    endX = change.position.x,
                    endY = change.position.y,
                    durationMs = elapsed,
                    viewWidth = size.width.toFloat(),
                    viewHeight = size.height.toFloat(),
                    wasHoldMoving = isHoldMove
                )
                break
            } else if (controller.shouldTriggerHoldMove(elapsed)) {
                isHoldMove = true
                controller.handleHoldMove(
                    currentX = change.position.x,
                    currentY = change.position.y,
                    viewWidth = size.width.toFloat(),
                    viewHeight = size.height.toFloat()
                )
            }
        }
    }
}
