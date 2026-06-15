package com.koke1024.craftdice.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koke1024.craftdice.domain.physics.PhysicsConstraints

@Composable
fun DiceCanvas(
    state: BattleUiState,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier.fillMaxSize(),
    ) {
        drawTrayBackground()
        drawTrayBorder()
        drawDice(state, textMeasurer)
        drawSwipePreview(state)
    }
}

private fun DrawScope.drawTrayBackground() {
    drawRect(
        color = Color(0xFF1A1A2E),
        size = size,
    )
}

private fun DrawScope.drawTrayBorder() {
    val borderWidth = 3.dp.toPx()
    drawRect(
        color = Color(0xFFE94560),
        topLeft = Offset.Zero,
        size = size,
        style = Stroke(width = borderWidth),
    )
}

private fun DrawScope.drawDice(
    state: BattleUiState,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val scaleX = size.width / PhysicsConstraints.TRAY_WIDTH.toFloat()
    val scaleY = size.height / PhysicsConstraints.TRAY_HEIGHT.toFloat()

    for (dice in state.diceSnapshots) {
        val center = Offset(dice.x * scaleX, dice.y * scaleY)
        val radius = dice.radius * scaleX

        drawCircle(
            color = Color(dice.faceColor),
            radius = radius,
            center = center,
        )

        drawCircle(
            color = if (dice.isStopped) Color.White else Color.White.copy(alpha = 0.5f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )

        val textLayout =
            textMeasurer.measure(
                text = dice.faceLabel,
                style = TextStyle(color = Color.White, fontSize = 11.sp),
            )
        drawText(
            textLayout,
            topLeft =
                Offset(
                    center.x - textLayout.size.width / 2,
                    center.y - textLayout.size.height / 2,
                ),
        )
    }
}

private fun DrawScope.drawSwipePreview(state: BattleUiState) {
    val preview = state.swipePreview ?: return
    val scaleX = size.width / PhysicsConstraints.TRAY_WIDTH.toFloat()
    val scaleY = size.height / PhysicsConstraints.TRAY_HEIGHT.toFloat()

    val start = Offset(preview.startX, preview.startY)
    val end = Offset(preview.endX, preview.endY)

    drawLine(
        color = Color.White.copy(alpha = 0.6f * preview.power),
        start = start,
        end = end,
        strokeWidth = 4.dp.toPx(),
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.3f * preview.power),
        radius = 8.dp.toPx() * preview.power.coerceAtLeast(0.1f),
        center = start,
    )
}
