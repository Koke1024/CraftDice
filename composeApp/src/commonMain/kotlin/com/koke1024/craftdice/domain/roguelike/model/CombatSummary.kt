package com.koke1024.craftdice.domain.roguelike.model

import com.koke1024.craftdice.domain.battle.model.BattleStatus
import com.koke1024.craftdice.domain.battle.model.BattleUnit

/**
 * Hand-off value from a resolved combat back into the roguelike layer.
 *
 * The run engine stays decoupled from the physics/tray simulation by reading
 * only this summary: who won, which player units survived (with their
 * post-battle HP and broken faces synced back into the run state), how many
 * rounds were fought, and which template was defeated (drives the drop table).
 */
data class CombatSummary(
    val status: BattleStatus,
    val survivingPlayerUnits: List<BattleUnit>,
    val roundsFought: Int,
    val defeatedTemplate: EnemyTemplate?,
) {
    val playerWon: Boolean get() = status == BattleStatus.PLAYER1_WON
    val playerLost: Boolean get() = status == BattleStatus.PLAYER2_WON || status == BattleStatus.DRAW
}
