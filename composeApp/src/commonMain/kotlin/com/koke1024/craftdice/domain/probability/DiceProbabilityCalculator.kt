package com.koke1024.craftdice.domain.probability

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.SkillType

/**
 * Calculates the probability distribution of a [Dice].
 *
 * The handoff physics engine assigns the final face uniformly at random
 * (physics.js: `faceIdx = (Math.random() * 6) | 0`), so every face
 * has equal probability 1 / faceCount regardless of physics trajectory.
 */
object DiceProbabilityCalculator {

    fun calculate(dice: Dice): DiceProbability {
        val faceCount = dice.faceCount
        val perFace = 1.0 / faceCount

        val faceProbabilities = dice.faces.mapIndexed { index, face ->
            FaceProbability(index, face, perFace)
        }

        val skillProbabilities = SkillType.entries.associateWith { type ->
            val count = dice.countBySkillType(type)
            SkillProbability(type, count * perFace, count)
        }

        val expectedAttack = expectedValue(dice, SkillType.ATK, perFace)
        val expectedCrit = expectedValue(dice, SkillType.CRIT, perFace)
        val expectedHeal = expectedValue(dice, SkillType.HEAL, perFace)

        return DiceProbability(
            faceCount = faceCount,
            faceProbabilities = faceProbabilities,
            skillProbabilities = skillProbabilities,
            expectedAttackDamage = expectedAttack,
            expectedCritDamage = expectedCrit,
            expectedHeal = expectedHeal,
        )
    }

    private fun expectedValue(dice: Dice, type: SkillType, perFace: Double): Double =
        dice.facesOfSkillType(type).sumOf { it.value * perFace }
}
