package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 100% Public-Domain classical compositions procedurally remixed into
 * synthesized techno / synthwave tracks in-app with zero external audio assets.
 */
enum class ClassicalTechnoTrack(
    val id: String,
    val title: String,
    val composer: String,
    val workTitle: String,
    val bpm: Int,
    val isBossTrack: Boolean = false
) {
    TOCCATA_TECHNO(
        id = "toccata_techno",
        title = "Toccata Cyber-Rave",
        composer = "J.S. Bach",
        workTitle = "Toccata & Fugue in D Minor (Public Domain)",
        bpm = 134
    ),
    MOONLIGHT_SYNTH(
        id = "moonlight_synth",
        title = "Moonlight Overdrive",
        composer = "L. van Beethoven",
        workTitle = "Moonlight Sonata 3rd Mvt. (Public Domain)",
        bpm = 142
    ),
    FUR_ELISE_TRANCE(
        id = "fur_elise_trance",
        title = "Für Elise Neon Trance",
        composer = "L. van Beethoven",
        workTitle = "Bagatelle No. 25 'Für Elise' (Public Domain)",
        bpm = 130
    ),
    HALL_MOUNTAIN_BASS(
        id = "hall_mountain_bass",
        title = "Mountain King Hard-Tech",
        composer = "E. Grieg",
        workTitle = "In the Hall of the Mountain King (Public Domain)",
        bpm = 138
    ),
    TURKISH_MARCH_EDM(
        id = "turkish_march_edm",
        title = "Rondo Alla Techno",
        composer = "W.A. Mozart",
        workTitle = "Piano Sonata No. 11 'Turkish March' (Public Domain)",
        bpm = 134
    ),
    DIES_IRAE_DOOM(
        id = "dies_irae_doom",
        title = "Dies Irae Dread (Boss)",
        composer = "G. Verdi / Trad.",
        workTitle = "Requiem 'Dies Irae' Slowed Dark-Tech (Public Domain)",
        bpm = 86,
        isBossTrack = true
    ),
    BALD_MOUNTAIN_DOOM(
        id = "bald_mountain_doom",
        title = "Bald Mountain Doom (Boss)",
        composer = "M. Mussorgsky",
        workTitle = "Night on Bald Mountain Slowed Industrial (Public Domain)",
        bpm = 92,
        isBossTrack = true
    )
}

object ClassicalTechnoTracks {
    const val AUTO_TRACK_ID = "AUTO"

    fun resolveTrack(
        trackId: String,
        sectorId: String?,
        isBossFight: Boolean = false
    ): ClassicalTechnoTrack {
        if (trackId != AUTO_TRACK_ID) {
            ClassicalTechnoTrack.entries.firstOrNull { it.id == trackId }?.let { return it }
        }
        if (isBossFight) {
            return when (sectorId) {
                "sector_plaza", "sector_cloud" -> ClassicalTechnoTrack.BALD_MOUNTAIN_DOOM
                else -> ClassicalTechnoTrack.DIES_IRAE_DOOM
            }
        }
        return when (sectorId) {
            "sector_plaza" -> ClassicalTechnoTrack.FUR_ELISE_TRANCE
            "sector_cloud" -> ClassicalTechnoTrack.TURKISH_MARCH_EDM
            "sector_darknet" -> ClassicalTechnoTrack.TOCCATA_TECHNO
            "sector_quantum" -> ClassicalTechnoTrack.MOONLIGHT_SYNTH
            else -> ClassicalTechnoTrack.HALL_MOUNTAIN_BASS
        }
    }

    fun nextTrackId(currentTrackId: String): String {
        val allIds = listOf(AUTO_TRACK_ID) + ClassicalTechnoTrack.entries.map { it.id }
        val idx = allIds.indexOf(currentTrackId)
        return allIds[(idx + 1) % allIds.size]
    }

    fun displayLabel(
        trackId: String,
        sectorId: String? = null,
        isBossFight: Boolean = false
    ): String {
        return if (trackId == AUTO_TRACK_ID) {
            val resolved = resolveTrack(trackId, sectorId, isBossFight)
            "Auto (${resolved.title})"
        } else {
            val track = resolveTrack(trackId, sectorId, isBossFight)
            "${track.title} • ${track.composer}"
        }
    }
}

private data class StepPattern(
    val leadMidi: IntArray, // <= 0 means rest
    val bassMidi: IntArray, // <= 0 means rest
    val arpMidi: IntArray = IntArray(0) // subtle sparkle arpeggio
)

class TechnoClassicalMusicManager {
    companion object {
        const val SAMPLE_RATE = 22050
        private const val CHUNK_SAMPLES = 1024
        private const val MASTER_MUSIC_GAIN = 0.27

        private val patterns: Map<ClassicalTechnoTrack, StepPattern> by lazy {
            mapOf(
                // J.S. Bach — Toccata & Fugue in D Minor (64 steps = 4 bars of 16th notes)
                ClassicalTechnoTrack.TOCCATA_TECHNO to StepPattern(
                    leadMidi = intArrayOf(
                        // Bar 1: Iconic mordent + dramatic descent
                        81, 79, 81, 81, 81, 81, 0, 79, 77, 76, 74, 74, 73, 73, 74, 74,
                        // Bar 2: Second mordent + Baroque drive
                        69, 67, 69, 69, 69, 0, 64, 65, 67, 64, 65, 62, 69, 74, 77, 81,
                        // Bar 3: High D-minor techno arpeggio
                        86, 81, 77, 81, 86, 81, 77, 81, 85, 81, 76, 81, 85, 81, 76, 81,
                        // Bar 4: Driving turnaround back to A5
                        84, 79, 77, 79, 82, 79, 76, 79, 81, 77, 74, 77, 81, 79, 77, 76
                    ),
                    bassMidi = intArrayOf(
                        38, 0, 50, 38, 38, 0, 50, 38, 38, 0, 50, 38, 38, 0, 50, 45,
                        38, 0, 50, 38, 38, 0, 50, 38, 33, 0, 45, 33, 33, 0, 45, 38,
                        38, 0, 50, 38, 38, 0, 50, 38, 33, 0, 45, 33, 33, 0, 45, 33,
                        34, 0, 46, 34, 36, 0, 48, 36, 38, 0, 50, 38, 33, 0, 45, 33
                    )
                ),

                // Beethoven — Moonlight Sonata 3rd Movement Presto Agitato (64 steps)
                ClassicalTechnoTrack.MOONLIGHT_SYNTH to StepPattern(
                    leadMidi = intArrayOf(
                        // Bar 1: G#4 - C#5 - E5 rising storm
                        68, 73, 76, 68, 73, 76, 68, 73, 76, 68, 73, 76, 73, 76, 80, 85,
                        // Bar 2: A4 - D5 - F#5 / Neapolitan tension
                        69, 74, 78, 69, 74, 78, 69, 74, 78, 69, 74, 78, 74, 78, 81, 86,
                        // Bar 3: G#4 - B#4 - F#5 dominant drive
                        68, 72, 78, 68, 72, 78, 68, 72, 78, 68, 72, 78, 72, 78, 80, 84,
                        // Bar 4: Resolution cascade in C# minor
                        73, 76, 80, 85, 80, 76, 73, 76, 80, 76, 73, 68, 73, 76, 80, 85
                    ),
                    bassMidi = intArrayOf(
                        37, 0, 49, 37, 37, 0, 49, 37, 37, 0, 49, 37, 37, 0, 49, 37,
                        38, 0, 50, 38, 38, 0, 50, 38, 38, 0, 50, 38, 38, 0, 50, 38,
                        32, 0, 44, 32, 32, 0, 44, 32, 32, 0, 44, 32, 32, 0, 44, 32,
                        37, 0, 49, 37, 37, 0, 49, 37, 37, 0, 49, 37, 32, 0, 44, 32
                    )
                ),

                // Beethoven — Für Elise Neon Trance (64 steps)
                ClassicalTechnoTrack.FUR_ELISE_TRANCE to StepPattern(
                    leadMidi = intArrayOf(
                        // Bar 1: E5 D#5 E5 D#5 E5 B4 D5 C5 A4
                        76, 75, 76, 75, 76, 71, 74, 72, 69, 69, 0, 60, 64, 69, 71, 71,
                        // Bar 2: E4 G#4 B4 C5 turnaround
                        0, 64, 68, 71, 72, 72, 0, 64, 76, 75, 76, 75, 76, 71, 74, 72,
                        // Bar 3: Repeat motif -> A4 -> C4 E4 A4 B4
                        69, 69, 0, 60, 64, 69, 71, 71, 0, 64, 72, 71, 69, 69, 0, 71,
                        // Bar 4: High trance lift C5 D5 E5 -> G4 F5 E5 D5
                        72, 74, 76, 76, 0, 67, 77, 76, 74, 74, 0, 65, 76, 74, 72, 71
                    ),
                    bassMidi = intArrayOf(
                        45, 0, 57, 45, 45, 0, 57, 45, 45, 0, 57, 45, 45, 0, 57, 45,
                        40, 0, 52, 40, 40, 0, 52, 40, 45, 0, 57, 45, 45, 0, 57, 45,
                        45, 0, 57, 45, 40, 0, 52, 40, 45, 0, 57, 45, 45, 0, 57, 45,
                        48, 0, 60, 48, 43, 0, 55, 43, 41, 0, 53, 41, 40, 0, 52, 40
                    )
                ),

                // Grieg — In the Hall of the Mountain King (64 steps)
                ClassicalTechnoTrack.HALL_MOUNTAIN_BASS to StepPattern(
                    leadMidi = intArrayOf(
                        // Bar 1: B4 C#5 D5 E5 F#5 D5 F#5
                        71, 73, 74, 76, 78, 74, 78, 0, 77, 73, 77, 0, 76, 72, 76, 0,
                        // Bar 2: Repeat + octave climb
                        71, 73, 74, 76, 78, 74, 78, 83, 81, 78, 74, 78, 81, 81, 0, 0,
                        // Bar 3: High F# minor transpose escalation
                        78, 80, 81, 83, 85, 81, 85, 0, 84, 80, 84, 0, 83, 79, 83, 0,
                        // Bar 4: Climactic rave run back down to B
                        78, 80, 81, 83, 85, 81, 85, 88, 86, 85, 83, 81, 79, 78, 76, 74
                    ),
                    bassMidi = intArrayOf(
                        35, 0, 47, 35, 35, 0, 47, 35, 37, 0, 49, 37, 36, 0, 48, 36,
                        35, 0, 47, 35, 35, 0, 47, 35, 42, 0, 54, 42, 42, 0, 54, 42,
                        42, 0, 54, 42, 42, 0, 54, 42, 44, 0, 56, 44, 43, 0, 55, 43,
                        42, 0, 54, 42, 42, 0, 54, 42, 47, 0, 59, 47, 42, 0, 54, 35
                    )
                ),

                // Mozart — Rondo Alla Turca / Turkish March (64 steps)
                ClassicalTechnoTrack.TURKISH_MARCH_EDM to StepPattern(
                    leadMidi = intArrayOf(
                        // Bar 1: B4 A4 G#4 A4 C5 - D5 C5 B4 C5 E5
                        71, 69, 68, 69, 72, 72, 0, 0, 74, 72, 71, 72, 76, 76, 0, 0,
                        // Bar 2: F5 E5 D#5 E5 B5 A5 G#5 A5 B5 A5 G#5 A5 C6
                        77, 76, 75, 76, 83, 81, 80, 81, 83, 81, 80, 81, 84, 84, 0, 0,
                        // Bar 3: Rapid classical octave trill A5 C6 B5 A5 G5 A5
                        81, 84, 83, 81, 79, 81, 83, 81, 79, 81, 83, 81, 79, 78, 76, 0,
                        // Bar 4: Heroic A minor cadence
                        76, 79, 78, 76, 74, 76, 78, 76, 74, 72, 71, 69, 72, 71, 69, 64
                    ),
                    bassMidi = intArrayOf(
                        45, 0, 57, 45, 45, 0, 57, 45, 40, 0, 52, 40, 40, 0, 52, 40,
                        45, 0, 57, 45, 45, 0, 57, 45, 45, 0, 57, 45, 48, 0, 60, 48,
                        45, 0, 57, 45, 43, 0, 55, 43, 41, 0, 53, 41, 40, 0, 52, 40,
                        48, 0, 60, 48, 43, 0, 55, 43, 40, 0, 52, 40, 45, 0, 57, 45
                    )
                ),

                // Verdi / Trad. — Dies Irae Slowed Dark Boss Techno (86 BPM, D Phrygian/Minor, 64 steps)
                ClassicalTechnoTrack.DIES_IRAE_DOOM to StepPattern(
                    leadMidi = intArrayOf(
                        // Bar 1: Heavy ominous Dies Irae motif (F4 - E4 - F4 - D4 | E4 - C#4 - D4)
                        65, 65, 64, 64, 65, 65, 62, 62, 64, 64, 61, 61, 62, 62, 0, 0,
                        // Bar 2: Threatening lower octave answer + tritone sting (F3-E3-F3-D3-G#4)
                        53, 53, 52, 52, 53, 53, 50, 50, 52, 52, 50, 50, 56, 56, 57, 0,
                        // Bar 3: Rising chromatic dread (D4 -> E4 -> F4 -> G#4 -> A4 hold -> G4)
                        62, 62, 64, 64, 65, 65, 68, 68, 69, 69, 69, 69, 67, 67, 65, 0,
                        // Bar 4: Crushing finale descent (F4 - E4 - D4 - C#4 - D4 doom hold)
                        65, 65, 64, 64, 62, 62, 61, 61, 62, 62, 57, 57, 50, 50, 50, 0
                    ),
                    bassMidi = intArrayOf(
                        38, 38, 0, 38, 38, 0, 38, 44, 38, 38, 0, 38, 37, 37, 0, 33,
                        38, 38, 0, 38, 38, 0, 38, 44, 34, 34, 0, 34, 33, 33, 0, 33,
                        38, 38, 0, 38, 41, 41, 0, 41, 44, 44, 0, 44, 45, 45, 0, 45,
                        34, 34, 0, 34, 33, 33, 0, 33, 38, 38, 0, 38, 26, 26, 38, 33
                    )
                ),

                // Mussorgsky — Night on Bald Mountain Slowed Industrial Boss Techno (92 BPM, D Minor, 64 steps)
                ClassicalTechnoTrack.BALD_MOUNTAIN_DOOM to StepPattern(
                    leadMidi = intArrayOf(
                        // Bar 1: Menacing low D-minor whirlwind crawl + demonic brass stab (Bb4-A4-G#4-A4)
                        50, 53, 55, 56, 57, 53, 50, 0, 70, 70, 69, 69, 68, 68, 69, 0,
                        // Bar 2: Second sinister ascent + tritone shriek (F4-G#4-A4-D5-C#5-Bb4-A4)
                        53, 56, 57, 60, 62, 57, 53, 0, 74, 74, 73, 73, 70, 70, 69, 0,
                        // Bar 3: Relentless pounding ostinato (D4-F4-G#4-A4 pulsing)
                        62, 0, 65, 0, 68, 0, 69, 0, 62, 65, 68, 69, 74, 73, 70, 69,
                        // Bar 4: Heavy threatening cadence down to low D3
                        65, 64, 62, 61, 62, 57, 56, 55, 53, 52, 50, 50, 49, 49, 50, 0
                    ),
                    bassMidi = intArrayOf(
                        38, 0, 38, 38, 45, 0, 38, 0, 34, 34, 0, 33, 32, 32, 33, 0,
                        38, 0, 38, 38, 45, 0, 38, 0, 38, 38, 0, 37, 34, 34, 33, 0,
                        38, 38, 41, 41, 44, 44, 45, 45, 38, 38, 41, 44, 45, 45, 34, 33,
                        38, 0, 37, 0, 38, 0, 32, 0, 33, 33, 26, 26, 37, 37, 38, 33
                    )
                )
            )
        }

        private fun midiToHz(midi: Int): Double {
            return 440.0 * 2.0.pow((midi - 69) / 12.0)
        }

        /**
         * Stateless deterministic PCM synthesis for a block of [count] samples starting at
         * [startSample]. Exposed publicly so unit tests can verify DSP quality directly.
         */
        fun renderPcmBlock(
            track: ClassicalTechnoTrack,
            startSample: Long,
            count: Int,
            duckFactor: Float = 1.0f
        ): ShortArray {
            if (count <= 0) return ShortArray(0)
            val pattern = patterns.getValue(track)
            val patternLen = pattern.leadMidi.size
            val samplesPerStep = (SAMPLE_RATE * 60.0) / (track.bpm * 4.0)
            val stepDurSec = samplesPerStep / SAMPLE_RATE
            val isBoss = track.isBossTrack
            val out = ShortArray(count)
            val gain = MASTER_MUSIC_GAIN * duckFactor.coerceIn(0f, 1.5f)

            for (i in 0 until count) {
                val absSample = startSample + i
                val exactStep = absSample.toDouble() / samplesPerStep
                val totalStepIdx = exactStep.toLong()
                val step = ((totalStepIdx % patternLen) + patternLen).toInt() % patternLen
                val fracInStep = exactStep - totalStepIdx
                val t = fracInStep * stepDurSec

                // Beat position (quarter note = 4 sixteenth steps) for sidechain pump & kick tail
                val stepInBeat = step % 4
                val beatT = (stepInBeat + fracInStep) * stepDurSec

                // 1. Kick Drum: normal 909 techno punch vs dark industrial doom kick (sub-low + heavy saturation)
                var kick = 0.0
                val kickMaxSec = if (isBoss) 0.34 else 0.24
                if (beatT < kickMaxSec) {
                    val floorHz = if (isBoss) 38.0 else 46.0
                    val sweepSpanHz = if (isBoss) 115.0 else 140.0
                    val sweepRate = if (isBoss) 22.0 else 28.0
                    val kickPhase = 2.0 * PI * (floorHz * beatT + (sweepSpanHz / sweepRate) * (1.0 - exp(-beatT * sweepRate)))
                    val kickEnv = exp(-beatT * if (isBoss) 6.8 else 10.5)
                    val clickHz = if (isBoss) 640.0 else 950.0
                    val click = if (beatT < 0.009) sin(2.0 * PI * clickHz * beatT) * (1.0 - beatT / 0.009) * 0.45 else 0.0
                    val rawKick = sin(kickPhase) * kickEnv + click
                    kick = if (isBoss) tanh(rawKick * 1.65) * 0.96 else rawKick * 0.92
                }

                // 2. Snare / Industrial Anvil Slam on beats 2 and 4 (steps 4 and 12 within 16)
                var snare = 0.0
                val stepInBar = step % 16
                val snareBeatT = when {
                    stepInBar in 4..7 -> ((stepInBar - 4) + fracInStep) * stepDurSec
                    stepInBar in 12..15 -> ((stepInBar - 12) + fracInStep) * stepDurSec
                    else -> -1.0
                }
                val snareMaxSec = if (isBoss) 0.26 else 0.18
                if (snareBeatT in 0.0..snareMaxSec) {
                    val body = if (isBoss) {
                        // Dark metallic industrial anvil/tritone clang + low thud
                        (sin(2.0 * PI * 145.0 * snareBeatT) * 0.40 +
                            sin(2.0 * PI * 311.13 * snareBeatT) * 0.22 +
                            sin(2.0 * PI * 440.0 * snareBeatT) * 0.16) * exp(-snareBeatT * 14.0)
                    } else {
                        sin(2.0 * PI * 195.0 * snareBeatT) * exp(-snareBeatT * 24.0) * 0.35
                    }
                    val noise = pseudoNoise(absSample) - 0.5 * pseudoNoise(absSample - 1)
                    val clapEnv = exp(-snareBeatT * if (isBoss) 16.0 else 28.0)
                    snare = (body + noise * clapEnv * if (isBoss) 0.65 else 0.55) * if (isBoss) 0.72 else 0.65
                }

                // 3. Hi-Hats (bright offbeats for normal tracks vs menacing tense industrial ticks for boss tracks)
                val isOffbeat = (step % 4 == 2)
                val hatDecay = if (isBoss) (if (isOffbeat) 20.0 else 48.0) else (if (isOffbeat) 26.0 else 65.0)
                val hatLevel = if (isBoss) {
                    if (isOffbeat) 0.16 else if (step % 2 == 1) 0.06 else 0.0
                } else {
                    if (isOffbeat) 0.24 else if (step % 2 == 1) 0.08 else 0.0
                }
                var hiHat = 0.0
                if (hatLevel > 0.0 && t < 0.16) {
                    val metallicFreq1 = if (isBoss) 2180.0 else 3340.0
                    val metallicFreq2 = if (isBoss) 3110.0 else 4890.0
                    val metallic = sin(2.0 * PI * metallicFreq1 * t) * 0.4 + sin(2.0 * PI * metallicFreq2 * t) * 0.3
                    val hpNoise = pseudoNoise(absSample) - pseudoNoise(absSample - 1)
                    hiHat = (hpNoise * 0.7 + metallic * 0.3) * exp(-t * hatDecay) * hatLevel
                }

                // Sidechain pump ducks bass & lead briefly on every kick hit
                val sidechainDuck = 1.0 - (if (isBoss) 0.74 else 0.68) * exp(-beatT * if (isBoss) 9.5 else 14.0)

                // 4. Bassline: Acid saw (normal) vs Dark Sub-Harmonic Industrial Growl (boss)
                var bass = 0.0
                val bassNote = pattern.bassMidi[step]
                if (bassNote > 0) {
                    val f = midiToHz(bassNote)
                    val attack = (t / 0.005).coerceIn(0.0, 1.0)
                    val decay = exp(-t * if (isBoss) 3.4 else 6.5)
                    val bright = exp(-t * if (isBoss) 7.5 else 11.0)
                    val w = 2.0 * PI * f * t
                    val saw = sin(w) +
                        0.50 * bright * sin(2.0 * w) +
                        0.30 * bright * sin(3.0 * w) +
                        0.18 * bright * sin(4.0 * w)
                    bass = if (isBoss) {
                        val sub = sin(0.5 * w) * 0.65
                        val growl = tanh((saw + sub) * 1.45)
                        growl * 0.40 * attack * decay * sidechainDuck
                    } else {
                        saw * 0.34 * attack * decay * sidechainDuck
                    }
                }

                // 5. Classical Lead: Crisp bright supersaw (normal) vs Dark Heavy Detuned Doom Brass/Choir (boss)
                var lead = 0.0
                val leadNote = pattern.leadMidi[step]
                if (leadNote > 0) {
                    val f1 = midiToHz(leadNote)
                    val nextStepHeld = pattern.leadMidi[(step + 1) % patternLen] == leadNote
                    val attack = (t / if (isBoss) 0.012 else 0.005).coerceIn(0.0, 1.0)
                    if (isBoss) {
                        val f2 = f1 * 1.0075
                        val f3 = f1 * 0.9925
                        val decayRate = if (nextStepHeld) 1.1 else 3.2
                        val env = attack * exp(-t * decayRate)
                        val w1 = 2.0 * PI * f1 * t
                        val w2 = 2.0 * PI * f2 * t
                        val w3 = 2.0 * PI * f3 * t
                        // Darker harmonic rolloff + heavy octave-down ominous sub-layer
                        val osc1 = sin(w1) + 0.38 * sin(2.0 * w1) + 0.16 * sin(3.0 * w1)
                        val osc2 = sin(w2) + 0.34 * sin(2.0 * w2) + 0.14 * sin(3.0 * w2)
                        val osc3 = sin(w3) + 0.34 * sin(2.0 * w3)
                        val doomSub = sin(0.5 * w1) * 0.48
                        lead = tanh(((osc1 + osc2 + osc3) / 3.0 + doomSub) * 1.35) * 0.32 * env * (0.40 + 0.60 * sidechainDuck)
                    } else {
                        val f2 = f1 * 1.0055
                        val decayRate = if (nextStepHeld) 1.6 else 5.2
                        val env = attack * exp(-t * decayRate)
                        val w1 = 2.0 * PI * f1 * t
                        val w2 = 2.0 * PI * f2 * t
                        val osc1 = sin(w1) + 0.45 * sin(2.0 * w1) + 0.22 * sin(3.0 * w1) + 0.10 * sin(4.0 * w1)
                        val osc2 = sin(w2) + 0.45 * sin(2.0 * w2) + 0.22 * sin(3.0 * w2)
                        val shimmer = sin(2.0 * w1) * 0.18
                        lead = ((osc1 + osc2) * 0.5 + shimmer) * 0.28 * env * (0.45 + 0.55 * sidechainDuck)
                    }
                }

                // 6. Soft analog-style saturation limiter
                val rawMix = (kick + snare + hiHat + bass + lead) * gain * 2.2
                val limited = tanh(rawMix)
                val sampleInt = (limited * Short.MAX_VALUE * 0.85)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out[i] = sampleInt.toShort()
            }
            return out
        }

        private fun pseudoNoise(index: Long): Double {
            var x = index * 6364136223846793005L + 1442695040888963407L
            x = x xor (x ushr 21)
            x = x * -7046029254386353131L
            x = x xor (x ushr 28)
            val unsignedBits = (x ushr 40) and 0xFFFFFFL
            return (unsignedBits.toDouble() / 0xFFFFFF.toDouble()) * 2.0 - 1.0
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playbackJob: Job? = null

    @Volatile
    private var isEnabled: Boolean = true

    @Volatile
    private var isAppForeground: Boolean = true

    @Volatile
    private var isPaused: Boolean = false

    @Volatile
    private var selectedTrackId: String = ClassicalTechnoTracks.AUTO_TRACK_ID

    @Volatile
    private var currentSectorId: String? = null

    @Volatile
    private var isBossFightActive: Boolean = false

    @Volatile
    private var activeTrack: ClassicalTechnoTrack =
        ClassicalTechnoTracks.resolveTrack(ClassicalTechnoTracks.AUTO_TRACK_ID, null, false)

    @Volatile
    private var isDuckedForPause: Boolean = false

    fun updateConfig(
        enabled: Boolean,
        trackId: String,
        sectorId: String? = null,
        isBossFight: Boolean = false,
        paused: Boolean = false
    ) {
        val nextTrack = ClassicalTechnoTracks.resolveTrack(trackId, sectorId, isBossFight)
        val trackChanged = nextTrack != activeTrack
        val shouldBePlaying = enabled && isAppForeground && !paused
        val wasPlaying = playbackJob?.isActive == true

        isEnabled = enabled
        selectedTrackId = trackId
        currentSectorId = sectorId
        isBossFightActive = isBossFight
        isPaused = paused
        activeTrack = nextTrack

        when {
            !shouldBePlaying -> stopLoop()
            !wasPlaying || trackChanged -> startOrRestartLoop(resetPosition = trackChanged)
        }
    }

    fun setAppInForeground(inForeground: Boolean) {
        isAppForeground = inForeground
        val shouldPlay = isEnabled && isAppForeground && !isPaused
        if (shouldPlay) {
            if (playbackJob?.isActive != true) {
                startOrRestartLoop(resetPosition = false)
            }
        } else {
            stopLoop()
        }
    }

    fun setDucking(ducked: Boolean) {
        isDuckedForPause = ducked
    }

    private fun startOrRestartLoop(resetPosition: Boolean) {
        playbackJob?.cancel()
        val trackSnapshot = activeTrack
        playbackJob = scope.launch {
            var audioTrack: AudioTrack? = null
            var sampleCursor = if (resetPosition) 0L else 0L
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(CHUNK_SAMPLES * 4)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(maxOf(minBuf, CHUNK_SAMPLES * 4))
                    .build()

                audioTrack.play()

                while (isActive && isEnabled && isAppForeground && !isPaused && activeTrack == trackSnapshot) {
                    val duck = if (isDuckedForPause) 0.45f else 1.0f
                    val chunk = renderPcmBlock(
                        track = trackSnapshot,
                        startSample = sampleCursor,
                        count = CHUNK_SAMPLES,
                        duckFactor = duck
                    )
                    val written = audioTrack.write(chunk, 0, chunk.size, AudioTrack.WRITE_BLOCKING)
                    if (written < 0) {
                        delay(50L)
                        break
                    }
                    sampleCursor += chunk.size
                }
            } catch (_: Throwable) {
                // Safe no-op in headless/Robolectric environments without audio HAL
            } finally {
                try {
                    audioTrack?.pause()
                    audioTrack?.flush()
                    audioTrack?.stop()
                } catch (_: Throwable) {
                }
                try {
                    audioTrack?.release()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun stopLoop() {
        playbackJob?.cancel()
        playbackJob = null
    }

    fun release() {
        stopLoop()
        scope.cancel()
    }
}
