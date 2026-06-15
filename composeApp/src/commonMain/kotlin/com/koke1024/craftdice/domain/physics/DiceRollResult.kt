package com.koke1024.craftdice.domain.physics

import com.koke1024.craftdice.domain.model.DiceFace

/**
 * Result of a completed dice roll simulation.
 *
 * Produced when all dice in the tray have come to rest.
 */
data class DiceRollResult(
    val results: List<DiceRollEntry>,
) {
    val isComplete: Boolean get() = results.isNotEmpty()

    fun faceById(id: Int): DiceFace? = results.find { it.diceId == id }?.face
}

data class DiceRollEntry(
    val diceId: Int,
    val face: DiceFace,
    val faceIndex: Int,
    val finalPosition: Vector2,
)
