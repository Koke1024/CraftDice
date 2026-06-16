package com.koke1024.craftdice.domain.meta

/**
 * A permanent, one-time purchase bought with dice shards at the workshop.
 *
 * Each upgrade is bought exactly once (it then lives in
 * [MetaProgression.purchasedUpgradeIds]) and carries a single [effect]. The
 * upgrade itself is stateless; whether it is owned and active is determined by
 * the player's [MetaProgression].
 */
data class PersistentUpgrade(
    val id: String,
    val displayName: String,
    val description: String,
    val cost: Int,
    val effect: UpgradeEffect,
) {
    init {
        require(cost >= 0) { "Upgrade cost must be non-negative, got $cost" }
    }
}
