package com.koke1024.craftdice.domain.roguelike.meta

import com.koke1024.craftdice.domain.roguelike.model.RunState
import com.koke1024.craftdice.domain.roguelike.model.RunStatus

/**
 * Computes the meta currency ("dice shards") a run brings home.
 *
 * The design rewards both progress and daring: every cleared room and slain
 * enemy pays into a progress pool, the inventory's already-earned meta
 * currency is always kept in full, and clearing the dungeon (victory) adds a
 * flat bonus while dying halves the progress portion (the "risk" of a
 * roguelike death — but you never leave empty-handed).
 */
object MetaCurrencyCalculator {

    const val PER_FLOOR_CLEARED = 2
    const val PER_ENEMY_DEFEATED = 1
    const val VICTORY_BONUS = 20

    fun progressBonus(state: RunState): Int =
        state.roomsCleared * PER_FLOOR_CLEARED + state.enemiesDefeated * PER_ENEMY_DEFEATED

    fun forVictory(state: RunState): Int =
        state.totalMetaCurrency() + progressBonus(state) + VICTORY_BONUS

    fun forDefeat(state: RunState): Int =
        state.totalMetaCurrency() + progressBonus(state) / 2

    fun forOutcome(state: RunState, status: RunStatus): Int = when (status) {
        RunStatus.VICTORY -> forVictory(state)
        RunStatus.DEFEAT -> forDefeat(state)
        RunStatus.ONGOING -> 0
    }
}
