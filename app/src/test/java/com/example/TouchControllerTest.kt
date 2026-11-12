package com.example

import com.example.data.PlayerProgress
import com.example.game.ArenaTouchController
import com.example.game.GameEngine
import com.example.game.WebSectors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchControllerTest {

    @Test
    fun directionalPad_clampsMagnitudeToUnitCircle() {
        val engine = GameEngine(WebSectors.all.first(), PlayerProgress())
        val controller = ArenaTouchController(engine)

        controller.onDirectionalPadMove(3f, 4f) // magnitude 5
        assertEquals(0.6f, controller.dpadInput.x, 0.001f)
        assertEquals(0.8f, controller.dpadInput.y, 0.001f)
        assertTrue(controller.dpadInput.isActive)
    }

    @Test
    fun swipeGesture_triggersDirectionalSwordSlash() {
        val engine = GameEngine(WebSectors.all.first(), PlayerProgress())
        val controller = ArenaTouchController(engine)

        // Fast swipe to the right (> 45px, < 280ms)
        controller.handlePointerUp(
            startX = 200f,
            startY = 400f,
            endX = 320f,
            endY = 400f,
            durationMs = 120L,
            viewWidth = 1080f,
            viewHeight = 1920f,
            wasHoldMoving = false
        )

        assertTrue(engine.player.slashActiveTimer > 0f)
        assertEquals(0f, engine.player.facingAngle, 0.01f)
    }

    @Test
    fun tapGesture_focusesEnemyOrSetsWalkTarget() {
        val engine = GameEngine(WebSectors.all.first(), PlayerProgress())
        val controller = ArenaTouchController(engine)

        // Tap at center of screen (near player)
        controller.handlePointerUp(
            startX = 540f,
            startY = 1000f,
            endX = 545f,
            endY = 1004f,
            durationMs = 90L,
            viewWidth = 1080f,
            viewHeight = 1920f,
            wasHoldMoving = false
        )

        // Either an enemy was focused or a ground move target was set
        val hasTarget = engine.focusedEnemyId != null || (engine.moveTargetX != null && engine.moveTargetY != null)
        assertTrue(hasTarget)
    }

    @Test
    fun holdMove_setsAndClearsMoveTarget() {
        val engine = GameEngine(WebSectors.all.first(), PlayerProgress())
        val controller = ArenaTouchController(engine)

        controller.handleHoldMove(currentX = 800f, currentY = 600f, viewWidth = 1080f, viewHeight = 1920f)
        assertNotNull(engine.moveTargetX)
        assertNotNull(engine.moveTargetY)

        controller.handlePointerUp(
            startX = 800f,
            startY = 600f,
            endX = 800f,
            endY = 600f,
            durationMs = 450L,
            viewWidth = 1080f,
            viewHeight = 1920f,
            wasHoldMoving = true
        )
        assertNull(engine.moveTargetX)
        assertNull(engine.moveTargetY)
    }
}
