package com.koke1024.craftdice.domain.battle.model

/**
 * Overall battle outcome state.
 */
enum class BattleStatus {
    ONGOING,
    PLAYER1_WON,
    PLAYER2_WON,
    DRAW;

    val isFinished: Boolean get() = this != ONGOING

    fun winningSide(): BattleSide? = when (this) {
        PLAYER1_WON -> BattleSide.PLAYER1
        PLAYER2_WON -> BattleSide.PLAYER2
        else -> null
    }
}
