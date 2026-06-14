package com.koke1024.craftdice.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DiceFaceTest {

    @Test
    fun `attack factory creates ATK face with value`() {
        val face = DiceFace.attack(5)
        assertEquals(SkillType.ATK, face.skillType)
        assertEquals(5, face.value)
        assertEquals(Rarity.COMMON, face.rarity)
    }

    @Test
    fun `defense factory creates DEF face with zero value`() {
        val face = DiceFace.defense()
        assertEquals(SkillType.DEF, face.skillType)
        assertEquals(0, face.value)
    }

    @Test
    fun `heal factory creates HEAL face with value`() {
        val face = DiceFace.heal(4)
        assertEquals(SkillType.HEAL, face.skillType)
        assertEquals(4, face.value)
    }

    @Test
    fun `critical factory creates CRIT face with value`() {
        val face = DiceFace.critical(8)
        assertEquals(SkillType.CRIT, face.skillType)
        assertEquals(8, face.value)
    }

    @Test
    fun `miss factory creates MISS face with zero value`() {
        val face = DiceFace.miss()
        assertEquals(SkillType.MISS, face.skillType)
        assertEquals(0, face.value)
    }

    @Test
    fun `factory methods accept custom rarity`() {
        assertEquals(Rarity.LEGENDARY, DiceFace.attack(5, Rarity.LEGENDARY).rarity)
        assertEquals(Rarity.RARE, DiceFace.defense(Rarity.RARE).rarity)
        assertEquals(Rarity.EPIC, DiceFace.heal(3, Rarity.EPIC).rarity)
        assertEquals(Rarity.LEGENDARY, DiceFace.critical(9, Rarity.LEGENDARY).rarity)
        assertEquals(Rarity.RARE, DiceFace.miss(Rarity.RARE).rarity)
    }

    @Test
    fun `ATK face rejects zero value`() {
        assertFailsWith<IllegalArgumentException> { DiceFace(SkillType.ATK, 0) }
    }

    @Test
    fun `ATK face rejects negative value`() {
        assertFailsWith<IllegalArgumentException> { DiceFace(SkillType.ATK, -1) }
    }

    @Test
    fun `HEAL face rejects zero value`() {
        assertFailsWith<IllegalArgumentException> { DiceFace(SkillType.HEAL, 0) }
    }

    @Test
    fun `CRIT face rejects zero value`() {
        assertFailsWith<IllegalArgumentException> { DiceFace(SkillType.CRIT, 0) }
    }

    @Test
    fun `DEF face rejects non-zero value`() {
        assertFailsWith<IllegalArgumentException> { DiceFace(SkillType.DEF, 1) }
    }

    @Test
    fun `MISS face rejects non-zero value`() {
        assertFailsWith<IllegalArgumentException> { DiceFace(SkillType.MISS, 1) }
    }

    @Test
    fun `face value above max is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            DiceFace(SkillType.ATK, DiceConstraints.MAX_FACE_VALUE + 1)
        }
    }

    @Test
    fun `default rarity is COMMON`() {
        assertNull(null)
        assertEquals(Rarity.COMMON, DiceFace.attack(3).rarity)
        assertEquals(Rarity.COMMON, DiceFace.defense().rarity)
    }
}
