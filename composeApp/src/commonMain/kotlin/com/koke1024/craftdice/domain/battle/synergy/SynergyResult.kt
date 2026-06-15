package com.koke1024.craftdice.domain.battle.synergy

/**
 * Detected synergy category for a single side's roll.
 *
 * - [ZOROME]: two or more (but not all) dice share an identical face; effect
 *   magnitudes are multiplied.
 * - [STRAIGHT]: three or more ATK faces form a consecutive value run; grants a
 *   bonus action (flat bonus damage).
 * - [PINZORO]: every die shows an identical face; strongest multiplier but
 *   inflicts recoil self-damage (high risk, high return).
 */
enum class SynergyType {
    NONE,
    ZOROME,
    STRAIGHT,
    PINZORO,
}

/**
 * Result of synergy detection for one side.
 *
 * [valueMultiplier] scales ATK/HEAL/CRIT magnitudes. [bonusDamage] is applied
 * as an extra strike. [recoilDamage] is self-damage risk (pinzoro).
 */
data class SynergyResult(
    val type: SynergyType,
    val valueMultiplier: Double = 1.0,
    val bonusDamage: Int = 0,
    val recoilDamage: Int = 0,
) {
    val hasSynergy: Boolean get() = type != SynergyType.NONE
}
