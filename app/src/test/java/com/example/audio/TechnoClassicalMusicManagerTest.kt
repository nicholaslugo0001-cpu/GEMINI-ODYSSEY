package com.example.audio

import com.example.data.PlayerProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TechnoClassicalMusicManagerTest {

    @Test
    fun resolveTrack_autoModeMatchesHubAndAllSectors() {
        assertEquals(
            ClassicalTechnoTrack.HALL_MOUNTAIN_BASS,
            ClassicalTechnoTracks.resolveTrack(ClassicalTechnoTracks.AUTO_TRACK_ID, null)
        )
        assertEquals(
            ClassicalTechnoTrack.FUR_ELISE_TRANCE,
            ClassicalTechnoTracks.resolveTrack(ClassicalTechnoTracks.AUTO_TRACK_ID, "sector_plaza")
        )
        assertEquals(
            ClassicalTechnoTrack.TURKISH_MARCH_EDM,
            ClassicalTechnoTracks.resolveTrack(ClassicalTechnoTracks.AUTO_TRACK_ID, "sector_cloud")
        )
        assertEquals(
            ClassicalTechnoTrack.TOCCATA_TECHNO,
            ClassicalTechnoTracks.resolveTrack(ClassicalTechnoTracks.AUTO_TRACK_ID, "sector_darknet")
        )
        assertEquals(
            ClassicalTechnoTrack.MOONLIGHT_SYNTH,
            ClassicalTechnoTracks.resolveTrack(ClassicalTechnoTracks.AUTO_TRACK_ID, "sector_quantum")
        )
    }

    @Test
    fun resolveTrack_explicitTrackOverridesSector() {
        ClassicalTechnoTrack.entries.forEach { track ->
            assertEquals(track, ClassicalTechnoTracks.resolveTrack(track.id, "sector_plaza"))
            assertEquals(track, ClassicalTechnoTracks.resolveTrack(track.id, null))
        }
        assertEquals(
            ClassicalTechnoTrack.TOCCATA_TECHNO,
            ClassicalTechnoTracks.resolveTrack("unknown_track_id", "sector_darknet")
        )
    }

    @Test
    fun nextTrackId_cyclesThroughAutoAndAllClassicalTracks() {
        val visited = mutableListOf<String>()
        var current = ClassicalTechnoTracks.AUTO_TRACK_ID
        repeat(ClassicalTechnoTrack.entries.size + 1) {
            current = ClassicalTechnoTracks.nextTrackId(current)
            visited += current
        }
        assertEquals(ClassicalTechnoTracks.AUTO_TRACK_ID, visited.last())
        assertEquals(ClassicalTechnoTrack.entries.size + 1, visited.toSet().size)
    }

    @Test
    fun renderPcmBlock_allTracksProduceBoundedNonSilentWaveform() {
        ClassicalTechnoTrack.entries.forEach { track ->
            val pcm = TechnoClassicalMusicManager.renderPcmBlock(
                track = track,
                startSample = 0L,
                count = 4096
            )
            assertEquals(4096, pcm.size)
            val peak = pcm.maxOf { abs(it.toInt()) }
            assertTrue("${track.title} produced silence", peak > 500)
            assertTrue("${track.title} exceeded safe output ceiling", peak <= 30000)
            assertTrue("${track.title} has zero variance", pcm.toSet().size > 40)
        }
    }

    @Test
    fun renderPcmBlock_duckFactorScalesAmplitudeAndDistinctTracksDiffer() {
        val full = TechnoClassicalMusicManager.renderPcmBlock(
            track = ClassicalTechnoTrack.TOCCATA_TECHNO,
            startSample = 1024L,
            count = 2048,
            duckFactor = 1.0f
        )
        val ducked = TechnoClassicalMusicManager.renderPcmBlock(
            track = ClassicalTechnoTrack.TOCCATA_TECHNO,
            startSample = 1024L,
            count = 2048,
            duckFactor = 0.4f
        )
        val moonlight = TechnoClassicalMusicManager.renderPcmBlock(
            track = ClassicalTechnoTrack.MOONLIGHT_SYNTH,
            startSample = 1024L,
            count = 2048,
            duckFactor = 1.0f
        )

        val fullEnergy = full.sumOf { abs(it.toLong()) }
        val duckedEnergy = ducked.sumOf { abs(it.toLong()) }
        assertTrue("Ducked audio should be quieter than full mix", duckedEnergy < fullEnergy)
        assertNotEquals(full.toList(), moonlight.toList())
    }

    @Test
    fun playerProgress_defaultsMusicEnabledAndAutoTrack() {
        val defaults = PlayerProgress()
        assertTrue(defaults.musicEnabled)
        assertEquals(ClassicalTechnoTracks.AUTO_TRACK_ID, defaults.selectedMusicTrackId)
    }

    @Test
    fun resolveTrack_bossFightAutoSwitchesToDarkSlowedBossTracks() {
        val plazaBoss = ClassicalTechnoTracks.resolveTrack(
            trackId = ClassicalTechnoTracks.AUTO_TRACK_ID,
            sectorId = "sector_plaza",
            isBossFight = true
        )
        val darknetBoss = ClassicalTechnoTracks.resolveTrack(
            trackId = ClassicalTechnoTracks.AUTO_TRACK_ID,
            sectorId = "sector_darknet",
            isBossFight = true
        )
        assertTrue(plazaBoss.isBossTrack)
        assertTrue(darknetBoss.isBossTrack)
        assertEquals(ClassicalTechnoTrack.BALD_MOUNTAIN_DOOM, plazaBoss)
        assertEquals(ClassicalTechnoTrack.DIES_IRAE_DOOM, darknetBoss)
        assertTrue("Boss tracks should be slowed (< 100 BPM)", plazaBoss.bpm < 100 && darknetBoss.bpm < 100)
    }

    @Test
    fun renderPcmBlock_bossTracksProduceDistinctHeavyLowEndAudio() {
        val diesIrae = TechnoClassicalMusicManager.renderPcmBlock(
            track = ClassicalTechnoTrack.DIES_IRAE_DOOM,
            startSample = 0L,
            count = 4096
        )
        val baldMountain = TechnoClassicalMusicManager.renderPcmBlock(
            track = ClassicalTechnoTrack.BALD_MOUNTAIN_DOOM,
            startSample = 0L,
            count = 4096
        )
        val normalToccata = TechnoClassicalMusicManager.renderPcmBlock(
            track = ClassicalTechnoTrack.TOCCATA_TECHNO,
            startSample = 0L,
            count = 4096
        )
        assertNotEquals(diesIrae.toList(), baldMountain.toList())
        assertNotEquals(diesIrae.toList(), normalToccata.toList())
    }
}
