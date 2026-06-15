package com.koke1024.craftdice.domain.battle.resolution

import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleState
import com.koke1024.craftdice.domain.battle.model.BattleUnit

/**
 * Selects targets during battle resolution.
 *
 * Allows the CPU targeting strategy to vary (random by default, smarter AI
 * later) and enables deterministic tests.
 */
interface TargetSelector {
    fun selectAttackTarget(state: BattleState, attackerSide: BattleSide): BattleUnit?

    fun selectHealTarget(state: BattleState, healer: BattleUnit): BattleUnit?
}

/**
 * Random target selector mirroring the handoff prototype.
 *
 * Attacks hit a random alive enemy; heals land on the most-damaged alive
 * ally (lowest HP ratio), falling back to the healer.
 */
class RandomTargetSelector(
    private val random: () -> Double = { kotlin.random.Random.nextDouble() },
) : TargetSelector {
    override fun selectAttackTarget(state: BattleState, attackerSide: BattleSide): BattleUnit? {
        val foes = state.aliveUnits(attackerSide.opponent())
        if (foes.isEmpty()) return null
        val index = (random() * foes.size).toInt().coerceIn(0, foes.lastIndex)
        return foes[index]
    }

    override fun selectHealTarget(state: BattleState, healer: BattleUnit): BattleUnit? {
        val team = state.aliveUnits(healer.owner)
        if (team.isEmpty()) return null
        return team.minByOrNull { it.hpRatio } ?: healer
    }
}
