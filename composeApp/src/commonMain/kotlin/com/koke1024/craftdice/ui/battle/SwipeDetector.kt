package com.koke1024.craftdice.ui.battle

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

data class SwipeResult(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
) {
    val distance: Float
        get() = kotlin.math.hypot(endX - startX, endY - startY)

    fun toVelocity(scale: Float = SWIPE_VELOCITY_SCALE): Pair<Float, Float> =
        (endX - startX) * scale to (endY - startY) * scale
}

internal const val SWIPE_VELOCITY_SCALE = 5.0f

internal fun swipeDetectorModifier(
    onSwipeUpdate: (Float, Float, Float, Float) -> Unit,
    onSwipeEnd: (SwipeResult) -> Unit,
    onSwipeCancel: () -> Unit,
): Modifier =
    Modifier.pointerInput(Unit) {
        var startPos = Offset.Zero
        var currentPos = Offset.Zero

        detectDragGestures(
            onDragStart = { offset ->
                startPos = offset
                currentPos = offset
            },
            onDrag = { change, _ ->
                change.consume()
                currentPos = change.position
                onSwipeUpdate(startPos.x, startPos.y, currentPos.x, currentPos.y)
            },
            onDragEnd = {
                onSwipeEnd(
                    SwipeResult(
                        startX = startPos.x,
                        startY = startPos.y,
                        endX = currentPos.x,
                        endY = currentPos.y,
                    ),
                )
                startPos = Offset.Zero
                currentPos = Offset.Zero
            },
            onDragCancel = {
                onSwipeCancel()
                startPos = Offset.Zero
                currentPos = Offset.Zero
            },
        )
    }
