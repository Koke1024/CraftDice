package com.koke1024.craftdice.domain.probability

import com.koke1024.craftdice.domain.model.SkillType
import kotlin.math.roundToInt

/**
 * Complete probability distribution for a dice.
 *
 * The physics engine (handoff physics.js) selects the final face uniformly
 * at random when a die settles, so each face has equal probability = 1 / faceCount.
 *
 * @param faceCount         total number of faces on the dice
 * @param faceProbabilities per-slot probabilities
 * @param skillProbabilities aggregated by [SkillType]
 * @param expectedAttackDamage  expected raw damage from ATK faces per roll
 * @param expectedCritDamage    expected raw damage from CRIT faces per roll
 * @param expectedHeal          expected HP restored from HEAL faces per roll
 */
data class DiceProbability(
    val faceCount: Int,
    val faceProbabilities: List<FaceProbability>,
    val skillProbabilities: Map<SkillType, SkillProbability>,
    val expectedAttackDamage: Double,
    val expectedCritDamage: Double,
    val expectedHeal: Double,
) {
    val expectedTotalDamage: Double
        get() = expectedAttackDamage + expectedCritDamage

    val probabilityOfMiss: Double
        get() = skillProbabilities[SkillType.MISS]?.probability ?: 0.0

    val probabilityOfDefend: Double
        get() = skillProbabilities[SkillType.DEF]?.probability ?: 0.0

    val probabilityOfHit: Double
        get() = 1.0 - probabilityOfMiss

    fun skillProbability(type: SkillType): Double =
        skillProbabilities[type]?.probability ?: 0.0

    fun skillFaceCount(type: SkillType): Int =
        skillProbabilities[type]?.faceCount ?: 0

    /**
     * Formats [probability] as an integer percentage (0-100).
     */
    fun percent(probability: Double): Int =
        (probability * 100).roundToInt()
}
