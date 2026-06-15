package com.koke1024.craftdice.domain.battle.synergy

import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.outcome.DiceOutcome
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.model.SkillType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SynergyDetectorTest {

    private val detector = SynergyDetector()

    private fun outcome(
        value: Int,
        type: SkillType = SkillType.ATK,
        broken: Boolean = false,
    ): DiceOutcome = DiceOutcome(
        unitId = 0,
        owner = BattleSide.PLAYER1,
        face = if (type.hasValue) DiceFace(type, value) else DiceFace(type),
        faceIndex = 0,
        isFaceBroken = broken,
        centerBonus = false,
    )

    @Test
    fun detect_singleDiceHasNoSynergy() {
        val result = detector.detect(listOf(outcome(3)))
        assertEquals(SynergyType.NONE, result.type)
        assertFalse(result.hasSynergy)
    }

    @Test
    fun detect_twoMatchingAtkFacesIsZorome() {
        val result = detector.detect(listOf(outcome(3), outcome(3)))
        assertEquals(SynergyType.ZOROME, result.type)
        assertEquals(2.0, result.valueMultiplier)
    }

    @Test
    fun detect_allMatchingFacesIsPinzoro() {
        val result = detector.detect(listOf(outcome(5), outcome(5), outcome(5)))
        assertEquals(SynergyType.PINZORO, result.type)
        assertEquals(3.0, result.valueMultiplier)
        assertTrue(result.recoilDamage > 0)
    }

    @Test
    fun detect_threeConsecutiveAtkValuesIsStraight() {
        val result = detector.detect(listOf(outcome(3), outcome(4), outcome(5)))
        assertEquals(SynergyType.STRAIGHT, result.type)
        assertTrue(result.bonusDamage > 0)
    }

    @Test
    fun detect_brokenFacesAreIgnored() {
        val result = detector.detect(listOf(outcome(3, broken = true), outcome(3, broken = true)))
        assertEquals(SynergyType.NONE, result.type)
    }

    @Test
    fun detect_pinzoroTakesPriorityOverZorome() {
        val result = detector.detect(listOf(outcome(3), outcome(3)))
        assertEquals(SynergyType.ZOROME, result.type)

        val pin = detector.detect(listOf(outcome(3), outcome(3), outcome(3)))
        assertEquals(SynergyType.PINZORO, pin.type)
    }

    @Test
    fun detect_nonConsecutiveValuesIsNoSynergy() {
        val result = detector.detect(listOf(outcome(3), outcome(5), outcome(8)))
        assertEquals(SynergyType.NONE, result.type)
    }

    @Test
    fun detect_defenseFacesCanFormZorome() {
        val result = detector.detect(
            listOf(
                outcome(0, type = SkillType.DEF),
                outcome(0, type = SkillType.DEF),
                outcome(3),
            ),
        )
        assertEquals(SynergyType.ZOROME, result.type)
    }
}
