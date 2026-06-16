package com.koke1024.craftdice.domain.roguelike.reward

import com.koke1024.craftdice.domain.roguelike.model.Reward

/**
 * A weighted candidate in a [DropTable].
 *
 * [weight] is relative to the other entries in the same table; an entry with
 * weight 0 can never be picked.
 */
data class DropEntry(
    val reward: Reward,
    val weight: Int,
) {
    init {
        require(weight >= 0) { "weight must be non-negative, got $weight" }
    }
}

/**
 * A loot table rolled after combat.
 *
 * The table is rolled [rolls] times; each roll independently picks one entry
 * by weight, so the result is a list of [rolls] rewards (or fewer if the
 * table is empty). [guaranteed] rewards are always appended verbatim — used
 * for boss / milestone drops that must always happen.
 */
data class DropTable(
    val entries: List<DropEntry>,
    val rolls: Int = 1,
    val guaranteed: List<Reward> = emptyList(),
) {
    init {
        require(rolls >= 0) { "rolls must be non-negative, got $rolls" }
    }

    val isEmpty: Boolean get() = entries.isEmpty() && guaranteed.isEmpty()

    companion object {
        val EMPTY: DropTable = DropTable(entries = emptyList(), rolls = 0)
    }
}
