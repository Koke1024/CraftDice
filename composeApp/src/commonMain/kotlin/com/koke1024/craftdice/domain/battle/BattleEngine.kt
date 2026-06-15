package com.koke1024.craftdice.domain.battle

import com.koke1024.craftdice.domain.battle.model.BattleRule
import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleState
import com.koke1024.craftdice.domain.battle.model.BattleStatus
import com.koke1024.craftdice.domain.battle.model.BattleUnit
import com.koke1024.craftdice.domain.battle.outcome.OutcomeMapper
import com.koke1024.craftdice.domain.battle.resolution.BattleResolver
import com.koke1024.craftdice.domain.battle.resolution.ResolutionResult
import com.koke1024.craftdice.domain.physics.DiceRollResult

/**
 * A single throw entry in the alternating throw order of a round.
 */
data class ThrowEntry(
    val side: BattleSide,
    val unitId: Int,
)

/**
 * Orchestrates the turn-based battle flow (Phase 3 top-level entry point).
 *
 * The engine is a thin state holder over the pure [BattleResolver]: it tracks
 * the current [BattleState], exposes the alternating throw order, and feeds
 * Phase 2 [DiceRollResult]s through resolution once the tray settles.
 *
 * The UI/ViewModel drives the physics and calls [resolveRound]; this class
 * stays free of coroutine/physics concerns so it remains unit-testable.
 */
class BattleEngine(
    initialRule: BattleRule = BattleRule.BUMP,
    private val outcomeMapper: OutcomeMapper = OutcomeMapper(),
    private val resolver: BattleResolver = BattleResolver(),
) {
    var state: BattleState = BattleState(emptyMap(), rule = initialRule)
        private set

    val isFinished: Boolean get() = state.status.isFinished

    val round: Int get() = state.round

    fun setup(
        player1Units: List<BattleUnit>,
        player2Units: List<BattleUnit>,
        rule: BattleRule = state.rule,
    ): BattleState {
        state = BattleState(
            unitsBySide = mapOf(
                BattleSide.PLAYER1 to player1Units,
                BattleSide.PLAYER2 to player2Units,
            ),
            round = 1,
            status = BattleStatus.ONGOING,
            rule = rule,
        )
        return state
    }

    /**
     * The throw order for the current round: strictly alternating
     * PLAYER1 → PLAYER2 → PLAYER1 … by unit index, alive units only.
     */
    fun throwOrder(): List<ThrowEntry> {
        val p1 = state.aliveUnits(BattleSide.PLAYER1)
        val p2 = state.aliveUnits(BattleSide.PLAYER2)
        val max = maxOf(p1.size, p2.size)
        val order = mutableListOf<ThrowEntry>()
        for (i in 0 until max) {
            if (i < p1.size) order.add(ThrowEntry(BattleSide.PLAYER1, p1[i].id))
            if (i < p2.size) order.add(ThrowEntry(BattleSide.PLAYER2, p2[i].id))
        }
        return order
    }

    /**
     * Resolves a round once all dice have settled. Maps the [DiceRollResult]
     * to outcomes, runs resolution, updates state, and advances the round
     * counter when the battle is still ongoing.
     */
    fun resolveRound(rollResult: DiceRollResult): ResolutionResult {
        val outcomes = outcomeMapper.map(rollResult, state)
        val result = resolver.resolve(state, outcomes)
        state = if (result.state.status.isFinished) result.state else result.state.nextRound()
        return result
    }
}
