package com.koke1024.craftdice.domain.roguelike.model

import com.koke1024.craftdice.domain.battle.model.BattleRule
import com.koke1024.craftdice.domain.battle.model.BattleUnit

/**
 * Snapshot the run engine hands to the battle layer when a combat room is
 * entered.
 *
 * Carries the player's current roster (HP and broken faces persistent across
 * battles) and the materialised enemy unit(s) with non-colliding ids. The
 * caller feeds these into [com.koke1024.craftdice.domain.battle.BattleEngine.setup].
 *
 * [enemyTemplate] rides along so that, on a player victory, the battle layer
 * can hand the defeated template back to the run engine inside the
 * [CombatSummary] (it drives the drop table). The run engine owns the mapping
 * from a [FloorNode] enemy to its template, so this stays a single value that
 * matches the current single-enemy encounter model.
 */
data class BattleSetup(
    val playerUnits: List<BattleUnit>,
    val enemyUnits: List<BattleUnit>,
    val enemyTemplate: EnemyTemplate,
    val rule: BattleRule = BattleRule.BUMP,
) {
    init {
        require(playerUnits.isNotEmpty()) { "A battle needs at least one player unit" }
        require(enemyUnits.isNotEmpty()) { "A battle needs at least one enemy unit" }
    }
}
