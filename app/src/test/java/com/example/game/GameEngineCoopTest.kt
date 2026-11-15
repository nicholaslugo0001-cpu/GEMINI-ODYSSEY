package com.example.game

import com.example.data.PlayerProgress
import com.example.multiplayer.CoopEnemySnapshot
import com.example.multiplayer.CoopWorldSnapshot
import com.example.multiplayer.RemotePlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineCoopTest {

    @Test
    fun focusEnemy_autoAttacksWithoutClobberingSlashState() {
        val engine = GameEngine(WebSectors.all.first(), PlayerProgress())
        val enemy = engine.enemies.first()

        // Place enemy within melee range of player
        val idx = engine.enemies.indexOfFirst { it.id == enemy.id }
        engine.enemies[idx] = enemy.copy(x = engine.player.x + 50f, y = engine.player.y)

        engine.focusEnemyAt(engine.enemies[idx].x, engine.enemies[idx].y)
        engine.tick(0.016f, 0f, 0f)

        assertTrue("Expected slashActiveTimer > 0 after auto-attack", engine.player.slashActiveTimer > 0f)
        assertTrue("Expected slashCooldown > 0 after auto-attack", engine.player.slashCooldown > 0f)
    }

    @Test
    fun hostAppliesRemotePlayerHit_andExportsSnapshotForGuest() {
        val host = GameEngine(WebSectors.all.first(), PlayerProgress())
        val target = host.enemies.first()
        val initialHp = target.hp

        host.updateRemotePlayers(
            listOf(
                RemotePlayerState(
                    uid = "friend_2",
                    displayName = "Friend 2",
                    colorIndex = 1,
                    x = 20f,
                    y = 20f,
                    facingAngle = 0f,
                    hp = 120f,
                    maxHp = 130f
                )
            )
        )
        host.applyRemotePlayerHit(target.id, 15f)
        val updated = host.enemies.first { it.id == target.id }
        assertEquals(initialHp - 15f, updated.hp, 0.01f)

        val snapshot: CoopWorldSnapshot = host.exportWorldSnapshot()
        val guest = GameEngine(WebSectors.all.first(), PlayerProgress()).apply {
            isCoopGuest = true
        }
        guest.applyHostWorldSnapshot(snapshot)
        assertEquals(host.enemies.size, guest.enemies.size)
        assertEquals(updated.hp, guest.enemies.first { it.id == target.id }.hp, 0.01f)
    }
}
