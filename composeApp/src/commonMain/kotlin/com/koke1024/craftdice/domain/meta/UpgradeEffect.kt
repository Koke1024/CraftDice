package com.koke1024.craftdice.domain.meta

/**
 * The concrete effect a [PersistentUpgrade] applies once purchased.
 *
 * Effects are read at run-start time by the dungeon layer; purchasing only
 * records the upgrade id and applies the [UnlockClass] side effect immediately
 * (by making the class selectable). Other effects are passive bonuses derived
 * from the purchased set.
 */
sealed interface UpgradeEffect {

    /** Adds a flat amount of crafting material to the starting inventory. */
    data class BonusStartingMaterial(val amount: Int) : UpgradeEffect

    /** Adds a flat amount of starting HP to every player unit. */
    data class BonusStartingHp(val amount: Int) : UpgradeEffect

    /** Makes the referenced [StarterClass] selectable and playable. */
    data class UnlockClass(val classId: String) : UpgradeEffect
}
