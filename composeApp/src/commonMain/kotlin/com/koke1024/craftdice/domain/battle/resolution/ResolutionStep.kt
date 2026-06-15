package com.koke1024.craftdice.domain.battle.resolution

import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleState

/**
 * Kind of a single resolution step, for display and filtering.
 */
enum class StepKind {
    DEFEND,
    ATTACK,
    CRIT,
    HEAL,
    MISS,
    BROKEN_FACE,
    SYNERGY_BONUS,
    RECOIL,
}

/**
 * One resolved action in a battle round.
 *
 * Produced by [BattleResolver]; [message] is a human-readable description
 * while the numeric fields drive UI feedback (HP bars, face shatter, etc.).
 */
data class ResolutionStep(
    val kind: StepKind,
    val actorUnitId: Int,
    val owner: BattleSide,
    val message: String,
    val targetUnitId: Int? = null,
    val damageDealt: Int = 0,
    val healAmount: Int = 0,
    val facesBroken: Int = 0,
)

/**
 * Outcome of resolving a full round.
 *
 * Contains the ordered [steps] and the resulting [state] (HP changes, broken
 * faces, win/loss status applied, defending flags cleared).
 */
data class ResolutionResult(
    val steps: List<ResolutionStep>,
    val state: BattleState,
)
