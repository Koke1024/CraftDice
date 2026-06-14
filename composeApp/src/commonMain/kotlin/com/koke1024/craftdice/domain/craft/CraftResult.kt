package com.koke1024.craftdice.domain.craft

import com.koke1024.craftdice.domain.model.Dice

/**
 * Result of a craft operation.
 *
 * Craft operations are immutable: on success a new [Dice] is returned
 * rather than mutating the source.
 */
sealed interface CraftResult {

    data class Success(
        val dice: Dice,
    ) : CraftResult

    data class Failure(
        val error: CraftError,
    ) : CraftResult
}
