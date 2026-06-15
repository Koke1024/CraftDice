package com.koke1024.craftdice.domain.battle.model

/**
 * Battle participant sides.
 *
 * PLAYER1 is the human player; PLAYER2 is the CPU/enemy.
 */
enum class BattleSide(val displayName: String) {
    PLAYER1("プレイヤー"),
    PLAYER2("エネミー");

    fun opponent(): BattleSide = if (this == PLAYER1) PLAYER2 else PLAYER1
}
