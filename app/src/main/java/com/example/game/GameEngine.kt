package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.data.PlayerProgress
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GoogleGold
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.HackerNeonGreen
import com.example.ui.theme.VirusCrimson
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    val sector: SectorDefinition,
    progress: PlayerProgress
) {
    val worldHalfWidth = 720f
    val worldHalfHeight = 720f

    private val attackPower = (32f + (progress.tpuCoreLevel - 1) * 8f) * (1f + (progress.attackStat - 1) * 0.12f)
    private val maxHp = 130f + (progress.contextWindowLevel - 1) * 30f + (progress.healthStat - 1) * 22f
    private val maxShieldStamina = 100f + (progress.contextWindowLevel - 1) * 15f + (progress.staminaStat - 1) * 15f
    private val defenseReduction = ((progress.defenseStat - 1) * 0.06f).coerceAtMost(0.50f)
    private val moveSpeed = 305f + (progress.fiberOpticLevel - 1) * 18f
    private val dashCooldownBase = max(0.85f, 2.0f - (progress.fiberOpticLevel - 1) * 0.12f - (progress.staminaStat - 1) * 0.08f)
    private val critChance = 0.14f + (progress.neuralMagnetLevel - 1) * 0.04f

    private val difficultyMultiplier = when (sector.id) {
        "sector_plaza" -> 1.0f
        "sector_cloud" -> 1.35f
        "sector_darknet" -> 1.7f
        else -> 2.1f
    }

    var player = PlayerEntity(
        hp = maxHp,
        maxHp = maxHp,
        shieldStamina = maxShieldStamina,
        maxShieldStamina = maxShieldStamina,
        dashMaxCooldown = dashCooldownBase
    )
        private set

    val enemies = mutableListOf<EnemyEntity>()
    val projectiles = mutableListOf<ProjectileEntity>()
    val coins = mutableListOf<CoinEntity>()
    val items = mutableListOf<ItemDropEntity>()
    val tetrisObstacles = mutableListOf<TetrisObstacle>()
    val searchNodes = mutableListOf<SearchNodeEntity>()
    val floatingTexts = mutableListOf<FloatingText>()
    val particles = mutableListOf<ParticleEffect>()
    val shockwaves = mutableListOf<ShockwaveRing>()

    var worldTime: Float = 0f
        private set
    var currentWave: Int = 1
        private set
    var coinsCollected: Int = 0
        private set
    var virusesKilled: Int = 0
        private set
    var adwareKilled: Int = 0
    private set
    var hackersKilled: Int = 0
        private set
    var xpEarnedThisStage: Int = 0
        private set

    var isPaused: Boolean = false
    var isMissionOver: Boolean = false
        private set
    var isVictory: Boolean = false
        private set
    var starsEarned: Int = 0
        private set

    var focusedEnemyId: Int? = null
        private set
    var moveTargetX: Float? = null
        private set
    var moveTargetY: Float? = null
        private set

    private var bannerMessage: String? = "TOUCH TO MOVE • SWIPE TO SLASH • TAP ENEMY TO FOCUS"
    private var bannerTimer: Float = 3.2f
    private var nextEntityId = 1
    private var waveClearDelay = 0f
    private var comboResetTimer = 0f
    private var itemSpawnTimer = 10f
    private val pendingHaptics = mutableListOf<HapticEvent>()

    fun drainHapticEvents(): List<HapticEvent> {
        if (pendingHaptics.isEmpty()) return emptyList()
        val copy = pendingHaptics.toList()
        pendingHaptics.clear()
        return copy
    }

    private fun emitHaptic(event: HapticEvent) {
        if (pendingHaptics.size < 8) {
            pendingHaptics.add(event)
        }
    }

    init {
        spawnTetrisObstacles()
        spawnSearchNodes(count = 2 + (sector.waves / 2))
        spawnStarterItems()
        spawnWave(1)
    }

    private fun spawnTetrisObstacles() {
        val baseShapes = listOf(
            // L-Block
            listOf(TetrisCell(0, 0), TetrisCell(0, 1), TetrisCell(0, 2), TetrisCell(1, 2)),
            // J-Block
            listOf(TetrisCell(0, 0), TetrisCell(0, 1), TetrisCell(0, 2), TetrisCell(-1, 2)),
            // T-Block
            listOf(TetrisCell(-1, 0), TetrisCell(0, 0), TetrisCell(1, 0), TetrisCell(0, 1)),
            // Z-Block
            listOf(TetrisCell(-1, 0), TetrisCell(0, 0), TetrisCell(0, 1), TetrisCell(1, 1)),
            // I-Block
            listOf(TetrisCell(-1, 0), TetrisCell(0, 0), TetrisCell(1, 0)),
            // O-Block
            listOf(TetrisCell(0, 0), TetrisCell(1, 0), TetrisCell(0, 1), TetrisCell(1, 1))
        )
        val colors = listOf(
            GeminiCyan,
            com.example.ui.theme.AdwareMagenta,
            GoogleGold,
            com.example.ui.theme.GeminiBlue,
            GoogleGreen
        )

        val targetCount = 5 + Random.nextInt(3) // 5 to 7 random obstacles per run
        var attempts = 0
        while (tetrisObstacles.size < targetCount && attempts < 80) {
            attempts++
            val wx = (Random.nextFloat() * 2f - 1f) * (worldHalfWidth - 160f)
            val wy = (Random.nextFloat() * 2f - 1f) * (worldHalfHeight - 160f)

            // Keep center spawn clear and avoid overlapping existing obstacles
            if (hypot(wx, wy) < 170f) continue
            val tooClose = tetrisObstacles.any { hypot(it.worldX - wx, it.worldY - wy) < 165f }
            if (tooClose) continue

            val rawCells = baseShapes.random()
            val rotSteps = Random.nextInt(4)
            val rotatedCells = rawCells.map { cell ->
                var gx = cell.gx
                var gy = cell.gy
                repeat(rotSteps) {
                    val nx = -gy
                    val ny = gx
                    gx = nx
                    gy = ny
                }
                TetrisCell(gx, gy)
            }

            tetrisObstacles.add(
                TetrisObstacle(
                    id = nextEntityId++,
                    worldX = wx,
                    worldY = wy,
                    cells = rotatedCells,
                    color = colors.random()
                )
            )
        }
    }

    private fun spawnStarterItems() {
        repeat(3 + Random.nextInt(2)) {
            spawnRandomItemInLevel()
        }
    }

    private fun spawnRandomItemInLevel() {
        if (items.size >= 6) return
        repeat(20) {
            val wx = (Random.nextFloat() * 2f - 1f) * (worldHalfWidth - 90f)
            val wy = (Random.nextFloat() * 2f - 1f) * (worldHalfHeight - 90f)
            if (hypot(wx - player.x, wy - player.y) > 90f && !hitsTetrisObstacle(wx, wy, 28f)) {
                items.add(
                    ItemDropEntity(
                        id = nextEntityId++,
                        type = ItemUpgradeType.entries.random(),
                        x = wx,
                        y = wy
                    )
                )
                return
            }
        }
    }

    private fun spawnSearchNodes(count: Int) {
        val labels = listOf("INDEX α", "QUERY β", "CACHE γ", "KNOWLEDGE δ")
        for (i in 0 until count) {
            val angle = (i.toFloat() / count) * (2f * PI.toFloat()) + 0.4f
            val dist = 330f
            searchNodes.add(
                SearchNodeEntity(
                    id = nextEntityId++,
                    x = cos(angle) * dist,
                    y = sin(angle) * dist,
                    label = labels[i % labels.size]
                )
            )
        }
    }

    private fun spawnWave(wave: Int) {
        currentWave = wave
        val isBossWave = wave == sector.waves
        if (isBossWave) {
            bannerMessage = "BOSS ALERT: ${sector.bossName.uppercase()}!"
            bannerTimer = 3.2f
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val bossHp = EnemyType.SECTOR_BOSS.baseHp * difficultyMultiplier
            enemies.add(
                EnemyEntity(
                    id = nextEntityId++,
                    type = EnemyType.SECTOR_BOSS,
                    x = cos(angle) * 420f,
                    y = sin(angle) * 420f,
                    hp = bossHp,
                    maxHp = bossHp
                )
            )
            repeat(3) {
                spawnSingleEnemy(
                    if (it % 2 == 0) EnemyType.TROJAN_VIRUS else EnemyType.POPUP_ADWARE
                )
            }
        } else {
            bannerMessage = "WAVE $wave / ${sector.waves} • DEFEND THE WEB"
            bannerTimer = 2.4f
            val count = 4 + wave * 2
            repeat(count) { idx ->
                val type = when {
                    sector.id == "sector_plaza" && wave == 1 ->
                        if (idx % 3 == 0) EnemyType.POPUP_ADWARE else EnemyType.TROJAN_VIRUS
                    idx % 4 == 3 -> EnemyType.PESKY_HACKER
                    idx % 3 == 1 -> EnemyType.POPUP_ADWARE
                    else -> EnemyType.TROJAN_VIRUS
                }
                spawnSingleEnemy(type)
            }
        }
    }

    private fun spawnSingleEnemy(type: EnemyType) {
        val angle = Random.nextFloat() * 2f * PI.toFloat()
        val dist = 240f + Random.nextFloat() * 340f
        val hp = type.baseHp * difficultyMultiplier
        enemies.add(
            EnemyEntity(
                id = nextEntityId++,
                type = type,
                x = (cos(angle) * dist).coerceIn(-worldHalfWidth + 60f, worldHalfWidth - 60f),
                y = (sin(angle) * dist).coerceIn(-worldHalfHeight + 60f, worldHalfHeight - 60f),
                hp = hp,
                maxHp = hp,
                attackTimer = 0.9f + Random.nextFloat() * 1.1f,
                dashTimer = 1.5f + Random.nextFloat() * 1.5f,
                wanderAngle = Random.nextFloat() * 2f * PI.toFloat(),
                wanderTimer = 1.0f + Random.nextFloat() * 2.0f
            )
        )
    }

    fun setGuarding(active: Boolean) {
        if (isPaused || isMissionOver) return
        val canGuard = active && player.shieldStamina > 5f
        player = player.copy(isGuarding = canGuard)
    }

    fun setMoveTarget(wx: Float?, wy: Float?) {
        if (isPaused || isMissionOver) return
        if (wx == null || wy == null) {
            moveTargetX = null
            moveTargetY = null
        } else {
            focusedEnemyId = null
            moveTargetX = wx.coerceIn(-worldHalfWidth + 32f, worldHalfWidth - 32f)
            moveTargetY = wy.coerceIn(-worldHalfHeight + 32f, worldHalfHeight - 32f)
        }
    }

    fun focusEnemyAt(wx: Float, wy: Float) {
        if (isPaused || isMissionOver) return
        val tappedEnemy = enemies
            .filter { hypot(it.x - wx, it.y - wy) <= 135f }
            .minByOrNull { hypot(it.x - wx, it.y - wy) }
            ?: enemies.minByOrNull { hypot(it.x - player.x, it.y - player.y) }
                ?.takeIf { hypot(it.x - wx, it.y - wy) <= 240f }

        if (tappedEnemy != null) {
            focusedEnemyId = tappedEnemy.id
            moveTargetX = null
            moveTargetY = null
            spawnFloatingText(tappedEnemy.x, tappedEnemy.y - 34f, "LOCKED", GeminiCyan)
        } else {
            // Tapped open ground: walk there
            focusedEnemyId = null
            moveTargetX = wx.coerceIn(-worldHalfWidth + 32f, worldHalfWidth - 32f)
            moveTargetY = wy.coerceIn(-worldHalfHeight + 32f, worldHalfHeight - 32f)
        }
    }

    fun triggerSwordSlash(explicitAngle: Float? = null) {
        if (isPaused || isMissionOver || player.slashCooldown > 0f) return

        var swingAngle = explicitAngle ?: player.facingAngle
        if (explicitAngle == null) {
            val nearest = enemies.minByOrNull { hypot(it.x - player.x, it.y - player.y) }
            if (nearest != null && hypot(nearest.x - player.x, nearest.y - player.y) < 190f) {
                swingAngle = atan2(nearest.y - player.y, nearest.x - player.x)
            }
        } else {
            // Manual swipe clears auto-walk target
            moveTargetX = null
            moveTargetY = null
        }

        val comboStep = if (comboResetTimer > 0f) (player.slashComboStep + 1) % 3 else 0
        comboResetTimer = 0.85f
        val isSpinCleave = comboStep == 2
        val reach = if (isSpinCleave) 155f else 135f
        val mult = if (player.attackMultTimer > 0f) 2.0f else 1.0f
        val damageMult = (if (isSpinCleave) 1.55f else 1.0f) * mult

        // Small forward lunge on slash
        val lungeX = (player.x + cos(swingAngle) * 18f).coerceIn(-worldHalfWidth + 32f, worldHalfWidth - 32f)
        val lungeY = (player.y + sin(swingAngle) * 18f).coerceIn(-worldHalfHeight + 32f, worldHalfHeight - 32f)

        player = player.copy(
            x = lungeX,
            y = lungeY,
            facingAngle = swingAngle,
            isGuarding = false,
            slashCooldown = if (isSpinCleave) 0.36f else player.slashMaxCooldown,
            slashActiveTimer = 0.22f,
            slashComboStep = comboStep
        )
        var hitHeavy = isSpinCleave
        var hitAny = false

        val eIter = enemies.listIterator()
        while (eIter.hasNext()) {
            val enemy = eIter.next()
            val dx = enemy.x - player.x
            val dy = enemy.y - player.y
            val dist = hypot(dx, dy)
            if (dist <= reach + enemy.type.radius) {
                val angleToEnemy = atan2(dy, dx)
                val angleDiff = abs(normalizeAngle(angleToEnemy - swingAngle))
                if (isSpinCleave || angleDiff <= 1.25f) {
                    val isCrit = Random.nextFloat() < critChance || isSpinCleave
                    val dmg = attackPower * damageMult * (if (isCrit && !isSpinCleave) 1.65f else 1.0f)
                    val knockback = if (isSpinCleave) 68f else 42f
                    val pushedX = (enemy.x + cos(angleToEnemy) * knockback).coerceIn(-worldHalfWidth + 40f, worldHalfWidth - 40f)
                    val pushedY = (enemy.y + sin(angleToEnemy) * knockback).coerceIn(-worldHalfHeight + 40f, worldHalfHeight - 40f)
                    val updated = enemy.copy(
                        x = pushedX,
                        y = pushedY,
                        hp = enemy.hp - dmg,
                        hitFlashTimer = 0.16f
                    )
                    spawnFloatingText(
                        enemy.x,
                        enemy.y - 26f,
                        if (isCrit) "CRIT -${dmg.toInt()}!" else "-${dmg.toInt()}",
                        if (isCrit) GoogleGold else Color.White,
                        isCrit = isCrit
                    )
                    spawnParticles(enemy.x, enemy.y, GeminiCyan, count = 7)
                    hitAny = true
                    if (isCrit) hitHeavy = true
                    if (updated.hp <= 0f) {
                        eIter.remove()
                        onEnemyDefeated(updated)
                    } else {
                        eIter.set(updated)
                    }
                }
            }
        }

        // Reflect enemy projectiles back at them!
        val pIter = projectiles.listIterator()
        while (pIter.hasNext()) {
            val proj = pIter.next()
            if (!proj.fromPlayer) {
                val dist = hypot(proj.x - player.x, proj.y - player.y)
                if (dist <= reach + 32f) {
                    val targetEnemy = enemies.minByOrNull { hypot(it.x - proj.x, it.y - proj.y) }
                    val speed = hypot(proj.vx, proj.vy) * 1.65f
                    val reflectAngle = if (targetEnemy != null) {
                        atan2(targetEnemy.y - proj.y, targetEnemy.x - proj.x)
                    } else {
                        atan2(-proj.vy, -proj.vx)
                    }
                    spawnFloatingText(proj.x, proj.y - 20f, "REFLECT!", GoogleGold, isCrit = true)
                    spawnParticles(proj.x, proj.y, GeminiCyan, count = 8)
                    hitAny = true
                    hitHeavy = true
                    pIter.set(
                        proj.copy(
                            fromPlayer = true,
                            vx = cos(reflectAngle) * speed,
                            vy = sin(reflectAngle) * speed,
                            damage = proj.damage * 1.6f * (if (player.attackMultTimer > 0f) 2f else 1f),
                            radius = proj.radius * 1.25f,
                            color = GoogleGold,
                            life = 1.8f
                        )
                    )
                }
            }
        }
        emitHaptic(if (hitHeavy || hitAny) HapticEvent.HEAVY_ATTACK else HapticEvent.LIGHT_ATTACK)
    }

    fun triggerShieldBash() {
        if (isPaused || isMissionOver || player.shieldBashCooldown > 0f) return

        val bashRadius = 175f
        shockwaves.add(
            ShockwaveRing(
                x = player.x,
                y = player.y,
                radius = 24f,
                maxRadius = bashRadius,
                color = GoogleGold,
                life = 0.38f,
                maxLife = 0.38f
            )
        )

        val eIter = enemies.listIterator()
        while (eIter.hasNext()) {
            val enemy = eIter.next()
            val dx = enemy.x - player.x
            val dy = enemy.y - player.y
            val dist = hypot(dx, dy)
            if (dist <= bashRadius) {
                val dmg = attackPower * 1.15f
                val pushAngle = atan2(dy, dx)
                val pushedX = (enemy.x + cos(pushAngle) * 95f).coerceIn(-worldHalfWidth + 40f, worldHalfWidth - 40f)
                val pushedY = (enemy.y + sin(pushAngle) * 95f).coerceIn(-worldHalfHeight + 40f, worldHalfHeight - 40f)
                val updated = enemy.copy(
                    x = pushedX,
                    y = pushedY,
                    hp = enemy.hp - dmg,
                    hitFlashTimer = 0.18f,
                    stunTimer = 1.1f
                )
                spawnFloatingText(enemy.x, enemy.y - 26f, "STUN -${dmg.toInt()}", GoogleGold, isCrit = true)
                if (updated.hp <= 0f) {
                    eIter.remove()
                    onEnemyDefeated(updated)
                } else {
                    eIter.set(updated)
                }
            }
        }

        projectiles.removeAll { !it.fromPlayer && hypot(it.x - player.x, it.y - player.y) <= bashRadius }

        player = player.copy(
            shieldBashCooldown = player.shieldBashMaxCooldown,
            invulnTimer = max(player.invulnTimer, 0.35f)
        )
        spawnParticles(player.x, player.y, GoogleGold, count = 14)
        emitHaptic(HapticEvent.HEAVY_ATTACK)
    }

    fun triggerDash(joyX: Float, joyY: Float) {
        if (isPaused || isMissionOver || player.dashCooldown > 0f) return

        val mag = hypot(joyX, joyY)
        val angle = if (mag > 0.15f) atan2(joyY, joyX) else player.facingAngle
        val dashSpeed = moveSpeed * 3.1f
        player = player.copy(
            vx = cos(angle) * dashSpeed,
            vy = sin(angle) * dashSpeed,
            facingAngle = angle,
            isGuarding = false,
            dashActiveTimer = 0.20f,
            invulnTimer = max(player.invulnTimer, 0.30f),
            dashCooldown = player.dashMaxCooldown
        )
        spawnParticles(player.x, player.y, GeminiCyan, count = 12)
        emitHaptic(HapticEvent.DASH)
    }

    fun tick(rawDt: Float, joyX: Float, joyY: Float) {
        if (isPaused || isMissionOver) return
        val dt = rawDt.coerceIn(0.001f, 0.05f)
        worldTime += dt

        if (bannerTimer > 0f) {
            bannerTimer -= dt
            if (bannerTimer <= 0f) bannerMessage = null
        }
        if (comboResetTimer > 0f) {
            comboResetTimer = max(0f, comboResetTimer - dt)
        }

        updatePlayer(dt, joyX, joyY)
        updateAntiVirusGun(dt)
        updateSearchNodes(dt)
        updateEnemies(dt)
        updateProjectiles(dt)
        updateCoins(dt)
        updateItems(dt)
        itemSpawnTimer -= dt
        if (itemSpawnTimer <= 0f) {
            itemSpawnTimer = 11f + Random.nextFloat() * 5f
            spawnRandomItemInLevel()
        }
        updateEffects(dt)
        checkWaveProgression(dt)
    }

    private fun updateAntiVirusGun(dt: Float) {
        if (player.antiVirusGunTimer <= 0f) return
        val fireCd = max(0f, player.antiVirusFireCooldown - dt)
        if (fireCd <= 0f && enemies.isNotEmpty()) {
            val target = enemies.minByOrNull { hypot(it.x - player.x, it.y - player.y) }
            if (target != null && hypot(target.x - player.x, target.y - player.y) < 460f) {
                val angle = atan2(target.y - player.y, target.x - player.x)
                val gunX = player.x + cos(worldTime * 3.5f) * 30f
                val gunY = player.y + sin(worldTime * 3.5f) * 30f
                val mult = if (player.attackMultTimer > 0f) 2.0f else 1.0f
                projectiles.add(
                    ProjectileEntity(
                        id = nextEntityId++,
                        fromPlayer = true,
                        x = gunX,
                        y = gunY,
                        vx = cos(angle) * 580f,
                        vy = sin(angle) * 580f,
                        damage = attackPower * 0.65f * mult,
                        radius = 8f,
                        color = GoogleGreen,
                        life = 1.4f
                    )
                )
                player = player.copy(antiVirusFireCooldown = 0.36f)
                return
            }
        }
        player = player.copy(antiVirusFireCooldown = fireCd)
    }

    private fun updatePlayer(dt: Float, joyX: Float, joyY: Float) {
        val p = player
        val newSlashCd = max(0f, p.slashCooldown - dt)
        val newSlashActive = max(0f, p.slashActiveTimer - dt)
        val newBashCd = max(0f, p.shieldBashCooldown - dt)
        val newDashCd = max(0f, p.dashCooldown - dt)
        val newInvuln = max(0f, p.invulnTimer - dt)
        val newHitFlash = max(0f, p.hitFlashTimer - dt)

        // Shield stamina drain / regen
        var stamina = p.shieldStamina
        var guarding = p.isGuarding
        if (guarding) {
            stamina = max(0f, stamina - 16f * dt)
            if (stamina <= 0f) guarding = false
        } else {
            stamina = min(p.maxShieldStamina, stamina + 22f * dt)
        }

        val nx: Float
        val ny: Float
        val nvx: Float
        val nvy: Float
        val nAngle: Float
        var walkPhase = p.walkPhase

        val speedBoost = if (p.speedUpTimer > 0f) 1.45f else 1.0f
        if (p.dashActiveTimer > 0f) {
            val rawNx = (p.x + p.vx * dt).coerceIn(-worldHalfWidth + 32f, worldHalfWidth - 32f)
            val rawNy = (p.y + p.vy * dt).coerceIn(-worldHalfHeight + 32f, worldHalfHeight - 32f)
            val (resolvedX, resolvedY) = resolveTetrisCollisions(rawNx, rawNy, 20f)
            nx = resolvedX
            ny = resolvedY
            nvx = p.vx
            nvy = p.vy
            nAngle = p.facingAngle
            walkPhase += dt * 16f
        } else {
            val speedMult = (if (guarding) 0.55f else 1.0f) * speedBoost

            // Determine desired movement direction: joystick, focused enemy auto-chase, or touch moveTarget
            var dirX = joyX
            var dirY = joyY
            var mag = hypot(dirX, dirY).coerceAtMost(1f)

            val focused = focusedEnemyId?.let { id -> enemies.firstOrNull { it.id == id } }
            if (focusedEnemyId != null && focused == null) {
                focusedEnemyId = null
            }

            if (mag <= 0.08f && focused != null) {
                val dx = focused.x - p.x
                val dy = focused.y - p.y
                val dist = hypot(dx, dy)
                val angleToTarget = atan2(dy, dx)
                if (dist > 95f) {
                    dirX = cos(angleToTarget)
                    dirY = sin(angleToTarget)
                    mag = 1f
                } else {
                    // Within melee range: face target and auto-attack!
                    dirX = 0f
                    dirY = 0f
                    mag = 0f
                    if (p.slashCooldown <= 0f) {
                        triggerSwordSlash(angleToTarget)
                    }
                }
            } else if (mag <= 0.08f && moveTargetX != null && moveTargetY != null) {
                val dx = moveTargetX!! - p.x
                val dy = moveTargetY!! - p.y
                val dist = hypot(dx, dy)
                if (dist > 16f) {
                    dirX = dx / dist
                    dirY = dy / dist
                    mag = 1f
                } else {
                    moveTargetX = null
                    moveTargetY = null
                }
            }

            if (mag > 0.04f) {
                nAngle = atan2(dirY, dirX)
                nvx = (dirX / max(0.001f, hypot(dirX, dirY))) * moveSpeed * mag * speedMult
                nvy = (dirY / max(0.001f, hypot(dirX, dirY))) * moveSpeed * mag * speedMult
                walkPhase += dt * 13f * mag
            } else {
                nvx = 0f
                nvy = 0f
                nAngle = if (focused != null) atan2(focused.y - p.y, focused.x - p.x) else p.facingAngle
            }
            val rawNx = (p.x + nvx * dt).coerceIn(-worldHalfWidth + 32f, worldHalfWidth - 32f)
            val rawNy = (p.y + nvy * dt).coerceIn(-worldHalfHeight + 32f, worldHalfHeight - 32f)
            val (resolvedX, resolvedY) = resolveTetrisCollisions(rawNx, rawNy, 20f)
            nx = resolvedX
            ny = resolvedY
        }

        player = p.copy(
            x = nx,
            y = ny,
            vx = nvx,
            vy = nvy,
            facingAngle = nAngle,
            isGuarding = guarding,
            shieldStamina = stamina,
            slashCooldown = newSlashCd,
            slashActiveTimer = newSlashActive,
            shieldBashCooldown = newBashCd,
            dashCooldown = newDashCd,
            dashActiveTimer = max(0f, p.dashActiveTimer - dt),
            invulnTimer = newInvuln,
            hitFlashTimer = newHitFlash,
            walkPhase = walkPhase,
            antiVirusGunTimer = max(0f, p.antiVirusGunTimer - dt),
            adBlockBubbleTimer = max(0f, p.adBlockBubbleTimer - dt),
            speedUpTimer = max(0f, p.speedUpTimer - dt),
            attackMultTimer = max(0f, p.attackMultTimer - dt)
        )
    }

    private fun updateSearchNodes(dt: Float) {
        for (i in searchNodes.indices) {
            val node = searchNodes[i]
            if (node.indexed) continue
            val dist = hypot(player.x - node.x, player.y - node.y)
            if (dist < 110f) {
                val newProg = (node.progress + dt * 0.45f).coerceAtMost(1f)
                val nowIndexed = newProg >= 1f
                searchNodes[i] = node.copy(progress = newProg, indexed = nowIndexed)
                if (nowIndexed) {
                    spawnFloatingText(node.x, node.y - 34f, "NODE INDEXED!", GoogleGreen, isCrit = true)
                    shockwaves.add(ShockwaveRing(node.x, node.y, 14f, 140f, GoogleGreen, 0.45f, 0.45f))
                    repeat(5) {
                        val a = Random.nextFloat() * 2f * PI.toFloat()
                        coins.add(
                            CoinEntity(
                                id = nextEntityId++,
                                x = node.x + cos(a) * 26f,
                                y = node.y + sin(a) * 26f,
                                z = 24f,
                                vz = 140f,
                                value = 5,
                                spinPhase = Random.nextFloat() * 6.28f
                            )
                        )
                    }
                }
            }
        }
    }

    private fun updateEnemies(dt: Float) {
        for (i in enemies.indices) {
            val e = enemies[i]
            val stun = max(0f, e.stunTimer - dt)
            if (stun > 0f) {
                enemies[i] = e.copy(
                    stunTimer = stun,
                    hitFlashTimer = max(0f, e.hitFlashTimer - dt)
                )
                continue
            }

            val dx = player.x - e.x
            val dy = player.y - e.y
            val dist = max(1f, hypot(dx, dy))
            val dirX = dx / dist
            val dirY = dy / dist
            var facing = atan2(dy, dx)
            val speed = e.type.speed * (0.95f + 0.08f * difficultyMultiplier)

            var vx = e.vx
            var vy = e.vy
            var attackTimer = e.attackTimer - dt
            var dashTimer = e.dashTimer - dt
            var wanderAngle = e.wanderAngle
            var wanderTimer = e.wanderTimer - dt

            // Small 3D enemies roam randomly around the stage until Gemini gets within aggro range
            val aggroRange = if (e.type == EnemyType.SECTOR_BOSS) 900f else 360f
            if (dist > aggroRange && e.hp >= e.maxHp) {
                if (wanderTimer <= 0f) {
                    wanderTimer = 1.4f + Random.nextFloat() * 1.8f
                    wanderAngle = Random.nextFloat() * 2f * PI.toFloat()
                }
                vx = cos(wanderAngle) * speed * 0.45f
                vy = sin(wanderAngle) * speed * 0.45f
                facing = wanderAngle
            } else {
                when (e.type) {
                EnemyType.TROJAN_VIRUS -> {
                    vx = dirX * speed
                    vy = dirY * speed
                }
                EnemyType.POPUP_ADWARE -> {
                    if (dist > 250f) {
                        vx = dirX * speed
                        vy = dirY * speed
                    } else if (dist < 150f) {
                        vx = -dirX * speed * 0.8f
                        vy = -dirY * speed * 0.8f
                    } else {
                        vx = -dirY * speed * 0.85f
                        vy = dirX * speed * 0.85f
                    }
                    if (attackTimer <= 0f) {
                        attackTimer = 2.0f
                        val boltSpeed = 270f
                        projectiles.add(
                            ProjectileEntity(
                                id = nextEntityId++,
                                fromPlayer = false,
                                x = e.x,
                                y = e.y,
                                vx = dirX * boltSpeed,
                                vy = dirY * boltSpeed,
                                damage = 12f * difficultyMultiplier,
                                radius = 9f,
                                color = e.type.color,
                                life = 2.4f
                            )
                        )
                    }
                }
                EnemyType.PESKY_HACKER -> {
                    if (dashTimer <= 0f) {
                        dashTimer = 2.1f
                        val flankAngle = facing + (if (Random.nextBoolean()) 0.8f else -0.8f)
                        vx = cos(flankAngle) * speed * 2.1f
                        vy = sin(flankAngle) * speed * 2.1f
                    } else {
                        vx = vx * 0.92f + dirX * speed * 0.12f
                        vy = vy * 0.92f + dirY * speed * 0.12f
                    }
                    if (attackTimer <= 0f) {
                        attackTimer = 1.7f
                        val boltSpeed = 310f
                        projectiles.add(
                            ProjectileEntity(
                                id = nextEntityId++,
                                fromPlayer = false,
                                x = e.x,
                                y = e.y,
                                vx = dirX * boltSpeed,
                                vy = dirY * boltSpeed,
                                damage = 14f * difficultyMultiplier,
                                radius = 8f,
                                color = HackerNeonGreen,
                                life = 2.0f
                            )
                        )
                    }
                }
                EnemyType.SECTOR_BOSS -> {
                    vx = dirX * speed
                    vy = dirY * speed
                    if (attackTimer <= 0f) {
                        attackTimer = 1.9f
                        for (k in 0 until 6) {
                            val a = facing + (k - 2.5f) * 0.28f
                            projectiles.add(
                                ProjectileEntity(
                                    id = nextEntityId++,
                                    fromPlayer = false,
                                    x = e.x,
                                    y = e.y,
                                    vx = cos(a) * 290f,
                                    vy = sin(a) * 290f,
                                    damage = 16f * difficultyMultiplier,
                                    radius = 11f,
                                    color = VirusCrimson,
                                    life = 2.6f
                                )
                            )
                        }
                    }
                }
            }
            }

            val rawNx = (e.x + vx * dt).coerceIn(-worldHalfWidth + 40f, worldHalfWidth - 40f)
            val rawNy = (e.y + vy * dt).coerceIn(-worldHalfHeight + 40f, worldHalfHeight - 40f)
            val (nx, ny) = resolveTetrisCollisions(rawNx, rawNy, e.type.radius)

            if (hypot(player.x - nx, player.y - ny) < e.type.radius + 20f) {
                damagePlayer(14f * difficultyMultiplier, nx, ny)
            }

            enemies[i] = e.copy(
                x = nx,
                y = ny,
                vx = vx,
                vy = vy,
                facingAngle = facing,
                attackTimer = attackTimer,
                dashTimer = dashTimer,
                hitFlashTimer = max(0f, e.hitFlashTimer - dt),
                stunTimer = stun,
                walkPhase = e.walkPhase + dt * 9f,
                wanderAngle = wanderAngle,
                wanderTimer = wanderTimer
            )
        }
    }

    private fun updateProjectiles(dt: Float) {
        val pIter = projectiles.listIterator()
        while (pIter.hasNext()) {
            val proj = pIter.next()
            val nx = proj.x + proj.vx * dt
            val ny = proj.y + proj.vy * dt
            val nLife = proj.life - dt

            if (nLife <= 0f || nx < -worldHalfWidth || nx > worldHalfWidth || ny < -worldHalfHeight || ny > worldHalfHeight || hitsTetrisObstacle(nx, ny, proj.radius)) {
                if (hitsTetrisObstacle(nx, ny, proj.radius)) {
                    spawnParticles(nx, ny, proj.color, count = 4)
                }
                pIter.remove()
                continue
            }

            if (proj.fromPlayer) {
                var hit = false
                val eIter = enemies.listIterator()
                while (eIter.hasNext()) {
                    val enemy = eIter.next()
                    if (hypot(enemy.x - nx, enemy.y - ny) <= enemy.type.radius + proj.radius) {
                        val dmg = proj.damage * 1.4f
                        val updated = enemy.copy(hp = enemy.hp - dmg, hitFlashTimer = 0.14f)
                        spawnFloatingText(enemy.x, enemy.y - 22f, "-${dmg.toInt()}", GeminiCyan)
                        spawnParticles(nx, ny, enemy.type.color, count = 5)
                        if (updated.hp <= 0f) {
                            eIter.remove()
                            onEnemyDefeated(updated)
                        } else {
                            eIter.set(updated)
                        }
                        hit = true
                        break
                    }
                }
                if (hit) {
                    pIter.remove()
                    continue
                }
            } else {
                if (hypot(player.x - nx, player.y - ny) <= 22f + proj.radius) {
                    damagePlayer(proj.damage, nx, ny)
                    spawnParticles(nx, ny, proj.color, count = 5)
                    pIter.remove()
                    continue
                }
            }

            pIter.set(proj.copy(x = nx, y = ny, life = nLife))
        }
    }

    private fun damagePlayer(rawAmount: Float, sourceX: Float, sourceY: Float) {
        if (player.invulnTimer > 0f || isMissionOver) return

        if (player.adBlockBubbleTimer > 0f) {
            player = player.copy(invulnTimer = 0.35f)
            spawnFloatingText(player.x, player.y - 36f, "AD-BLOCKED!", GeminiCyan, isCrit = true)
            spawnParticles(player.x, player.y, GeminiCyan, count = 6)
            emitHaptic(HapticEvent.SHIELD_BLOCK)
            return
        }

        val mitigated = rawAmount * (1f - defenseReduction)

        if (player.isGuarding && player.shieldStamina >= 10f) {
            val blockedStamina = max(0f, player.shieldStamina - 16f)
            val chipDamage = mitigated * 0.15f
            player = player.copy(
                hp = max(1f, player.hp - chipDamage),
                shieldStamina = blockedStamina,
                invulnTimer = 0.35f
            )
            spawnFloatingText(player.x, player.y - 34f, "BLOCKED!", GoogleGold, isCrit = true)
            spawnParticles(
                (player.x + sourceX) * 0.5f,
                (player.y + sourceY) * 0.5f,
                GoogleGold,
                count = 8
            )
            emitHaptic(HapticEvent.SHIELD_BLOCK)
            return
        }

        val newHp = max(0f, player.hp - mitigated)
        player = player.copy(
            hp = newHp,
            invulnTimer = 0.55f,
            hitFlashTimer = 0.22f
        )
        spawnFloatingText(player.x, player.y - 28f, "-${mitigated.toInt()}", VirusCrimson)
        emitHaptic(HapticEvent.DAMAGE_TAKEN)
        if (newHp <= 0f) {
            endMission(victory = false)
        }
    }

    private fun onEnemyDefeated(enemy: EnemyEntity) {
        when (enemy.type) {
            EnemyType.TROJAN_VIRUS -> { virusesKilled++; xpEarnedThisStage += 18 }
            EnemyType.POPUP_ADWARE -> { adwareKilled++; xpEarnedThisStage += 22 }
            EnemyType.PESKY_HACKER -> { hackersKilled++; xpEarnedThisStage += 30 }
            EnemyType.SECTOR_BOSS -> { virusesKilled++; xpEarnedThisStage += 90 }
        }
        spawnParticles(enemy.x, enemy.y, enemy.type.color, count = 14)

        // Chance to drop a grab-able Item Upgrade!
        if (enemy.type == EnemyType.SECTOR_BOSS || Random.nextFloat() < 0.28f) {
            val dropType = ItemUpgradeType.entries.random()
            items.add(
                ItemDropEntity(
                    id = nextEntityId++,
                    type = dropType,
                    x = enemy.x,
                    y = enemy.y
                )
            )
        }

        val coinCount = if (enemy.type == EnemyType.SECTOR_BOSS) 8 else 2
        val perCoin = max(2, enemy.type.coinReward / coinCount)
        repeat(coinCount) {
            val a = Random.nextFloat() * 2f * PI.toFloat()
            val offset = if (coinCount > 2) 35f else 14f
            coins.add(
                CoinEntity(
                    id = nextEntityId++,
                    x = enemy.x + cos(a) * offset,
                    y = enemy.y + sin(a) * offset,
                    z = 16f,
                    vz = 130f + Random.nextFloat() * 50f,
                    value = perCoin,
                    spinPhase = Random.nextFloat() * 6.28f
                )
            )
        }
    }

    private fun updateCoins(dt: Float) {
        val cIter = coins.listIterator()
        while (cIter.hasNext()) {
            val coin = cIter.next()
            var z = coin.z + coin.vz * dt
            var vz = coin.vz - 420f * dt
            if (z < 0f) {
                z = 0f
                vz = -vz * 0.45f
                if (abs(vz) < 25f) vz = 0f
            }

            val dx = player.x - coin.x
            val dy = player.y - coin.y
            val dist = hypot(dx, dy)

            // Items are grabbed once walked on (no long-range magnet pull)
            if (dist < 38f) {
                coinsCollected += coin.value
                spawnFloatingText(player.x, player.y - 34f, "+${coin.value}G", GoogleGold)
                emitHaptic(HapticEvent.ITEM_COLLECTED)
                cIter.remove()
            } else {
                cIter.set(
                    coin.copy(
                        z = z,
                        vz = vz,
                        spinPhase = coin.spinPhase + dt * 6f
                    )
                )
            }
        }
    }

    private fun updateItems(dt: Float) {
        val iter = items.listIterator()
        while (iter.hasNext()) {
            val item = iter.next()
            var z = item.z + item.vz * dt
            var vz = item.vz - 380f * dt
            if (z < 0f) {
                z = 0f
                vz = 0f
            }

            // Walked-on pickup
            if (hypot(player.x - item.x, player.y - item.y) < 42f) {
                when (item.type) {
                    ItemUpgradeType.ANTI_VIRUS_GUN -> player = player.copy(antiVirusGunTimer = 12f)
                    ItemUpgradeType.AD_BLOCK_BUBBLE -> player = player.copy(adBlockBubbleTimer = 10f)
                    ItemUpgradeType.SPEED_UP -> player = player.copy(speedUpTimer = 10f)
                    ItemUpgradeType.ATTACK_MULTIPLIER -> player = player.copy(attackMultTimer = 10f)
                    ItemUpgradeType.HEALTH_PACK -> player = player.copy(hp = min(player.maxHp, player.hp + 35f))
                }
                spawnFloatingText(player.x, player.y - 42f, item.type.label, item.type.color, isCrit = true)
                spawnParticles(item.x, item.y, item.type.color, count = 12)
                emitHaptic(HapticEvent.ITEM_COLLECTED)
                iter.remove()
            } else {
                iter.set(item.copy(z = z, vz = vz, bobPhase = item.bobPhase + dt * 4f))
            }
        }
    }

    private fun resolveTetrisCollisions(wx: Float, wy: Float, radius: Float): Pair<Float, Float> {
        var rx = wx
        var ry = wy
        for (obs in tetrisObstacles) {
            val half = obs.cellSize / 2f
            for (cell in obs.cells) {
                val cx = obs.worldX + cell.gx * obs.cellSize
                val cy = obs.worldY + cell.gy * obs.cellSize
                val closestX = rx.coerceIn(cx - half, cx + half)
                val closestY = ry.coerceIn(cy - half, cy + half)
                val dx = rx - closestX
                val dy = ry - closestY
                val dist = hypot(dx, dy)
                if (dist < radius && dist > 0.0001f) {
                    val push = radius - dist
                    rx += (dx / dist) * push
                    ry += (dy / dist) * push
                } else if (dist <= 0.0001f) {
                    rx += radius
                }
            }
        }
        return rx to ry
    }

    private fun hitsTetrisObstacle(wx: Float, wy: Float, radius: Float): Boolean {
        for (obs in tetrisObstacles) {
            val half = obs.cellSize / 2f
            for (cell in obs.cells) {
                val cx = obs.worldX + cell.gx * obs.cellSize
                val cy = obs.worldY + cell.gy * obs.cellSize
                val closestX = wx.coerceIn(cx - half, cx + half)
                val closestY = wy.coerceIn(cy - half, cy + half)
                if (hypot(wx - closestX, wy - closestY) < radius) return true
            }
        }
        return false
    }

    private fun updateEffects(dt: Float) {
        val fIter = floatingTexts.listIterator()
        while (fIter.hasNext()) {
            val ft = fIter.next()
            val life = ft.life - dt
            if (life <= 0f) fIter.remove() else fIter.set(ft.copy(y = ft.y - 36f * dt, life = life))
        }

        val pIter = particles.listIterator()
        while (pIter.hasNext()) {
            val pt = pIter.next()
            val life = pt.life - dt
            if (life <= 0f) pIter.remove() else pIter.set(
                pt.copy(
                    x = pt.x + pt.vx * dt,
                    y = pt.y + pt.vy * dt,
                    z = max(0f, pt.z + pt.vz * dt),
                    vz = pt.vz - 120f * dt,
                    life = life
                )
            )
        }

        val sIter = shockwaves.listIterator()
        while (sIter.hasNext()) {
            val sw = sIter.next()
            val life = sw.life - dt
            if (life <= 0f) {
                sIter.remove()
            } else {
                val t = 1f - (life / sw.maxLife)
                sIter.set(sw.copy(radius = 24f + (sw.maxRadius - 24f) * t, life = life))
            }
        }
    }

    private fun checkWaveProgression(dt: Float) {
        if (enemies.isEmpty()) {
            if (currentWave >= sector.waves) {
                endMission(victory = true)
            } else {
                waveClearDelay += dt
                if (waveClearDelay >= 1.1f) {
                    waveClearDelay = 0f
                    spawnWave(currentWave + 1)
                }
            }
        }
    }

    private fun endMission(victory: Boolean) {
        isMissionOver = true
        isVictory = victory
        if (victory) {
            coinsCollected += sector.coinBonus
            val hpRatio = player.hp / player.maxHp
            val indexedCount = searchNodes.count { it.indexed }
            val nodeRatio = if (searchNodes.isEmpty()) 1f else indexedCount.toFloat() / searchNodes.size
            val score = hpRatio * 0.6f + nodeRatio * 0.4f
            starsEarned = when {
                score >= 0.85f -> 5
                score >= 0.68f -> 4
                score >= 0.48f -> 3
                score >= 0.25f -> 2
                else -> 1
            }
        } else {
            starsEarned = 0
        }
    }

    private fun spawnFloatingText(x: Float, y: Float, text: String, color: Color, isCrit: Boolean = false) {
        floatingTexts.add(
            FloatingText(
                id = nextEntityId++,
                x = x,
                y = y,
                text = text,
                color = color,
                isCrit = isCrit
            )
        )
    }

    private fun spawnParticles(x: Float, y: Float, color: Color, count: Int) {
        repeat(count) {
            val a = Random.nextFloat() * 2f * PI.toFloat()
            val spd = 60f + Random.nextFloat() * 140f
            particles.add(
                ParticleEffect(
                    x = x,
                    y = y,
                    z = 14f,
                    vx = cos(a) * spd,
                    vy = sin(a) * spd,
                    vz = 50f + Random.nextFloat() * 80f,
                    color = color,
                    radius = 4f + Random.nextFloat() * 3.5f,
                    life = 0.35f + Random.nextFloat() * 0.25f,
                    maxLife = 0.6f
                )
            )
        }
    }

    private fun normalizeAngle(a: Float): Float {
        var res = a
        while (res > PI.toFloat()) res -= 2f * PI.toFloat()
        while (res < -PI.toFloat()) res += 2f * PI.toFloat()
        return res
    }

    fun snapshotHud(): ArenaHudState {
        val boss = enemies.firstOrNull { it.type == EnemyType.SECTOR_BOSS }
        return ArenaHudState(
            sector = sector,
            wave = currentWave,
            totalWaves = sector.waves,
            hp = player.hp,
            maxHp = player.maxHp,
            shieldStamina = player.shieldStamina,
            maxShieldStamina = player.maxShieldStamina,
            isGuarding = player.isGuarding,
            coinsCollected = coinsCollected,
            nodesIndexed = searchNodes.count { it.indexed },
            totalNodes = searchNodes.size,
            bossHp = boss?.hp,
            bossMaxHp = boss?.maxHp,
            bossName = if (boss != null) sector.bossName else null,
            slashReadyFraction = 1f - (player.slashCooldown / player.slashMaxCooldown).coerceIn(0f, 1f),
            shieldBashReadyFraction = 1f - (player.shieldBashCooldown / player.shieldBashMaxCooldown).coerceIn(0f, 1f),
            dashReadyFraction = 1f - (player.dashCooldown / player.dashMaxCooldown).coerceIn(0f, 1f),
            isPaused = isPaused,
            isMissionOver = isMissionOver,
            isVictory = isVictory,
            starsEarned = starsEarned,
            virusesKilled = virusesKilled,
            adwareKilled = adwareKilled,
            hackersKilled = hackersKilled,
            xpEarnedThisStage = xpEarnedThisStage,
            hasAntiVirusGun = player.antiVirusGunTimer > 0f,
            hasAdBlockBubble = player.adBlockBubbleTimer > 0f,
            hasSpeedUp = player.speedUpTimer > 0f,
            hasAttackMultiplier = player.attackMultTimer > 0f,
            bannerMessage = bannerMessage
        )
    }
}
