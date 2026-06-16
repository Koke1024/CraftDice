package com.koke1024.craftdice.domain.meta

/**
 * Persistent meta-progression state that survives across runs.
 *
 * This is the single source of truth for what the player has unlocked and how
 * many "dice shards" (the meta currency) they can spend at the workshop. It is
 * mutated only through [MetaProgressionService] (pure transitions) and persisted
 * by the data layer via [com.koke1024.craftdice.data.repository.MetaProgressRepository].
 *
 * - [diceShards]: spendable balance (decreases on purchase).
 * - [totalEarned]: lifetime earnings, never decreases (display only).
 * - [purchasedUpgradeIds]: every [PersistentUpgrade] bought. Unlocked classes
 *   and other effects are derived from this set, so we never store redundant
 *   "is unlocked" flags.
 * - [selectedClassId]: which [StarterClass] the next run begins with.
 */
data class MetaProgression(
    val diceShards: Int,
    val totalEarned: Int,
    val purchasedUpgradeIds: Set<String>,
    val selectedClassId: String,
) {
    init {
        require(diceShards >= 0) { "diceShards must be non-negative, got $diceShards" }
        require(totalEarned >= 0) { "totalEarned must be non-negative, got $totalEarned" }
        require(totalEarned >= diceShards) {
            "totalEarned ($totalEarned) cannot be less than diceShards ($diceShards)"
        }
    }

    companion object {
        val DEFAULT: MetaProgression = MetaProgression(
            diceShards = 0,
            totalEarned = 0,
            purchasedUpgradeIds = emptySet(),
            selectedClassId = StarterClassCatalog.DEFAULT_ID,
        )
    }
}
