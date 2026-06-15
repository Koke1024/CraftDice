package com.koke1024.craftdice.domain.battle.model

/**
 * Tunable battle parameters.
 *
 * Values are derived from the handoff prototype and the Phase 3 design.
 */
object BattleConfig {
    const val DEFAULT_HP = 24

    const val CENTER_BONUS = 2
    const val DEFENSE_DIVISOR = 2
    const val MIN_DAMAGE_AFTER_DEFENSE = 1

    const val CENTER_ZONE_RATIO = 0.2

    const val ZOROME_MIN_DICE = 2
    const val STRAIGHT_MIN_DICE = 3
    const val ZOROME_MULTIPLIER = 2.0
    const val PINZORO_MULTIPLIER = 3.0
    const val STRAIGHT_BONUS_DAMAGE = 5
    const val PINZORO_RECOIL = 3

    const val FACE_DAMAGE_PER_HIT = 1

    const val BROKEN_FACE_SELF_DAMAGE = 2
}
