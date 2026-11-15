package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiTacticalRepository
import com.example.data.CloudSyncRepository
import com.example.data.PlayerProgressRepository
import com.example.multiplayer.LocalLanMultiplayerManager
import com.example.multiplayer.OnlineMultiplayerRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainViewModelGuestTest {

    @Test
    fun guestMode_exposesGuestLeaderboardState_andGuardsOnlineRooms() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))

        val vm = MainViewModel(
            repository = PlayerProgressRepository(context),
            cloudRepository = CloudSyncRepository(db),
            onlineMultiplayer = OnlineMultiplayerRepository(db),
            lanMultiplayer = LocalLanMultiplayerManager(context),
            currentUserId = "guest_local",
            displayName = "Guest Knight",
            isGuestMode = true,
            geminiRepository = GeminiTacticalRepository()
        )

        assertEquals(LeaderboardUiState.GuestMode, vm.leaderboardState.value)

        vm.hostOnlineRoom("sector_plaza")
        val errorMsg = vm.coopSession.value.errorMessage.orEmpty()
        assertTrue("Expected Google Sign-In guidance for guest online room", errorMsg.contains("Google Sign-In"))
        vm.leaveCoopRoom()
    }
}
