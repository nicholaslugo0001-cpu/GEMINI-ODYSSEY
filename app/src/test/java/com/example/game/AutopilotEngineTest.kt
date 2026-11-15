package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.data.PlayerProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutopilotEngineTest {

    @Test
    fun autopilot_engagesNearbyEnemyAndTriggersAttack() {
        val engine = GameEngine(WebSectors.all.first(), PlayerProgress())
        engine.items.clear()
        engine.searchNodes.clear()
        val enemy = engine.enemies.first()
        engine.enemies[0] = enemy.copy(x = engine.player.x + 60f, y = engine.player.y)

        engine.setAutopilot(true)
        engine.tick(0.016f, 0f, 0f)

        val hud = engine.snapshotHud()
        assertTrue(hud.isAutopilotEnabled)
        assertTrue("Expected Autopilot to trigger sword slash", engine.player.slashActiveTimer > 0f)
        assertEquals(enemy.id, engine.focusedEnemyId)
    }

    @Test
    fun autopilot_reflectsIncomingEnemyProjectile() {
        val engine = GameEngine(WebSectors.all.first(), PlayerProgress())
        engine.enemies.clear()
        engine.projectiles.clear()
        engine.projectiles.add(
            ProjectileEntity(
                id = 9001,
                fromPlayer = false,
                x = engine.player.x + 80f,
                y = engine.player.y,
                vx = -260f,
                vy = 0f,
                damage = 12f,
                radius = 9f,
                color = Color.Magenta,
                life = 2.0f
            )
        )

        engine.setAutopilot(true)
        engine.tick(0.016f, 0f, 0f)

        assertTrue(
            "Expected Autopilot to reflect enemy projectile back",
            engine.projectiles.any { it.id == 9001 && it.fromPlayer }
        )
        assertEquals("Reflecting Fire", engine.snapshotHud().autopilotStatus)
    }

    @Test
    fun autopilot_cyclesStancesAndResetsOnDisable() {
        val engine = GameEngine(WebSectors.all.first(), PlayerProgress())
        assertEquals(AutopilotStance.BALANCED, engine.autopilotStance)

        engine.setAutopilot(true)
        engine.cycleAutopilotStance()
        assertEquals(AutopilotStance.AGGRESSIVE, engine.snapshotHud().autopilotStance)

        engine.cycleAutopilotStance()
        assertEquals(AutopilotStance.OBJECTIVE, engine.snapshotHud().autopilotStance)

        engine.cycleAutopilotStance()
        assertEquals(AutopilotStance.BALANCED, engine.snapshotHud().autopilotStance)

        engine.setAutopilot(false)
        assertFalse(engine.snapshotHud().isAutopilotEnabled)
        assertEquals("STANDBY", engine.snapshotHud().autopilotStatus)
    }

    @Test
    fun autopilot_manualJoystickInputOverridesTemporarily() {
        val engine = GameEngine(WebSectors.all.first(), PlayerProgress())
        engine.setAutopilot(true)
        val startX = engine.player.x

        engine.tick(0.03f, 1.0f, 0f)

        assertEquals("Manual Override", engine.snapshotHud().autopilotStatus)
        assertTrue("Expected hero to move right under manual override", engine.player.x > startX)
    }
}
