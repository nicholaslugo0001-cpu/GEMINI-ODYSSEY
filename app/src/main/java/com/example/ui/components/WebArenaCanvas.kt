package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.game.CoinEntity
import com.example.game.EnemyEntity
import com.example.game.EnemyType
import com.example.game.GameEngine
import com.example.game.ItemDropEntity
import com.example.game.ItemUpgradeType
import com.example.game.PlayerEntity
import com.example.game.SearchNodeEntity
import com.example.ui.theme.AdwareMagenta
import com.example.ui.theme.DeepWebVoid
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GoogleGold
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.HackerNeonGreen
import com.example.ui.theme.VirusCrimson
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val InkOutline = Color(0xFF070A16)

private data class ProjPoint(val x: Float, val y: Float, val scale: Float)

private sealed interface RenderItem {
    val sortY: Float
}
private data class PillarItem(val x: Float, val y: Float, override val sortY: Float = y) : RenderItem
private data class NodeItem(val node: SearchNodeEntity, override val sortY: Float = node.y) : RenderItem
private data class CoinItem(val coin: CoinEntity, override val sortY: Float = coin.y) : RenderItem
private data class EnemyItem(val enemy: EnemyEntity, override val sortY: Float = enemy.y) : RenderItem
private data class PlayerItem(val player: PlayerEntity, override val sortY: Float = player.y) : RenderItem
private data class ItemDropRender(val item: ItemDropEntity, override val sortY: Float = item.y) : RenderItem
private data class TetrisCellRender(
    val wx: Float,
    val wy: Float,
    val cellSize: Float,
    val color: Color,
    override val sortY: Float = wy
) : RenderItem

fun screenToWorld(
    sx: Float,
    sy: Float,
    viewWidth: Float,
    viewHeight: Float,
    camX: Float,
    camY: Float
): Pair<Float, Float> {
    val cx = viewWidth / 2f
    val cy = viewHeight * 0.68f
    val focalLength = 860f
    val cameraZoom = 1.32f
    val camTiltCos = 0.84f
    val camTiltSin = 0.54f

    val syDelta = (sy - cy) / cameraZoom
    val denom = (focalLength * camTiltCos + syDelta * camTiltSin).coerceAtLeast(120f)
    val dy = (syDelta * focalLength) / denom
    val depth = (focalLength - dy * camTiltSin).coerceAtLeast(180f)
    val scale = (focalLength / depth) * cameraZoom
    val dx = (sx - cx) / scale
    return (camX + dx) to (camY + dy)
}

@Composable
fun WebArenaCanvas(
    engine: GameEngine,
    frameTick: Long,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        @Suppress("UNUSED_EXPRESSION")
        frameTick

        val cx = size.width / 2f
        val cy = size.height * 0.68f
        val camX = engine.player.x
        val camY = engine.player.y
        val time = if (reducedMotion) 0f else engine.worldTime
        val primary = engine.sector.primaryColor
        val accent = engine.sector.accentColor

        // 3rd-Person High-Angle Bird's-Eye Perspective Camera
        // Ground fills the bottom/center; top of screen shows more distance
        val focalLength = 860f
        val cameraZoom = 1.32f
        val camTiltCos = 0.84f
        val camTiltSin = 0.54f

        fun project(wx: Float, wy: Float, wz: Float = 0f): ProjPoint {
            val dx = wx - camX
            val dy = wy - camY
            val depth = (focalLength - dy * camTiltSin).coerceAtLeast(180f)
            val scale = (focalLength / depth) * cameraZoom
            val sx = cx + dx * scale
            val sy = cy + (dy * camTiltCos - wz) * scale
            return ProjPoint(sx, sy, scale)
        }

        // 1. Stylized Cel-Shaded Cyber Sky & Distant Horizon at Top of Screen
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0xFF060916),
                0.22f to primary.copy(alpha = 0.24f),
                0.38f to Color(0xFF0B1228),
                1.0f to DeepWebVoid
            )
        )

        // 2. 3D Perspective Cyber-Grid Floor
        val halfW = engine.worldHalfWidth
        val halfH = engine.worldHalfHeight
        val gridStep = 120f

        var gx = -halfW
        while (gx <= halfW) {
            val p1 = project(gx, -halfH, 0f)
            val p2 = project(gx, halfH, 0f)
            val isMajor = (gx.toInt() % 240 == 0)
            drawLine(
                color = primary.copy(alpha = if (isMajor) 0.34f else 0.14f),
                start = Offset(p1.x, p1.y),
                end = Offset(p2.x, p2.y),
                strokeWidth = if (isMajor) 2.5f else 1.2f
            )
            gx += gridStep
        }

        var gy = -halfH
        while (gy <= halfH) {
            val p1 = project(-halfW, gy, 0f)
            val p2 = project(halfW, gy, 0f)
            val isMajor = (gy.toInt() % 240 == 0)
            drawLine(
                color = primary.copy(alpha = if (isMajor) 0.34f else 0.14f),
                start = Offset(p1.x, p1.y),
                end = Offset(p2.x, p2.y),
                strokeWidth = if (isMajor) 2.5f else 1.2f
            )
            gy += gridStep
        }

        // Arena Boundary
        val tl = project(-halfW, -halfH)
        val tr = project(halfW, -halfH)
        val br = project(halfW, halfH)
        val bl = project(-halfW, halfH)
        val borderPath = Path().apply {
            moveTo(tl.x, tl.y)
            lineTo(tr.x, tr.y)
            lineTo(br.x, br.y)
            lineTo(bl.x, bl.y)
            close()
        }
        drawPath(borderPath, color = InkOutline, style = Stroke(width = 7f))
        drawPath(borderPath, color = accent.copy(alpha = 0.8f), style = Stroke(width = 3.5f))

        // 3. Shockwaves on the ground
        engine.shockwaves.forEach { sw ->
            val p = project(sw.x, sw.y, 2f)
            val r = sw.radius * p.scale
            val alpha = (sw.life / sw.maxLife).coerceIn(0f, 1f)
            drawOval(
                color = InkOutline.copy(alpha = alpha * 0.7f),
                topLeft = Offset(p.x - r, p.y - r * 0.72f),
                size = Size(r * 2f, r * 1.44f),
                style = Stroke(width = 8f)
            )
            drawOval(
                color = sw.color.copy(alpha = alpha),
                topLeft = Offset(p.x - r, p.y - r * 0.72f),
                size = Size(r * 2f, r * 1.44f),
                style = Stroke(width = 4.5f)
            )
        }

        // 3b. Touch Move Destination Ring
        val tx = engine.moveTargetX
        val ty = engine.moveTargetY
        if (tx != null && ty != null) {
            val mp = project(tx, ty, 2f)
            val mr = (20f + sin(time * 8f) * 4f) * mp.scale
            drawOval(
                color = GeminiCyan.copy(alpha = 0.28f),
                topLeft = Offset(mp.x - mr, mp.y - mr * 0.72f),
                size = Size(mr * 2f, mr * 1.44f)
            )
            drawOval(
                color = GeminiCyan,
                topLeft = Offset(mp.x - mr, mp.y - mr * 0.72f),
                size = Size(mr * 2f, mr * 1.44f),
                style = Stroke(width = 3f)
            )
        }

        // 4. Painter's Algorithm Depth-Sorted 3D Renderables
        // Collect pillars, search nodes, coins, enemies, and player sorted by Y (depth)
        val renderList = mutableListOf<RenderItem>()
        // 4 Corner 3D Monoliths
        val pOff = halfW - 90f
        renderList.add(PillarItem(-pOff, -pOff))
        renderList.add(PillarItem(pOff, -pOff))
        renderList.add(PillarItem(-pOff, pOff))
        renderList.add(PillarItem(pOff, pOff))

        engine.tetrisObstacles.forEach { obs ->
            obs.cells.forEach { cell ->
                renderList.add(
                    TetrisCellRender(
                        wx = obs.worldX + cell.gx * obs.cellSize,
                        wy = obs.worldY + cell.gy * obs.cellSize,
                        cellSize = obs.cellSize,
                        color = obs.color
                    )
                )
            }
        }
        engine.searchNodes.forEach { renderList.add(NodeItem(it)) }
        engine.coins.forEach { renderList.add(CoinItem(it)) }
        engine.items.forEach { renderList.add(ItemDropRender(it)) }
        engine.enemies.forEach { renderList.add(EnemyItem(it)) }
        renderList.add(PlayerItem(engine.player))

        renderList.sortBy { it.sortY }

        renderList.forEach { item ->
            when (item) {
                is PillarItem -> {
                    val base = project(item.x, item.y, 0f)
                    val top = project(item.x, item.y, 110f)
                    drawCelPillar(base, top, primary)
                }
                is TetrisCellRender -> {
                    val base = project(item.wx, item.wy, 0f)
                    val top = project(item.wx, item.wy, item.cellSize * 0.85f)
                    drawCelTetrisCube(base, top, item.cellSize, item.color)
                }
                is NodeItem -> {
                    val base = project(item.node.x, item.node.y, 0f)
                    val crystal = project(item.node.x, item.node.y, 44f + sin(time * 3f + item.node.id) * 8f)
                    drawCelSearchNode(item.node, base, crystal, textPaint)
                }
                is CoinItem -> {
                    val ground = project(item.coin.x, item.coin.y, 0f)
                    val elev = project(item.coin.x, item.coin.y, item.coin.z + 14f)
                    drawCelGoogleCoin(item.coin, ground, elev, textPaint)
                }
                is ItemDropRender -> {
                    val ground = project(item.item.x, item.item.y, 0f)
                    val elev = project(item.item.x, item.item.y, item.item.z + 18f + sin(item.item.bobPhase) * 5f)
                    drawCelItemDrop(item.item, ground, elev, textPaint)
                }
                is EnemyItem -> {
                    val ground = project(item.enemy.x, item.enemy.y, 0f)
                    val bob = abs(sin(item.enemy.walkPhase)) * 6f
                    val body = project(item.enemy.x, item.enemy.y, bob)
                    if (engine.focusedEnemyId == item.enemy.id) {
                        val tr = (item.enemy.type.radius + 14f + sin(time * 7f) * 3f) * ground.scale
                        drawOval(
                            color = GoogleGold.copy(alpha = 0.25f),
                            topLeft = Offset(ground.x - tr, ground.y - tr * 0.65f),
                            size = Size(tr * 2f, tr * 1.3f)
                        )
                        drawOval(
                            color = GoogleGold,
                            topLeft = Offset(ground.x - tr, ground.y - tr * 0.65f),
                            size = Size(tr * 2f, tr * 1.3f),
                            style = Stroke(width = 3.5f)
                        )
                    }
                    drawCelEnemyCharacter(item.enemy, ground, body, time, textPaint)
                }
                is PlayerItem -> {
                    val ground = project(item.player.x, item.player.y, 0f)
                    val bob = abs(sin(item.player.walkPhase)) * 6f
                    val body = project(item.player.x, item.player.y, bob)
                    drawCelGeminiHero(item.player, ground, body, time)
                }
            }
        }

        // 5. Projectiles (Cel-Shaded Bolts)
        engine.projectiles.forEach { proj ->
            val p = project(proj.x, proj.y, 24f)
            val r = proj.radius * p.scale
            drawCircle(color = InkOutline, radius = r * 1.28f, center = Offset(p.x, p.y))
            drawCircle(color = proj.color, radius = r, center = Offset(p.x, p.y))
            drawCircle(color = Color.White, radius = r * 0.45f, center = Offset(p.x - r * 0.2f, p.y - r * 0.2f))
        }

        // 6. Cel-Shaded Sparks & Particles
        engine.particles.forEach { pt ->
            val p = project(pt.x, pt.y, pt.z)
            val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
            val r = pt.radius * p.scale * alpha
            if (r > 0.8f) {
                drawCircle(color = InkOutline.copy(alpha = alpha), radius = r + 1.5f, center = Offset(p.x, p.y))
                drawCircle(color = pt.color.copy(alpha = alpha), radius = r, center = Offset(p.x, p.y))
            }
        }

        // 7. Floating Combat Text
        engine.floatingTexts.forEach { ft ->
            val p = project(ft.x, ft.y, 58f)
            val alpha = (ft.life / ft.maxLife).coerceIn(0f, 1f)
            val fontSize = (if (ft.isCrit) 36f else 28f) * p.scale.coerceIn(0.8f, 1.3f)
            textPaint.textSize = fontSize
            // Ink outline shadow
            textPaint.color = InkOutline.copy(alpha = alpha).toArgb()
            drawContext.canvas.nativeCanvas.drawText(ft.text, p.x + 2f, p.y + 2f, textPaint)
            textPaint.color = ft.color.copy(alpha = alpha).toArgb()
            drawContext.canvas.nativeCanvas.drawText(ft.text, p.x, p.y, textPaint)
        }
    }
}

private fun DrawScope.drawCelPillar(base: ProjPoint, top: ProjPoint, color: Color) {
    val w = 28f * base.scale
    val wt = 28f * top.scale
    // Left lit face
    val leftFace = Path().apply {
        moveTo(base.x - w, base.y)
        lineTo(base.x, base.y + w * 0.4f)
        lineTo(top.x, top.y + wt * 0.4f)
        lineTo(top.x - wt, top.y)
        close()
    }
    drawPath(leftFace, color = color.copy(alpha = 0.75f))
    drawPath(leftFace, color = InkOutline, style = Stroke(width = 3f))

    // Right shadow face
    val rightFace = Path().apply {
        moveTo(base.x, base.y + w * 0.4f)
        lineTo(base.x + w, base.y)
        lineTo(top.x + wt, top.y)
        lineTo(top.x, top.y + wt * 0.4f)
        close()
    }
    drawPath(rightFace, color = color.copy(alpha = 0.35f))
    drawPath(rightFace, color = InkOutline, style = Stroke(width = 3f))

    // Top cap
    val topCap = Path().apply {
        moveTo(top.x, top.y - wt * 0.4f)
        lineTo(top.x + wt, top.y)
        lineTo(top.x, top.y + wt * 0.4f)
        lineTo(top.x - wt, top.y)
        close()
    }
    drawPath(topCap, color = Color.White.copy(alpha = 0.85f))
    drawPath(topCap, color = InkOutline, style = Stroke(width = 3f))
}

private fun DrawScope.drawCelSearchNode(
    node: SearchNodeEntity,
    base: ProjPoint,
    crystal: ProjPoint,
    textPaint: Paint
) {
    val color = if (node.indexed) GoogleGreen else GeminiCyan
    val rx = 86f * base.scale
    val ry = 60f * base.scale

    drawOval(
        color = color.copy(alpha = 0.18f),
        topLeft = Offset(base.x - rx, base.y - ry),
        size = Size(rx * 2f, ry * 2f)
    )
    drawOval(
        color = color.copy(alpha = 0.8f),
        topLeft = Offset(base.x - rx, base.y - ry),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(width = 3f)
    )

    if (!node.indexed && node.progress > 0f) {
        drawArc(
            color = GoogleGold,
            startAngle = -90f,
            sweepAngle = 360f * node.progress,
            useCenter = false,
            topLeft = Offset(base.x - rx, base.y - ry),
            size = Size(rx * 2f, ry * 2f),
            style = Stroke(width = 6f * base.scale, cap = StrokeCap.Round)
        )
    }

    // 3D Cel-Shaded Octahedron Crystal (Left lit facet + Right shaded facet)
    val s = 26f * crystal.scale
    val leftFacet = Path().apply {
        moveTo(crystal.x, crystal.y - s * 1.3f)
        lineTo(crystal.x, crystal.y + s * 1.1f)
        lineTo(crystal.x - s * 0.85f, crystal.y)
        close()
    }
    val rightFacet = Path().apply {
        moveTo(crystal.x, crystal.y - s * 1.3f)
        lineTo(crystal.x + s * 0.85f, crystal.y)
        lineTo(crystal.x, crystal.y + s * 1.1f)
        close()
    }
    drawPath(leftFacet, color = color)
    drawPath(rightFacet, color = color.copy(alpha = 0.55f))
    drawPath(leftFacet, color = InkOutline, style = Stroke(width = 3f))
    drawPath(rightFacet, color = InkOutline, style = Stroke(width = 3f))

    textPaint.textSize = 20f * base.scale
    textPaint.color = InkOutline.toArgb()
    drawContext.canvas.nativeCanvas.drawText(
        if (node.indexed) "✓ ${node.label}" else node.label,
        base.x + 1.5f,
        base.y + ry + 18f * base.scale + 1.5f,
        textPaint
    )
    textPaint.color = Color.White.toArgb()
    drawContext.canvas.nativeCanvas.drawText(
        if (node.indexed) "✓ ${node.label}" else node.label,
        base.x,
        base.y + ry + 18f * base.scale,
        textPaint
    )
}

private fun DrawScope.drawCelGoogleCoin(
    coin: CoinEntity,
    ground: ProjPoint,
    elev: ProjPoint,
    textPaint: Paint
) {
    val gr = 12f * ground.scale
    drawOval(
        color = InkOutline.copy(alpha = 0.45f),
        topLeft = Offset(ground.x - gr, ground.y - gr * 0.45f),
        size = Size(gr * 2f, gr * 0.9f)
    )
    val r = 14f * elev.scale
    val wScale = (0.35f + 0.65f * abs(cos(coin.spinPhase)))
    drawOval(
        color = InkOutline,
        topLeft = Offset(elev.x - r * wScale - 2f, elev.y - r - 2f),
        size = Size(r * 2f * wScale + 4f, r * 2f + 4f)
    )
    drawOval(
        color = GoogleGold,
        topLeft = Offset(elev.x - r * wScale, elev.y - r),
        size = Size(r * 2f * wScale, r * 2f)
    )
    if (wScale > 0.6f) {
        textPaint.color = InkOutline.toArgb()
        textPaint.textSize = 18f * elev.scale
        drawContext.canvas.nativeCanvas.drawText("G", elev.x, elev.y + 6f * elev.scale, textPaint)
    }
}

private fun DrawScope.drawCelGeminiHero(
    player: PlayerEntity,
    ground: ProjPoint,
    body: ProjPoint,
    time: Float
) {
    val s = body.scale
    val sg = ground.scale

    // Ground Contact Shadow
    drawOval(
        color = InkOutline.copy(alpha = 0.6f),
        topLeft = Offset(ground.x - 25f * sg, ground.y - 12f * sg),
        size = Size(50f * sg, 24f * sg)
    )

    val dirX = cos(player.facingAngle)
    val dirY = sin(player.facingAngle)

    // 1. Flowing Google-Red & Blue Cape behind hero (3D layered folds)
    val capeX = body.x - dirX * 15f * s
    val capeY = body.y - 20f * s - dirY * 6f * s
    val flutter = sin(time * 8f) * 4f * s
    val capePath = Path().apply {
        moveTo(body.x - 9f * s, body.y - 35f * s)
        lineTo(body.x + 9f * s, body.y - 35f * s)
        lineTo(capeX + 13f * s, capeY + 24f * s + flutter)
        lineTo(capeX - 13f * s, capeY + 24f * s - flutter)
        close()
    }
    drawPath(capePath, color = VirusCrimson)
    drawPath(capePath, color = InkOutline, style = Stroke(width = 3f))

    // 2. 3D Articulated Boots/Legs with Google Green & Slate armor
    val legSwing = sin(player.walkPhase) * 6f * s
    drawRoundRect(
        color = InkOutline,
        topLeft = Offset(body.x - 11.5f * s, body.y - 17f * s + legSwing),
        size = Size(10f * s, 17f * s),
        cornerRadius = CornerRadius(4f * s, 4f * s)
    )
    drawRoundRect(
        color = Color(0xFF1E3A8A),
        topLeft = Offset(body.x - 10f * s, body.y - 15.5f * s + legSwing),
        size = Size(7f * s, 14f * s),
        cornerRadius = CornerRadius(3f * s, 3f * s)
    )
    drawRoundRect(
        color = InkOutline,
        topLeft = Offset(body.x + 1.5f * s, body.y - 17f * s - legSwing),
        size = Size(10f * s, 17f * s),
        cornerRadius = CornerRadius(4f * s, 4f * s)
    )
    drawRoundRect(
        color = Color(0xFF1E3A8A),
        topLeft = Offset(body.x + 3f * s, body.y - 15.5f * s - legSwing),
        size = Size(7f * s, 14f * s),
        cornerRadius = CornerRadius(3f * s, 3f * s)
    )

    // 3. 3D Volumetric Armored Torso (Google Blue + Cyan highlight + Dark Blue shadow + Google 4-color crest)
    val torsoColor = if (player.hitFlashTimer > 0f) VirusCrimson else GeminiBlue
    drawRoundRect(
        color = InkOutline,
        topLeft = Offset(body.x - 17f * s, body.y - 43f * s),
        size = Size(34f * s, 29f * s),
        cornerRadius = CornerRadius(10f * s, 10f * s)
    )
    drawRoundRect(
        color = torsoColor,
        topLeft = Offset(body.x - 14.5f * s, body.y - 40.5f * s),
        size = Size(29f * s, 24f * s),
        cornerRadius = CornerRadius(8f * s, 8f * s)
    )
    // 3D Top-left specular rim
    drawRoundRect(
        color = GeminiCyan.copy(alpha = 0.65f),
        topLeft = Offset(body.x - 13f * s, body.y - 39.5f * s),
        size = Size(14f * s, 10f * s),
        cornerRadius = CornerRadius(6f * s, 6f * s)
    )
    // 3D Right-side cel shadow
    drawRoundRect(
        color = Color(0xFF1D4ED8).copy(alpha = 0.65f),
        topLeft = Offset(body.x + 1f * s, body.y - 39.5f * s),
        size = Size(13.5f * s, 22f * s),
        cornerRadius = CornerRadius(6f * s, 6f * s)
    )
    // 3D Shoulder Pauldrons (Google Gold & Green accents)
    drawCircle(color = InkOutline, radius = 7.5f * s, center = Offset(body.x - 15f * s, body.y - 37f * s))
    drawCircle(color = GoogleGold, radius = 5.5f * s, center = Offset(body.x - 15f * s, body.y - 37f * s))
    drawCircle(color = InkOutline, radius = 7.5f * s, center = Offset(body.x + 15f * s, body.y - 37f * s))
    drawCircle(color = GoogleGreen, radius = 5.5f * s, center = Offset(body.x + 15f * s, body.y - 37f * s))

    // Glowing 4-Point Gemini Star Emblem on Chest
    val chest = Offset(body.x, body.y - 29f * s)
    val starR = 6.5f * s
    val starIn = 2.5f * s
    val starPath = Path().apply {
        moveTo(chest.x, chest.y - starR)
        lineTo(chest.x + starIn, chest.y - starIn)
        lineTo(chest.x + starR, chest.y)
        lineTo(chest.x + starIn, chest.y + starIn)
        lineTo(chest.x, chest.y + starR)
        lineTo(chest.x - starIn, chest.y + starIn)
        lineTo(chest.x - starR, chest.y)
        lineTo(chest.x - starIn, chest.y - starIn)
        close()
    }
    drawPath(starPath, color = Color.White)

    // 4. 3D Spherical Helmet with Cel-Shaded Rim, Visor & Google-Blue Plume
    val headCenter = Offset(body.x, body.y - 54f * s)
    drawCircle(color = InkOutline, radius = 16.5f * s, center = headCenter)
    drawCircle(color = Color(0xFFF1F5F9), radius = 14f * s, center = headCenter)
    // Cel-shaded shadow crescent on helmet
    drawArc(
        color = Color(0xFFCBD5E1),
        startAngle = -20f,
        sweepAngle = 160f,
        useCenter = true,
        topLeft = Offset(headCenter.x - 14f * s, headCenter.y - 14f * s),
        size = Size(28f * s, 28f * s)
    )
    // Specular highlight dot on helmet
    drawCircle(
        color = Color.White,
        radius = 4f * s,
        center = Offset(headCenter.x - 5f * s, headCenter.y - 6f * s)
    )
    // 3D Visor facing movement/attack direction
    val visorX = headCenter.x + dirX * 5.5f * s
    val visorY = headCenter.y + dirY * 3.5f * s
    drawOval(
        color = InkOutline,
        topLeft = Offset(visorX - 8f * s, visorY - 5f * s),
        size = Size(16f * s, 10f * s)
    )
    drawOval(
        color = GeminiCyan,
        topLeft = Offset(visorX - 6f * s, visorY - 3.5f * s),
        size = Size(12f * s, 7f * s)
    )

    // 5. 3D Photon Sword & Cel-Shaded Slash Arc
    if (player.slashActiveTimer > 0f) {
        val t = 1f - (player.slashActiveTimer / 0.22f)
        val isSpin = player.slashComboStep == 2
        val arcRadius = (if (isSpin) 115f else 92f) * s
        val startDeg = Math.toDegrees(player.facingAngle.toDouble()).toFloat() - (if (isSpin) 180f else 75f)
        val sweepDeg = if (isSpin) 360f * t else 150f * t

        drawArc(
            color = InkOutline,
            startAngle = startDeg,
            sweepAngle = sweepDeg,
            useCenter = false,
            topLeft = Offset(body.x - arcRadius, body.y - 28f * s - arcRadius * 0.72f),
            size = Size(arcRadius * 2f, arcRadius * 1.44f),
            style = Stroke(width = 18f * s, cap = StrokeCap.Round)
        )
        drawArc(
            color = if (isSpin) GoogleGold else GeminiCyan,
            startAngle = startDeg,
            sweepAngle = sweepDeg,
            useCenter = false,
            topLeft = Offset(body.x - arcRadius, body.y - 28f * s - arcRadius * 0.72f),
            size = Size(arcRadius * 2f, arcRadius * 1.44f),
            style = Stroke(width = 11f * s, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color.White,
            startAngle = startDeg,
            sweepAngle = sweepDeg,
            useCenter = false,
            topLeft = Offset(body.x - arcRadius, body.y - 28f * s - arcRadius * 0.72f),
            size = Size(arcRadius * 2f, arcRadius * 1.44f),
            style = Stroke(width = 4f * s, cap = StrokeCap.Round)
        )
    } else {
        // Resting 3D Photon Sword with Gold Crossguard
        val handX = body.x + 18f * s
        val handY = body.y - 27f * s
        val tipX = handX + 18f * s
        val tipY = handY - 36f * s
        drawLine(color = InkOutline, start = Offset(handX, handY), end = Offset(tipX, tipY), strokeWidth = 10f * s, cap = StrokeCap.Round)
        drawLine(color = GeminiCyan, start = Offset(handX, handY), end = Offset(tipX, tipY), strokeWidth = 6f * s, cap = StrokeCap.Round)
        drawLine(color = Color.White, start = Offset(handX, handY), end = Offset(tipX, tipY), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
        // Crossguard
        drawLine(
            color = GoogleGold,
            start = Offset(handX - 5f * s, handY - 3f * s),
            end = Offset(handX + 7f * s, handY + 3f * s),
            strokeWidth = 5f * s,
            cap = StrokeCap.Round
        )
    }

    // 6. 3D Beveled Hexagonal Google Aegis Shield
    val shieldPos = if (player.isGuarding) {
        Offset(body.x + dirX * 22f * s, body.y - 28f * s + dirY * 14f * s)
    } else {
        Offset(body.x - 19f * s, body.y - 26f * s)
    }
    val sr = (if (player.isGuarding) 19f else 14f) * s
    val shieldPath = Path().apply {
        moveTo(shieldPos.x, shieldPos.y - sr)
        lineTo(shieldPos.x + sr * 0.85f, shieldPos.y - sr * 0.4f)
        lineTo(shieldPos.x + sr * 0.75f, shieldPos.y + sr * 0.55f)
        lineTo(shieldPos.x, shieldPos.y + sr)
        lineTo(shieldPos.x - sr * 0.75f, shieldPos.y + sr * 0.55f)
        lineTo(shieldPos.x - sr * 0.85f, shieldPos.y - sr * 0.4f)
        close()
    }
    drawPath(shieldPath, color = GoogleGold)
    drawPath(shieldPath, color = InkOutline, style = Stroke(width = 3.2f))
    // Inner 3D bevel + Google Blue core
    drawCircle(color = GeminiBlue, radius = sr * 0.42f, center = shieldPos)
    drawCircle(color = Color.White, radius = sr * 0.18f, center = shieldPos)

    if (player.isGuarding) {
        drawCircle(
            color = GoogleGold.copy(alpha = 0.24f),
            radius = 38f * s,
            center = Offset(body.x, body.y - 28f * s)
        )
        drawCircle(
            color = GoogleGold.copy(alpha = 0.85f),
            radius = 38f * s,
            center = Offset(body.x, body.y - 28f * s),
            style = Stroke(width = 3f)
        )
    }

    // 7. Ad-Blocking Defense Bubble
    if (player.adBlockBubbleTimer > 0f) {
        val bCenter = Offset(body.x, body.y - 30f * s)
        val br = (44f + sin(time * 6f) * 2.5f) * s
        drawCircle(color = GeminiCyan.copy(alpha = 0.20f), radius = br, center = bCenter)
        drawCircle(color = GeminiCyan.copy(alpha = 0.9f), radius = br, center = bCenter, style = Stroke(width = 3.5f))
    }

    // 8. Orbiting Anti-Virus Auto-Gun Turret
    if (player.antiVirusGunTimer > 0f) {
        val orbAngle = time * 3.5f
        val gunCenter = Offset(
            body.x + cos(orbAngle) * 36f * s,
            body.y - 36f * s + sin(orbAngle) * 16f * s
        )
        drawCircle(color = InkOutline, radius = 9.5f * s, center = gunCenter)
        drawCircle(color = GoogleGreen, radius = 7.5f * s, center = gunCenter)
        drawCircle(color = Color.White, radius = 3f * s, center = gunCenter)
    }

    // 9. 2X Attack Multiplier Golden Aura
    if (player.attackMultTimer > 0f) {
        drawOval(
            color = GoogleGold.copy(alpha = 0.75f),
            topLeft = Offset(ground.x - 30f * sg, ground.y - 15f * sg),
            size = Size(60f * sg, 30f * sg),
            style = Stroke(width = 3.5f)
        )
    }
}

private fun DrawScope.drawCelTetrisCube(
    base: ProjPoint,
    top: ProjPoint,
    cellSize: Float,
    color: Color
) {
    val hw = (cellSize * 0.48f) * base.scale
    val hwt = (cellSize * 0.48f) * top.scale
    val depthOff = hw * 0.35f

    // Front Face
    val front = Path().apply {
        moveTo(base.x - hw, base.y)
        lineTo(base.x + hw, base.y)
        lineTo(top.x + hwt, top.y)
        lineTo(top.x - hwt, top.y)
        close()
    }
    drawPath(front, color = color.copy(alpha = 0.85f))
    drawPath(front, color = InkOutline, style = Stroke(width = 2.8f))

    // Top Face
    val topCap = Path().apply {
        moveTo(top.x - hwt, top.y)
        lineTo(top.x + hwt, top.y)
        lineTo(top.x + hwt + depthOff, top.y - depthOff)
        lineTo(top.x - hwt + depthOff, top.y - depthOff)
        close()
    }
    drawPath(topCap, color = Color.White.copy(alpha = 0.65f))
    drawPath(topCap, color = color.copy(alpha = 0.65f))
    drawPath(topCap, color = InkOutline, style = Stroke(width = 2.8f))

    // Right Side Face
    val side = Path().apply {
        moveTo(base.x + hw, base.y)
        lineTo(base.x + hw + depthOff, base.y - depthOff)
        lineTo(top.x + hwt + depthOff, top.y - depthOff)
        lineTo(top.x + hwt, top.y)
        close()
    }
    drawPath(side, color = color.copy(alpha = 0.42f))
    drawPath(side, color = InkOutline, style = Stroke(width = 2.8f))
}

private fun DrawScope.drawCelItemDrop(
    item: ItemDropEntity,
    ground: ProjPoint,
    elev: ProjPoint,
    textPaint: Paint
) {
    val gr = 18f * ground.scale
    drawOval(
        color = item.type.color.copy(alpha = 0.25f),
        topLeft = Offset(ground.x - gr, ground.y - gr * 0.55f),
        size = Size(gr * 2f, gr * 1.1f)
    )
    drawOval(
        color = item.type.color,
        topLeft = Offset(ground.x - gr, ground.y - gr * 0.55f),
        size = Size(gr * 2f, gr * 1.1f),
        style = Stroke(width = 2.5f)
    )

    val r = 16f * elev.scale
    drawRoundRect(
        color = InkOutline,
        topLeft = Offset(elev.x - r - 2f, elev.y - r - 2f),
        size = Size(r * 2f + 4f, r * 2f + 4f),
        cornerRadius = CornerRadius(8f * elev.scale, 8f * elev.scale)
    )
    drawRoundRect(
        color = item.type.color,
        topLeft = Offset(elev.x - r, elev.y - r),
        size = Size(r * 2f, r * 2f),
        cornerRadius = CornerRadius(7f * elev.scale, 7f * elev.scale)
    )

    val badge = when (item.type) {
        ItemUpgradeType.ANTI_VIRUS_GUN -> "GUN"
        ItemUpgradeType.AD_BLOCK_BUBBLE -> "SHD"
        ItemUpgradeType.SPEED_UP -> "SPD"
        ItemUpgradeType.ATTACK_MULTIPLIER -> "2X"
        ItemUpgradeType.HEALTH_PACK -> "+HP"
    }
    textPaint.color = InkOutline.toArgb()
    textPaint.textSize = 14f * elev.scale
    drawContext.canvas.nativeCanvas.drawText(badge, elev.x, elev.y + 5f * elev.scale, textPaint)
}

private fun DrawScope.drawCelEnemyCharacter(
    enemy: EnemyEntity,
    ground: ProjPoint,
    body: ProjPoint,
    time: Float,
    textPaint: Paint
) {
    val s = body.scale
    val sg = ground.scale
    val r = enemy.type.radius * s
    val isFlash = enemy.hitFlashTimer > 0f
    val isStunned = enemy.stunTimer > 0f

    // Ground Shadow
    drawOval(
        color = InkOutline.copy(alpha = 0.56f),
        topLeft = Offset(ground.x - enemy.type.radius * sg, ground.y - enemy.type.radius * 0.45f * sg),
        size = Size(enemy.type.radius * 2f * sg, enemy.type.radius * 0.9f * sg)
    )

    val legSwing = sin(enemy.walkPhase) * 5f * s

    when (enemy.type) {
        EnemyType.TROJAN_VIRUS -> {
            // 3D Volumetric Cel-Shaded Crimson Imp
            val mainCol = if (isFlash) Color.White else VirusCrimson
            val darkCol = if (isFlash) Color.White else Color(0xFF991B1B)

            drawCircle(color = InkOutline, radius = 5.5f * s, center = Offset(body.x - 8f * s, body.y - 4f * s + legSwing))
            drawCircle(color = InkOutline, radius = 5.5f * s, center = Offset(body.x + 8f * s, body.y - 4f * s - legSwing))

            drawCircle(color = InkOutline, radius = r * 0.95f, center = Offset(body.x, body.y - r * 0.9f))
            drawCircle(color = mainCol, radius = r * 0.82f, center = Offset(body.x, body.y - r * 0.9f))
            drawArc(
                color = darkCol,
                startAngle = -20f,
                sweepAngle = 155f,
                useCenter = true,
                topLeft = Offset(body.x - r * 0.82f, body.y - r * 1.72f),
                size = Size(r * 1.64f, r * 1.64f)
            )
            // 3D Specular Highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = r * 0.22f,
                center = Offset(body.x - r * 0.35f, body.y - r * 1.25f)
            )

            // 3D Horns
            val hornL = Path().apply {
                moveTo(body.x - r * 0.5f, body.y - r * 1.4f)
                lineTo(body.x - r * 0.88f, body.y - r * 2.05f)
                lineTo(body.x - r * 0.15f, body.y - r * 1.55f)
                close()
            }
            val hornR = Path().apply {
                moveTo(body.x + r * 0.5f, body.y - r * 1.4f)
                lineTo(body.x + r * 0.88f, body.y - r * 2.05f)
                lineTo(body.x + r * 0.15f, body.y - r * 1.55f)
                close()
            }
            drawPath(hornL, color = GoogleGold)
            drawPath(hornL, color = InkOutline, style = Stroke(width = 2.5f))
            drawPath(hornR, color = GoogleGold)
            drawPath(hornR, color = InkOutline, style = Stroke(width = 2.5f))

            drawCircle(color = GoogleGold, radius = 3.8f * s, center = Offset(body.x - 6f * s, body.y - r * 0.95f))
            drawCircle(color = GoogleGold, radius = 3.8f * s, center = Offset(body.x + 6f * s, body.y - r * 0.95f))
        }

        EnemyType.POPUP_ADWARE -> {
            // 3D Extruded Cube Hover-Bot (Top face + Front face + Side shadow face)
            val mainCol = if (isFlash) Color.White else AdwareMagenta
            val sideCol = if (isFlash) Color.White else Color(0xFFA21CAF)
            val topCol = if (isFlash) Color.White else Color(0xFFF0ABFC)
            val hoverY = body.y - r * 1.05f + sin(time * 5f + enemy.id) * 4f * s

            // Antenna
            drawLine(color = InkOutline, start = Offset(body.x, hoverY - r * 0.95f), end = Offset(body.x, hoverY - r * 1.45f), strokeWidth = 3f * s)
            drawCircle(color = GoogleGold, radius = 4.5f * s, center = Offset(body.x, hoverY - r * 1.5f))

            // 3D Top Face
            val topFace = Path().apply {
                moveTo(body.x - r * 0.85f, hoverY - r * 0.72f)
                lineTo(body.x - r * 0.45f, hoverY - r * 1.05f)
                lineTo(body.x + r * 1.05f, hoverY - r * 1.05f)
                lineTo(body.x + r * 0.75f, hoverY - r * 0.72f)
                close()
            }
            drawPath(topFace, color = topCol)
            drawPath(topFace, color = InkOutline, style = Stroke(width = 2.5f))

            // 3D Right Side Face
            val sideFace = Path().apply {
                moveTo(body.x + r * 0.75f, hoverY - r * 0.72f)
                lineTo(body.x + r * 1.05f, hoverY - r * 1.05f)
                lineTo(body.x + r * 1.05f, hoverY + r * 0.45f)
                lineTo(body.x + r * 0.75f, hoverY + r * 0.72f)
                close()
            }
            drawPath(sideFace, color = sideCol)
            drawPath(sideFace, color = InkOutline, style = Stroke(width = 2.5f))

            // 3D Front Face
            drawRect(
                color = mainCol,
                topLeft = Offset(body.x - r * 0.85f, hoverY - r * 0.72f),
                size = Size(r * 1.6f, r * 1.44f)
            )
            drawRect(
                color = InkOutline,
                topLeft = Offset(body.x - r * 0.85f, hoverY - r * 0.72f),
                size = Size(r * 1.6f, r * 1.44f),
                style = Stroke(width = 2.5f)
            )
            drawRoundRect(
                color = DeepWebVoid,
                topLeft = Offset(body.x - r * 0.65f, hoverY - r * 0.45f),
                size = Size(r * 1.2f, r * 0.85f),
                cornerRadius = CornerRadius(4f * s, 4f * s)
            )
            textPaint.color = Color.White.toArgb()
            textPaint.textSize = 16f * s
            drawContext.canvas.nativeCanvas.drawText("AD!", body.x - r * 0.05f, hoverY + 5f * s, textPaint)
        }

        EnemyType.PESKY_HACKER -> {
            // 3D Layered Hooded Cyber Rogue
            val cloakCol = if (isFlash) Color.White else Color(0xFF065F46)
            val cloakShadow = if (isFlash) Color.White else Color(0xFF022C22)
            val neonCol = if (isFlash) Color.White else HackerNeonGreen

            val cloak = Path().apply {
                moveTo(body.x, body.y - r * 2.0f)
                lineTo(body.x + r * 0.98f, body.y - 2f * s)
                lineTo(body.x - r * 0.98f, body.y - 2f * s)
                close()
            }
            val cloakRightFold = Path().apply {
                moveTo(body.x, body.y - r * 2.0f)
                lineTo(body.x + r * 0.98f, body.y - 2f * s)
                lineTo(body.x, body.y - 2f * s)
                close()
            }
            drawPath(cloak, color = cloakCol)
            drawPath(cloakRightFold, color = cloakShadow)
            drawPath(cloak, color = InkOutline, style = Stroke(width = 3.5f))

            drawCircle(color = InkOutline, radius = r * 0.46f, center = Offset(body.x, body.y - r * 1.15f))
            drawCircle(color = neonCol, radius = 3.8f * s, center = Offset(body.x - 5f * s, body.y - r * 1.15f))
            drawCircle(color = neonCol, radius = 3.8f * s, center = Offset(body.x + 5f * s, body.y - r * 1.15f))

            drawLine(
                color = neonCol,
                start = Offset(body.x + r * 0.7f, body.y - r * 0.7f),
                end = Offset(body.x + r * 1.25f, body.y - r * 1.25f),
                strokeWidth = 4.5f * s,
                cap = StrokeCap.Round
            )
        }

        EnemyType.SECTOR_BOSS -> {
            // Towering 3D Cel-Shaded Cyber-Golem Boss
            val mainCol = if (isFlash) Color.White else VirusCrimson
            drawRoundRect(
                color = InkOutline,
                topLeft = Offset(body.x - r * 1.05f, body.y - r * 2.0f),
                size = Size(r * 2.1f, r * 1.7f),
                cornerRadius = CornerRadius(14f * s, 14f * s)
            )
            drawRoundRect(
                color = mainCol,
                topLeft = Offset(body.x - r * 0.95f, body.y - r * 1.9f),
                size = Size(r * 1.9f, r * 1.5f),
                cornerRadius = CornerRadius(12f * s, 12f * s)
            )
            // 3D Right-side cel shadow on Boss torso
            drawRoundRect(
                color = Color(0xFF991B1B).copy(alpha = 0.65f),
                topLeft = Offset(body.x, body.y - r * 1.9f),
                size = Size(r * 0.95f, r * 1.5f),
                cornerRadius = CornerRadius(10f * s, 10f * s)
            )
            drawCircle(color = InkOutline, radius = r * 0.55f, center = Offset(body.x - r * 0.95f, body.y - r * 1.65f))
            drawCircle(color = GoogleGold, radius = r * 0.44f, center = Offset(body.x - r * 0.95f, body.y - r * 1.65f))
            drawCircle(color = InkOutline, radius = r * 0.55f, center = Offset(body.x + r * 0.95f, body.y - r * 1.65f))
            drawCircle(color = GoogleGold, radius = r * 0.44f, center = Offset(body.x + r * 0.95f, body.y - r * 1.65f))
            drawCircle(color = InkOutline, radius = r * 0.38f, center = Offset(body.x, body.y - r * 1.15f))
            drawCircle(color = GoogleGold, radius = r * 0.28f, center = Offset(body.x, body.y - r * 1.15f))
        }
    }

    if (isStunned) {
        for (k in 0 until 3) {
            val a = time * 6f + k * 2.09f
            drawCircle(
                color = GoogleGold,
                radius = 4f * s,
                center = Offset(body.x + cos(a) * 18f * s, body.y - r * 2.1f + sin(a) * 6f * s)
            )
        }
    }

    if (enemy.hp < enemy.maxHp && enemy.type != EnemyType.SECTOR_BOSS) {
        val barW = r * 1.8f
        val ratio = (enemy.hp / enemy.maxHp).coerceIn(0f, 1f)
        drawRect(
            color = InkOutline,
            topLeft = Offset(body.x - barW / 2f - 1.5f, body.y - r * 2.25f - 1.5f),
            size = Size(barW + 3f, 8f * s)
        )
        drawRect(
            color = VirusCrimson,
            topLeft = Offset(body.x - barW / 2f, body.y - r * 2.25f),
            size = Size(barW * ratio, 5f * s)
        )
    }
}
