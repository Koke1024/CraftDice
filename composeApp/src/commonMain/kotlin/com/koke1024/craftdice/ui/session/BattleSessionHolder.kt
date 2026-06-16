package com.koke1024.craftdice.ui.session

import com.koke1024.craftdice.domain.roguelike.model.BattleSetup
import com.koke1024.craftdice.domain.roguelike.model.CombatSummary
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * App-scoped bridge that carries a [BattleSetup] from the dungeon layer to the
 * battle layer, and a [CombatSummary] back again.
 *
 * Compose Navigation cannot easily round-trip rich domain objects
 * ([BattleSetup] / [CombatSummary] carry dice graphs and templates) through
 * route arguments, and serialising them would couple the navigation layer to
 * the domain. Instead both screens resolve this single holder from Koin: the
 * dungeon calls [launch] before navigating, the battle calls [consumeSetup] on
 * entry and [publishResult] when it ends, and the dungeon calls
 * [consumeResult] when it resumes.
 *
 * One battle is in flight at a time, so a single slot for each direction is
 * enough; consume* clears the slot so a stale value never leaks into the next
 * run.
 */
class BattleSessionHolder {

    private val setupSlot = MutableStateFlow<BattleSetup?>(null)
    private val resultSlot = MutableStateFlow<CombatSummary?>(null)

    /** Stages a battle for the dungeon → battle handoff. */
    fun launch(setup: BattleSetup) {
        setupSlot.value = setup
    }

    /** Returns the staged setup and clears it, or null if none was staged. */
    fun consumeSetup(): BattleSetup? {
        val staged = setupSlot.value
        setupSlot.value = null
        return staged
    }

    /** Publishes the resolved combat result for the battle → dungeon handoff. */
    fun publishResult(summary: CombatSummary) {
        resultSlot.value = summary
    }

    /** Returns the published result and clears it, or null if none was published. */
    fun consumeResult(): CombatSummary? {
        val published = resultSlot.value
        resultSlot.value = null
        return published
    }
}
