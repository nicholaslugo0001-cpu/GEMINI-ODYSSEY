package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepWebVoid
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GoogleGold
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.VirusCrimson

/**
 * Colorful cel-shaded Gemini Knight emblem matching the in-game 3D player character:
 * Google Blue/Cyan armor, Red cape, Gold & Green pauldrons, Photon Sword, and Aegis Shield,
 * framed by a 4-color Google halo ring.
 */
@Composable
fun GeminiCharacterEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 60.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = (w.coerceAtMost(h) / 2f) * 0.92f
        val ink = Color(0xFF070A16)

        // Vibrant dark-indigo backdrop
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1D357A), DeepWebVoid),
                center = Offset(cx, cy),
                radius = r
            ),
            radius = r,
            center = Offset(cx, cy)
        )

        // 4-Color Google Ring Halo
        val ringStroke = Stroke(width = w * 0.065f, cap = StrokeCap.Butt)
        val ringTopLeft = Offset(cx - r, cy - r)
        val ringSize = Size(r * 2f, r * 2f)
        drawArc(color = GeminiBlue, startAngle = -135f, sweepAngle = 90f, useCenter = false, topLeft = ringTopLeft, size = ringSize, style = ringStroke)
        drawArc(color = VirusCrimson, startAngle = -45f, sweepAngle = 90f, useCenter = false, topLeft = ringTopLeft, size = ringSize, style = ringStroke)
        drawArc(color = GoogleGold, startAngle = 45f, sweepAngle = 90f, useCenter = false, topLeft = ringTopLeft, size = ringSize, style = ringStroke)
        drawArc(color = GoogleGreen, startAngle = 135f, sweepAngle = 90f, useCenter = false, topLeft = ringTopLeft, size = ringSize, style = ringStroke)

        // Red Cape behind shoulders
        val cape = Path().apply {
            moveTo(cx - r * 0.45f, cy + r * 0.1f)
            lineTo(cx + r * 0.45f, cy + r * 0.1f)
            lineTo(cx + r * 0.58f, cy + r * 0.72f)
            lineTo(cx - r * 0.58f, cy + r * 0.72f)
            close()
        }
        drawPath(cape, color = VirusCrimson)
        drawPath(cape, color = ink, style = Stroke(width = 2.5f))

        // Photon Sword behind right shoulder
        val hilt = Offset(cx + r * 0.36f, cy + r * 0.22f)
        val tip = Offset(cx + r * 0.64f, cy - r * 0.52f)
        drawLine(color = ink, start = hilt, end = tip, strokeWidth = w * 0.095f, cap = StrokeCap.Round)
        drawLine(color = GeminiCyan, start = hilt, end = tip, strokeWidth = w * 0.055f, cap = StrokeCap.Round)
        drawLine(color = Color.White, start = hilt, end = tip, strokeWidth = w * 0.02f, cap = StrokeCap.Round)

        // Google Blue & Cyan Armored Torso
        drawRoundRect(
            color = ink,
            topLeft = Offset(cx - r * 0.42f, cy + r * 0.08f),
            size = Size(r * 0.84f, r * 0.60f),
            cornerRadius = CornerRadius(r * 0.22f, r * 0.22f)
        )
        drawRoundRect(
            color = GeminiBlue,
            topLeft = Offset(cx - r * 0.36f, cy + r * 0.13f),
            size = Size(r * 0.72f, r * 0.50f),
            cornerRadius = CornerRadius(r * 0.18f, r * 0.18f)
        )
        // Cyan chest highlight
        drawRoundRect(
            color = GeminiCyan.copy(alpha = 0.65f),
            topLeft = Offset(cx - r * 0.32f, cy + r * 0.16f),
            size = Size(r * 0.34f, r * 0.22f),
            cornerRadius = CornerRadius(r * 0.12f, r * 0.12f)
        )

        // Gold & Green Shoulder Pauldrons
        drawCircle(color = ink, radius = r * 0.20f, center = Offset(cx - r * 0.40f, cy + r * 0.20f))
        drawCircle(color = GoogleGold, radius = r * 0.15f, center = Offset(cx - r * 0.40f, cy + r * 0.20f))
        drawCircle(color = ink, radius = r * 0.20f, center = Offset(cx + r * 0.40f, cy + r * 0.20f))
        drawCircle(color = GoogleGreen, radius = r * 0.15f, center = Offset(cx + r * 0.40f, cy + r * 0.20f))

        // Glowing 4-Point Gemini Star on Chest
        val starC = Offset(cx, cy + r * 0.36f)
        val sOut = r * 0.18f
        val sIn = r * 0.065f
        val star = Path().apply {
            moveTo(starC.x, starC.y - sOut)
            lineTo(starC.x + sIn, starC.y - sIn)
            lineTo(starC.x + sOut, starC.y)
            lineTo(starC.x + sIn, starC.y + sIn)
            lineTo(starC.x, starC.y + sOut)
            lineTo(starC.x - sIn, starC.y + sIn)
            lineTo(starC.x - sOut, starC.y)
            lineTo(starC.x - sIn, starC.y - sIn)
            close()
        }
        drawPath(star, color = Color.White)

        // 3D Helmet + Cyan Visor
        val headC = Offset(cx, cy - r * 0.22f)
        drawCircle(color = ink, radius = r * 0.38f, center = headC)
        drawCircle(color = Color(0xFFF8FAFC), radius = r * 0.32f, center = headC)
        drawArc(
            color = Color(0xFFCBD5E1),
            startAngle = -20f,
            sweepAngle = 155f,
            useCenter = true,
            topLeft = Offset(headC.x - r * 0.32f, headC.y - r * 0.32f),
            size = Size(r * 0.64f, r * 0.64f)
        )
        drawOval(
            color = ink,
            topLeft = Offset(headC.x - r * 0.20f, headC.y - r * 0.08f),
            size = Size(r * 0.40f, r * 0.22f)
        )
        drawOval(
            color = GeminiCyan,
            topLeft = Offset(headC.x - r * 0.16f, headC.y - r * 0.05f),
            size = Size(r * 0.32f, r * 0.15f)
        )

        // Hexagonal Gold/Blue Aegis Shield on left
        val shC = Offset(cx - r * 0.48f, cy + r * 0.42f)
        val sr = r * 0.28f
        val shield = Path().apply {
            moveTo(shC.x, shC.y - sr)
            lineTo(shC.x + sr * 0.85f, shC.y - sr * 0.4f)
            lineTo(shC.x + sr * 0.75f, shC.y + sr * 0.55f)
            lineTo(shC.x, shC.y + sr)
            lineTo(shC.x - sr * 0.75f, shC.y + sr * 0.55f)
            lineTo(shC.x - sr * 0.85f, shC.y - sr * 0.4f)
            close()
        }
        drawPath(shield, color = GoogleGold)
        drawPath(shield, color = ink, style = Stroke(width = 2.5f))
        drawCircle(color = GeminiBlue, radius = sr * 0.42f, center = shC)
        drawCircle(color = Color.White, radius = sr * 0.18f, center = shC)
    }
}
