package com.koke1024.craftdice.domain.battle.ai

import com.koke1024.craftdice.domain.physics.Vector2

/**
 * Position and outcome info for a settled player dice, used by the enemy AI
 * to plan bump attacks against high-value targets.
 */
data class PlayerDiceInfo(
    val unitId: Int,
    val position: Vector2,
    val faceValue: Int,
)

/**
 * A throw plan produced by the enemy AI for a single enemy dice.
 */
data class ThrowPlan(
    val unitId: Int,
    val origin: Vector2,
    val velocity: Vector2,
)
