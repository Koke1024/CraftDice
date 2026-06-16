package com.koke1024.craftdice.domain.meta

/**
 * Outcome of an attempt to purchase a [PersistentUpgrade].
 *
 * All transitions are pure: [Success] carries the next [MetaProgression] for
 * the caller to persist; failures leave the input progress untouched.
 */
sealed interface PurchaseResult {

    data class Success(
        val progress: MetaProgression,
        val upgrade: PersistentUpgrade,
    ) : PurchaseResult

    data object AlreadyOwned : PurchaseResult

    data object InsufficientFunds : PurchaseResult

    data object NotFound : PurchaseResult
}
