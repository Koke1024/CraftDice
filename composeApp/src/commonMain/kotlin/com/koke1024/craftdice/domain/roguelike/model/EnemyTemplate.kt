package com.koke1024.craftdice.domain.roguelike.model

import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleUnit
import com.koke1024.craftdice.domain.model.Dice

/**
 * Blueprint for an enemy encounter, decoupled from the per-battle [BattleUnit]
 * identity that the battle engine assigns.
 *
 * Holds the combat-relevant data (dice loadout + HP) and a [rewardTier] that
 * the reward system maps to a drop table. Keeping the drop table out of the
 * template preserves the layering (model does not depend on the reward
 * package).
 */
data class EnemyTemplate(
    val id: String,
    val name: String,
    val dice: Dice,
    val hp: Int,
    val rewardTier: Int = 0,
) {
    init {
        require(hp > 0) { "Enemy HP must be positive, got $hp" }
        require(rewardTier >= 0) { "rewardTier must be non-negative, got $rewardTier" }
    }

    /**
     * Materialises this template into a battle-ready [BattleUnit] with an
     * explicit numeric [unitId] (the caller owns id allocation so player and
     * enemy ids never collide inside a single battle).
     */
    fun toUnit(unitId: Int, owner: BattleSide = BattleSide.PLAYER2): BattleUnit =
        BattleUnit.fromDice(
            id = unitId,
            owner = owner,
            name = name,
            dice = dice,
            hp = hp,
        )
}
