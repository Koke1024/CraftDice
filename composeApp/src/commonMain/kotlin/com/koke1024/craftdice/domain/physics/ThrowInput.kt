package com.koke1024.craftdice.domain.physics

/**
 * Input for throwing a dice, derived from a user swipe gesture.
 *
 * @property diceId the target dice body to apply velocity to.
 * @property velocity the initial velocity vector in px/s.
 */
data class ThrowInput(
    val diceId: Int,
    val velocity: Vector2,
)
