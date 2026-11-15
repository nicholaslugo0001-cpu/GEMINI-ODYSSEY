package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun syncProgress_authenticatedUser_writesAndFetchesOwnProgress() = runBlocking {
        signInTestUser("alice_odyssey@test.com")
        val repo = CloudSyncRepository(firestore)
        val local = PlayerProgress(
            googleCoins = 120,
            totalCoinsCollected = 300,
            level = 3,
            xp = 40,
            sectorStars = mapOf("search_plains" to 5),
            missionsCompleted = 2
        )

        val syncRes = withTimeout(5000L) { repo.syncProgress(local, "Alice Knight") }
        assertTrue(syncRes.isSuccess)

        val fetched = withTimeout(5000L) { repo.fetchCloudProgress() }
        assertTrue(fetched.isSuccess)
        val cloud = fetched.getOrNull()
        assertNotNull(cloud)
        assertEquals(120L, cloud?.googleCoins)
        assertEquals(3L, cloud?.level)
        assertEquals("Alice Knight", cloud?.displayName)
    }

    @Test
    fun observeLeaderboard_authenticatedUser_receivesEntries() = runBlocking {
        signInTestUser("alice_odyssey@test.com")
        val repo = CloudSyncRepository(firestore)
        val local = PlayerProgress(
            googleCoins = 50,
            totalCoinsCollected = 500,
            level = 4,
            sectorStars = mapOf("search_plains" to 5, "youtube_studio" to 4),
            missionsCompleted = 3
        )
        withTimeout(5000L) { repo.syncProgress(local, "Alice Knight") }

        val entries = withTimeout(5000L) {
            repo.observeLeaderboard().first { list -> list.any { it.displayName == "Alice Knight" } }
        }
        assertTrue(entries.any { it.totalStars == 9L })
    }

    @Test
    fun observeLeaderboard_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = CloudSyncRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(4000L) { repo.observeLeaderboard().first() }
        } catch (t: Throwable) {
            caught = t
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
}
