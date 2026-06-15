package com.koke1024.craftdice.domain.battle.outcome

import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.model.DiceFace

/**
 * A single rolled dice mapped into battle context.
 *
 * Bridges the Phase 2 physics [DiceRollResult] and the Phase 3 resolution
 * engine by attaching the owning unit, whether the landed face is shattered,
 * and whether it landed in the central bonus zone.
 */
data class DiceOutcome(
    val unitId: Int,
    val owner: BattleSide,
    val face: DiceFace,
    val faceIndex: Int,
    val isFaceBroken: Boolean,
    val centerBonus: Boolean,
)
