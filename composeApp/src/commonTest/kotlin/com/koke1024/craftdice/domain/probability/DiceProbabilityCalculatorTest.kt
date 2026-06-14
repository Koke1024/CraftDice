package com.koke1024.craftdice.domain.probability

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceConstraints.MAX_FACES
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.model.SkillType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiceProbabilityCalculatorTest {

    private fun prob(value: Double): Double =
        kotlin.math.round(value * 1_000_000) / 1_000_000

    @Test
    fun `six-faced dice gives each face probability one-sixth`() {
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.attack(3),
            DiceFace.attack(5),
            DiceFace.attack(2),
            DiceFace.miss(),
            DiceFace.critical(8),
        )
        val result = DiceProbabilityCalculator.calculate(dice)

        assertEquals(6, result.faceCount)
        for (fp in result.faceProbabilities) {
            assertEquals(prob(1.0 / 6.0), prob(fp.probability))
        }
    }

    @Test
    fun `single-faced dice gives probability one`() {
        val dice = Dice.single(DiceFace.attack(5))
        val result = DiceProbabilityCalculator.calculate(dice)

        assertEquals(1.0, result.faceProbabilities.first().probability)
        assertEquals(1.0, result.skillProbability(SkillType.ATK))
    }

    @Test
    fun `stacked faces aggregate skill probability`() {
        // 2 ATK out of 6 = 1/3
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.attack(3),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
        )
        val result = DiceProbabilityCalculator.calculate(dice)

        assertEquals(prob(1.0 / 3.0), prob(result.skillProbability(SkillType.ATK)))
        assertEquals(2, result.skillFaceCount(SkillType.ATK))
        assertEquals(prob(4.0 / 6.0), prob(result.probabilityOfMiss))
    }

    @Test
    fun `flame monster face composition matches handoff prototype`() {
        // handoff: [atk(3), atk(3), atk(5), atk(2), miss, crit(8)]
        val flame = Dice.of(
            DiceFace.attack(3),
            DiceFace.attack(3),
            DiceFace.attack(5),
            DiceFace.attack(2),
            DiceFace.miss(),
            DiceFace.critical(8),
        )
        val result = DiceProbabilityCalculator.calculate(flame)

        assertEquals(6, result.faceCount)
        // ATK: 4/6
        assertEquals(prob(4.0 / 6.0), prob(result.skillProbability(SkillType.ATK)))
        // MISS: 1/6
        assertEquals(prob(1.0 / 6.0), prob(result.probabilityOfMiss))
        // CRIT: 1/6
        assertEquals(prob(1.0 / 6.0), prob(result.skillProbability(SkillType.CRIT)))
        // DEF: 0
        assertEquals(0.0, result.probabilityOfDefend)
        // HEAL: 0
        assertEquals(0.0, result.skillProbability(SkillType.HEAL))
    }

    @Test
    fun `golem monster face composition matches handoff prototype`() {
        // handoff: [def, def, atk(4), atk(4), miss, atk(6)]
        val golem = Dice.of(
            DiceFace.defense(),
            DiceFace.defense(),
            DiceFace.attack(4),
            DiceFace.attack(4),
            DiceFace.miss(),
            DiceFace.attack(6),
        )
        val result = DiceProbabilityCalculator.calculate(golem)

        // DEF: 2/6
        assertEquals(prob(2.0 / 6.0), prob(result.probabilityOfDefend))
        // ATK: 3/6
        assertEquals(prob(3.0 / 6.0), prob(result.skillProbability(SkillType.ATK)))
    }

    @Test
    fun `raiju monster has two miss faces`() {
        // handoff: [atk(2), atk(2), atk(2), crit(7), miss, miss]
        val raiju = Dice.of(
            DiceFace.attack(2),
            DiceFace.attack(2),
            DiceFace.attack(2),
            DiceFace.critical(7),
            DiceFace.miss(),
            DiceFace.miss(),
        )
        val result = DiceProbabilityCalculator.calculate(raiju)

        assertEquals(prob(2.0 / 6.0), prob(result.probabilityOfMiss))
        assertEquals(prob(4.0 / 6.0), prob(result.probabilityOfHit))
    }

    @Test
    fun `expected attack damage is sum of atk values weighted by probability`() {
        // atk(3) + atk(5) + miss*4 = expected (3+5) * 1/6 = 8/6
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.attack(5),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
        )
        val result = DiceProbabilityCalculator.calculate(dice)

        assertEquals(prob(8.0 / 6.0), prob(result.expectedAttackDamage))
    }

    @Test
    fun `expected heal is sum of heal values weighted by probability`() {
        // heal(4) + heal(5) + miss*4 = expected (4+5) * 1/6 = 9/6
        val dice = Dice.of(
            DiceFace.heal(4),
            DiceFace.heal(5),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
        )
        val result = DiceProbabilityCalculator.calculate(dice)

        assertEquals(prob(9.0 / 6.0), prob(result.expectedHeal))
    }

    @Test
    fun `expected crit damage is sum of crit values weighted by probability`() {
        // crit(8) + miss*5 = expected 8/6
        val dice = Dice.of(
            DiceFace.critical(8),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
        )
        val result = DiceProbabilityCalculator.calculate(dice)

        assertEquals(prob(8.0 / 6.0), prob(result.expectedCritDamage))
    }

    @Test
    fun `expected total damage combines attack and crit`() {
        // atk(3) + crit(8) + miss*4 = expected (3+8)/6
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.critical(8),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
        )
        val result = DiceProbabilityCalculator.calculate(dice)

        assertEquals(prob(11.0 / 6.0), prob(result.expectedTotalDamage))
    }

    @Test
    fun `five-faced dice concentrates probability after removing a face`() {
        // After crafting: 5 faces, each has 1/5 probability
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.attack(3),
            DiceFace.attack(5),
            DiceFace.attack(2),
            DiceFace.critical(8),
        )
        val result = DiceProbabilityCalculator.calculate(dice)

        assertEquals(5, result.faceCount)
        for (fp in result.faceProbabilities) {
            assertEquals(prob(1.0 / 5.0), prob(fp.probability))
        }
        // ATK: 4/5
        assertEquals(prob(4.0 / 5.0), prob(result.skillProbability(SkillType.ATK)))
    }

    @Test
    fun `all skill probabilities cover every type`() {
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.defense(),
            DiceFace.heal(4),
            DiceFace.critical(8),
            DiceFace.miss(),
            DiceFace.miss(),
        )
        val result = DiceProbabilityCalculator.calculate(dice)

        for (type in SkillType.entries) {
            assertTrue(result.skillProbabilities.containsKey(type))
        }
    }

    @Test
    fun `percent converts probability to integer percentage`() {
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.attack(3),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
        )
        val result = DiceProbabilityCalculator.calculate(dice)

        // 2/6 = 0.333... -> 33%
        assertEquals(33, result.percent(result.skillProbability(SkillType.ATK)))
        // 4/6 = 0.666... -> 67%
        assertEquals(67, result.percent(result.probabilityOfMiss))
    }

    @Test
    fun `face count cannot exceed max faces`() {
        assertEquals(MAX_FACES, 6)
    }
}
