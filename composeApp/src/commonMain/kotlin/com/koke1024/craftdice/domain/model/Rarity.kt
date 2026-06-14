package com.koke1024.craftdice.domain.model

/**
 * Face rarity tier.
 *
 * Higher rarity faces yield stronger effects or are harder to obtain.
 * The roguelike reward system (Phase 4+) will use this to gate drops.
 */
enum class Rarity(
    val displayName: String,
    val tier: Int,
) {
    COMMON("コモン", 0),
    RARE("レア", 1),
    EPIC("エピック", 2),
    LEGENDARY("レジェンダリー", 3),
}
