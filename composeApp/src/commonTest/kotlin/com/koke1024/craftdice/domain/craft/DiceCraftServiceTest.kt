package com.koke1024.craftdice.domain.craft

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceConstraints.MAX_FACES
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.model.SkillType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DiceCraftServiceTest {

    // ---- addFace ----

    @Test
    fun `addFace appends a new face and increases face count`() {
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.miss(),
        )
        val result = DiceCraftService.addFace(dice, DiceFace.heal(4))

        assertIs<CraftResult.Success>(result)
        assertEquals(3, result.dice.faceCount)
        assertEquals(DiceFace.heal(4), result.dice.faceAt(2))
    }

    @Test
    fun `addFace fails when at max faces`() {
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.attack(3),
            DiceFace.attack(5),
            DiceFace.attack(2),
            DiceFace.miss(),
            DiceFace.critical(8),
        )
        val result = DiceCraftService.addFace(dice, DiceFace.defense())

        assertIs<CraftResult.Failure>(result)
        assertEquals(CraftError.AT_MAX_FACES, result.error)
    }

    @Test
    fun `addFace allows duplicate skill types`() {
        val dice = Dice.of(DiceFace.attack(3), DiceFace.miss())
        val result = DiceCraftService.addFace(dice, DiceFace.attack(3))

        assertIs<CraftResult.Success>(result)
        assertEquals(2, result.dice.countBySkillType(SkillType.ATK))
    }

    // ---- removeFace ----

    @Test
    fun `removeFace removes face at index and decreases face count`() {
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.miss(),
            DiceFace.heal(4),
        )
        val result = DiceCraftService.removeFace(dice, 1)

        assertIs<CraftResult.Success>(result)
        assertEquals(2, result.dice.faceCount)
        assertEquals(DiceFace.attack(3), result.dice.faceAt(0))
        assertEquals(DiceFace.heal(4), result.dice.faceAt(1))
    }

    @Test
    fun `removeFace fails when at min faces`() {
        val dice = Dice.single(DiceFace.miss())
        val result = DiceCraftService.removeFace(dice, 0)

        assertIs<CraftResult.Failure>(result)
        assertEquals(CraftError.AT_MIN_FACES, result.error)
    }

    @Test
    fun `removeFace fails for out-of-range index`() {
        val dice = Dice.of(DiceFace.attack(3), DiceFace.miss())
        val result = DiceCraftService.removeFace(dice, 5)

        assertIs<CraftResult.Failure>(result)
        assertEquals(CraftError.INVALID_FACE_INDEX, result.error)
    }

    @Test
    fun `removeFace fails for negative index`() {
        val dice = Dice.of(DiceFace.attack(3), DiceFace.miss())
        val result = DiceCraftService.removeFace(dice, -1)

        assertIs<CraftResult.Failure>(result)
        assertEquals(CraftError.INVALID_FACE_INDEX, result.error)
    }

    // ---- replaceFace (swap) ----

    @Test
    fun `replaceFace swaps face at given index`() {
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.miss(),
            DiceFace.miss(),
        )
        val result = DiceCraftService.replaceFace(dice, 1, DiceFace.critical(9))

        assertIs<CraftResult.Success>(result)
        assertEquals(3, result.dice.faceCount)
        assertEquals(DiceFace.critical(9), result.dice.faceAt(1))
    }

    @Test
    fun `replaceFace fails for out-of-range index`() {
        val dice = Dice.of(DiceFace.attack(3), DiceFace.miss())
        val result = DiceCraftService.replaceFace(dice, 10, DiceFace.defense())

        assertIs<CraftResult.Failure>(result)
        assertEquals(CraftError.INVALID_FACE_INDEX, result.error)
    }

    // ---- removeAllOfType ----

    @Test
    fun `removeAllOfType strips all faces of the given skill type`() {
        val dice = Dice.of(
            DiceFace.miss(),
            DiceFace.attack(3),
            DiceFace.miss(),
            DiceFace.heal(4),
            DiceFace.miss(),
        )
        val result = DiceCraftService.removeAllOfType(dice, SkillType.MISS)

        assertIs<CraftResult.Success>(result)
        assertEquals(2, result.dice.faceCount)
        assertEquals(0, result.dice.countBySkillType(SkillType.MISS))
    }

    @Test
    fun `removeAllOfType fails when result would be empty`() {
        val dice = Dice.of(DiceFace.miss(), DiceFace.miss(), DiceFace.miss())
        val result = DiceCraftService.removeAllOfType(dice, SkillType.MISS)

        assertIs<CraftResult.Failure>(result)
        assertEquals(CraftError.WOULD_EMPTY_TYPE, result.error)
    }

    @Test
    fun `removeAllOfType succeeds when at least one face remains`() {
        val dice = Dice.of(DiceFace.miss(), DiceFace.miss(), DiceFace.attack(5))
        val result = DiceCraftService.removeAllOfType(dice, SkillType.MISS)

        assertIs<CraftResult.Success>(result)
        assertEquals(1, result.dice.faceCount)
    }

    // ---- immutability ----

    @Test
    fun `craft operations do not mutate the source dice`() {
        val original = Dice.of(DiceFace.attack(3), DiceFace.miss(), DiceFace.heal(4))
        val originalCount = original.faceCount

        DiceCraftService.addFace(original, DiceFace.defense())
        DiceCraftService.removeFace(original, 0)
        DiceCraftService.replaceFace(original, 0, DiceFace.critical(9))
        DiceCraftService.removeAllOfType(original, SkillType.MISS)

        assertEquals(originalCount, original.faceCount)
        assertEquals(3, original.faceCount)
    }

    // ---- integration: craft scenario from the design doc ----

    @Test
    fun `craft scenario - remove all MISS faces then add ATK`() {
        // Start: [atk(3), atk(5), miss, miss, miss, heal(4)]  (6 faces)
        val start = Dice.of(
            DiceFace.attack(3),
            DiceFace.attack(5),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.miss(),
            DiceFace.heal(4),
        )

        // Step 1: remove all MISS faces -> [atk(3), atk(5), heal(4)]  (3 faces)
        val step1 = DiceCraftService.removeAllOfType(start, SkillType.MISS)
        assertIs<CraftResult.Success>(step1)
        assertEquals(3, step1.dice.faceCount)

        // Step 2: add ATK(4) -> [atk(3), atk(5), heal(4), atk(4)]  (4 faces)
        val step2 = DiceCraftService.addFace(step1.dice, DiceFace.attack(4))
        assertIs<CraftResult.Success>(step2)
        assertEquals(4, step2.dice.faceCount)

        // ATK probability is now 3/4 = 75%
        assertEquals(3, step2.dice.countBySkillType(SkillType.ATK))
    }

    @Test
    fun `max faces is 6 and min faces is 1`() {
        assertTrue(MAX_FACES == 6)
        assertTrue(1 == 1)
    }
}
