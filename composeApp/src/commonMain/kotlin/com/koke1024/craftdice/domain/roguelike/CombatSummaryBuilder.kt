package com.koke1024.craftdice.domain.roguelike

import com.koke1024.craftdice.domain.battle.model.BattleState
import com.koke1024.craftdice.domain.battle.model.BattleStatus
import com.koke1024.craftdice.domain.roguelike.model.BattleSetup
import com.koke1024.craftdice.domain.roguelike.model.CombatSummary

/**
 * Builds the [CombatSummary] handed back to the run engine once a battle ends.
 *
 * Kept as a stateless, side-effect free mapping so it can be unit-tested in
 * isolation from the coroutine/physics machinery in [com.koke1024.craftdice.ui.battle.BattleViewModel].
 *
 * - [CombatSummary.survivingPlayerUnits] are the PLAYER1 units still alive in
 *   the resolved [BattleState] — their post-battle HP and broken faces are
 *   already baked into the unit instances, so the run engine can sync them
 *   straight into [com.koke1024.craftdice.domain.roguelike.model.RunState].
 * - [CombatSummary.defeatedTemplate] is only populated on a PLAYER1 win; on a
 *   loss or draw the run engine never rolls loot, so null is the honest value.
 * - [CombatSummary.roundsFought] mirrors the engine round counter at the end
 *   of the deciding round.
 */
object CombatSummaryBuilder {

    fun build(state: BattleState, setup: BattleSetup): CombatSummary =
        CombatSummary(
            status = state.status,
            survivingPlayerUnits = survivingPlayers(state),
            roundsFought = state.round,
            defeatedTemplate = if (state.status == BattleStatus.PLAYER1_WON) setup.enemyTemplate else null,
        )

    private fun survivingPlayers(state: BattleState) =
        state.player1Units.filter { it.isAlive }
}
