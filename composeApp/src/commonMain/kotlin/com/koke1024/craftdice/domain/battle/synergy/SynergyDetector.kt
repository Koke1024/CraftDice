package com.koke1024.craftdice.domain.battle.synergy

import com.koke1024.craftdice.domain.battle.model.BattleConfig
import com.koke1024.craftdice.domain.battle.outcome.DiceOutcome
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.model.SkillType

/**
 * Detects synergy bonuses for one side's roll outcomes.
 *
 * Priority order: PINZORO > STRAIGHT > ZOROME > NONE.
 * Broken faces are excluded so a shattered die cannot contribute to synergy.
 */
class SynergyDetector(
    private val zoromeMinDice: Int = BattleConfig.ZOROME_MIN_DICE,
    private val straightMinDice: Int = BattleConfig.STRAIGHT_MIN_DICE,
    private val zoromeMultiplier: Double = BattleConfig.ZOROME_MULTIPLIER,
    private val pinzoroMultiplier: Double = BattleConfig.PINZORO_MULTIPLIER,
    private val straightBonus: Int = BattleConfig.STRAIGHT_BONUS_DAMAGE,
    private val pinzoroRecoil: Int = BattleConfig.PINZORO_RECOIL,
    private val pinzoroMinDice: Int = BattleConfig.STRAIGHT_MIN_DICE,
) {
    fun detect(outcomes: List<DiceOutcome>): SynergyResult {
        val active = outcomes.filter { !it.isFaceBroken }
        if (active.size < zoromeMinDice) return SynergyResult(SynergyType.NONE)

        if (active.size >= pinzoroMinDice && isAllIdentical(active)) {
            return SynergyResult(
                type = SynergyType.PINZORO,
                valueMultiplier = pinzoroMultiplier,
                recoilDamage = pinzoroRecoil,
            )
        }

        if (active.size >= straightMinDice && isStraight(active)) {
            return SynergyResult(
                type = SynergyType.STRAIGHT,
                bonusDamage = straightBonus,
            )
        }

        if (hasRepeatedFace(active)) {
            return SynergyResult(
                type = SynergyType.ZOROME,
                valueMultiplier = zoromeMultiplier,
            )
        }

        return SynergyResult(SynergyType.NONE)
    }

    private fun isAllIdentical(active: List<DiceOutcome>): Boolean {
        if (active.size < 2) return false
        val first = faceKey(active.first().face)
        return active.all { faceKey(it.face) == first }
    }

    private fun isStraight(active: List<DiceOutcome>): Boolean {
        val atkValues = active
            .filter { it.face.skillType == SkillType.ATK }
            .map { it.face.value }
            .distinct()
            .sorted()
        if (atkValues.size < straightMinDice) return false
        for (i in 1 until atkValues.size) {
            if (atkValues[i] != atkValues[i - 1] + 1) return false
        }
        return true
    }

    private fun hasRepeatedFace(active: List<DiceOutcome>): Boolean =
        active.groupBy { faceKey(it.face) }.any { it.value.size >= zoromeMinDice }

    private fun faceKey(face: DiceFace): Pair<SkillType, Int> = face.skillType to face.value
}
