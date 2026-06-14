package com.koke1024.craftdice.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SkillTypeTest {

    @Test
    fun `ATK has value and correct display name`() {
        assertEquals("攻撃", SkillType.ATK.displayName)
        assertTrue(SkillType.ATK.hasValue)
    }

    @Test
    fun `DEF has no value and correct display name`() {
        assertEquals("防御", SkillType.DEF.displayName)
        assertFalse(SkillType.DEF.hasValue)
    }

    @Test
    fun `HEAL has value and correct display name`() {
        assertEquals("回復", SkillType.HEAL.displayName)
        assertTrue(SkillType.HEAL.hasValue)
    }

    @Test
    fun `CRIT has value and correct display name`() {
        assertEquals("必殺", SkillType.CRIT.displayName)
        assertTrue(SkillType.CRIT.hasValue)
    }

    @Test
    fun `MISS has no value and correct display name`() {
        assertEquals("ミス", SkillType.MISS.displayName)
        assertFalse(SkillType.MISS.hasValue)
    }

    @Test
    fun `hasValue partition covers all five types`() {
        val withValue = SkillType.entries.filter { it.hasValue }
        val withoutValue = SkillType.entries.filter { !it.hasValue }
        assertEquals(listOf(SkillType.ATK, SkillType.HEAL, SkillType.CRIT), withValue)
        assertEquals(listOf(SkillType.DEF, SkillType.MISS), withoutValue)
    }
}
