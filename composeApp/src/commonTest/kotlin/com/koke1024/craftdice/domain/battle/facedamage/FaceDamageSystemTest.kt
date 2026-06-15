package com.koke1024.craftdice.domain.battle.facedamage

import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleUnit
import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FaceDamageSystemTest {

    private fun unit(broken: Set<Int> = emptySet()): BattleUnit {
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.attack(4),
            DiceFace.heal(5),
            DiceFace.defense(),
        )
        return BattleUnit(0, BattleSide.PLAYER1, "U", dice, 20, 20, broken)
    }

    @Test
    fun selectFaceToBreak_returnsNullWhenAllFacesBroken() {
        val system = FaceDamageSystem(random = { 0.0 })
        val fullyBroken = unit(broken = setOf(0, 1, 2, 3))

        assertNull(system.selectFaceToBreak(fullyBroken))
    }

    @Test
    fun selectFaceToBreak_picksAnIntactFace() {
        val system = FaceDamageSystem(random = { 0.0 })
        val u = unit()

        val face = system.selectFaceToBreak(u)

        assertNotNull(face)
        assertTrue(face in 0 until u.dice.faceCount)
        assertFalse(u.isFaceBroken(face))
    }

    @Test
    fun applyDamage_reducesHpAndBreaksOneFace() {
        val system = FaceDamageSystem(random = { 0.0 })
        val u = unit()

        val damaged = system.applyDamage(u, 5)

        assertEquals(15, damaged.currentHp)
        assertEquals(1, damaged.brokenFaceIndices.size)
    }

    @Test
    fun applyDamage_breaksAdditionalFacesWithMultipleHits() {
        val twoHits = FaceDamageSystem(facesPerHit = 2, random = { 0.0 })
        val u = unit()

        val damaged = twoHits.applyDamage(u, 5)

        assertEquals(2, damaged.brokenFaceIndices.size)
    }

    @Test
    fun applyDamage_doesNotBreakAlreadyBrokenFaceTwice() {
        val system = FaceDamageSystem(facesPerHit = 2, random = { 0.0 })
        val u = unit(broken = setOf(0))

        val damaged = system.applyDamage(u, 3)

        assertTrue(0 in damaged.brokenFaceIndices)
        assertEquals(3, damaged.brokenFaceIndices.size)
    }

    @Test
    fun applyDamage_zeroDamageChangesNothing() {
        val system = FaceDamageSystem(random = { 0.0 })
        val u = unit()

        val damaged = system.applyDamage(u, 0)

        assertEquals(u, damaged)
    }

    @Test
    fun applyDamage_unitBecomesShatteredWhenAllFacesBroken() {
        val system = FaceDamageSystem(facesPerHit = 4, random = { 0.0 })
        val u = unit()

        val damaged = system.applyDamage(u, 1)

        assertTrue(damaged.isShattered)
    }

    @Test
    fun applyDamage_isDeterministicWithFixedRandom() {
        val a = FaceDamageSystem(random = { 0.5 }).applyDamage(unit(), 1)
        val b = FaceDamageSystem(random = { 0.5 }).applyDamage(unit(), 1)

        assertEquals(a.brokenFaceIndices, b.brokenFaceIndices)
    }
}
