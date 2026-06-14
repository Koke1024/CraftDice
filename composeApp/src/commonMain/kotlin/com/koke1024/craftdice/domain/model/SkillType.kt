package com.koke1024.craftdice.domain.model

/**
 * Dice face skill types.
 *
 * Based on the handoff prototype (game-data.js):
 * - ATK:  deals damage equal to [value]
 * - DEF:  halves incoming damage while active this round
 * - HEAL: restores HP equal to [value]
 * - CRIT: deals damage equal to [value] (typically high)
 * - MISS: no effect (wasted roll)
 */
enum class SkillType(
    val displayName: String,
    val hasValue: Boolean,
) {
    ATK("攻撃", true),
    DEF("防御", false),
    HEAL("回復", true),
    CRIT("必殺", true),
    MISS("ミス", false),
}
