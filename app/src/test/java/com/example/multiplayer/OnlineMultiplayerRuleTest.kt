package com.example.multiplayer

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineMultiplayerRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndJoinRoom_authenticatedUsers_syncsMembersAndPlayerDocs() = runBlocking {
        signInTestUser("host_knight@test.com")
        val hostRepo = OnlineMultiplayerRepository(firestore)
        val createRes = withTimeout(5000L) {
            hostRepo.createRoom(sectorId = "sector_plaza", displayName = "Host Knight", maxHp = 130f)
        }
        assertTrue("Expected room creation to succeed: ${createRes.exceptionOrNull()}", createRes.isSuccess)
        val roomCode = createRes.getOrThrow()

        val initialSession = withTimeout(5000L) {
            hostRepo.observeSession(roomCode).first { it.isActive && it.members.size == 1 }
        }
        assertEquals(roomCode, initialSession.roomCode)
        assertTrue(initialSession.isHost)

        // Second friend joins the same room
        signInTestUser("friend_two@test.com")
        val friendRepo = OnlineMultiplayerRepository(firestore)
        val joinRes = withTimeout(5000L) {
            friendRepo.joinRoom(rawCode = roomCode, displayName = "Friend Two", maxHp = 130f)
        }
        assertTrue("Expected joinRoom to succeed: ${joinRes.exceptionOrNull()}", joinRes.isSuccess)

        val joinedSession = withTimeout(5000L) {
            friendRepo.observeSession(roomCode).first { it.members.size == 2 && it.remotePlayers.size == 1 }
        }
        assertEquals(2, joinedSession.members.size)
        assertEquals("Host Knight", joinedSession.remotePlayers.first().displayName)
    }

    @Test
    fun createRoom_unauthenticatedUser_failsGracefully() = runBlocking {
        auth.signOut()
        val repo = OnlineMultiplayerRepository(firestore)
        val res = repo.createRoom(sectorId = "sector_plaza", displayName = "Anon", maxHp = 130f)
        assertTrue(res.isFailure)
    }
}
