package com.koke1024.craftdice.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DiceTest {

    @Test
    fun `dice with single face is valid`() {
        val dice = Dice.single(DiceFace.miss())
        assertEquals(1, dice.faceCount)
    }

    @Test
    fun `dice with six faces is valid`() {
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.attack(3),
            DiceFace.attack(5),
            DiceFace.attack(2),
            DiceFace.miss(),
            DiceFace.critical(8),
        )
        assertEquals(6, dice.faceCount)
    }

    @Test
    fun `empty dice is rejected`() {
        assertFailsWith<IllegalArgumentException> { Dice(emptyList()) }
    }

    @Test
    fun `dice with seven faces is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Dice(List(7) { DiceFace.miss() })
        }
    }

    @Test
    fun `countBySkillType returns correct counts`() {
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.attack(5),
            DiceFace.defense(),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.critical(8),
        )
        assertEquals(2, dice.countBySkillType(SkillType.ATK))
        assertEquals(1, dice.countBySkillType(SkillType.DEF))
        assertEquals(0, dice.countBySkillType(SkillType.HEAL))
        assertEquals(1, dice.countBySkillType(SkillType.CRIT))
        assertEquals(2, dice.countBySkillType(SkillType.MISS))
    }

    @Test
    fun `facesOfSkillType returns matching faces`() {
        val atk3 = DiceFace.attack(3)
        val atk5 = DiceFace.attack(5)
        val dice = Dice.of(atk3, DiceFace.miss(), atk5, DiceFace.defense())
        assertEquals(listOf(atk3, atk5), dice.facesOfSkillType(SkillType.ATK))
    }

    @Test
    fun `faceAt returns face at given index`() {
        val face0 = DiceFace.attack(1)
        val face1 = DiceFace.heal(2)
        val dice = Dice.of(face0, face1, DiceFace.miss())
        assertEquals(face0, dice.faceAt(0))
        assertEquals(face1, dice.faceAt(1))
    }
}
