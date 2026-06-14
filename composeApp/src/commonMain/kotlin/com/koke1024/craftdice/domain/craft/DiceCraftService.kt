package com.koke1024.craftdice.domain.craft

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceConstraints.MAX_FACES
import com.koke1024.craftdice.domain.model.DiceConstraints.MIN_FACES
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.model.SkillType

/**
 * Craft operations for manipulating dice faces.
 *
 * Supports the three core operations described in the design:
 * 1. **Add** — append a new face (up to [MAX_FACES])
 * 2. **Remove** — delete a face by index (down to [MIN_FACES])
 * 3. **Swap** — replace a face at a given index
 *
 * Additionally provides [removeAllOfType] for the use case of stripping
 * all faces of a certain skill type (e.g. "remove all MISS faces to
 * concentrate probability on the remaining faces").
 *
 * All operations are immutable and return [CraftResult].
 */
object DiceCraftService {

    fun addFace(dice: Dice, face: DiceFace): CraftResult {
        if (dice.faceCount >= MAX_FACES) {
            return CraftResult.Failure(CraftError.AT_MAX_FACES)
        }
        return CraftResult.Success(Dice(dice.faces + face))
    }

    fun removeFace(dice: Dice, index: Int): CraftResult {
        if (index !in dice.faces.indices) {
            return CraftResult.Failure(CraftError.INVALID_FACE_INDEX)
        }
        if (dice.faceCount <= MIN_FACES) {
            return CraftResult.Failure(CraftError.AT_MIN_FACES)
        }
        val newFaces = dice.faces.filterIndexed { i, _ -> i != index }
        return CraftResult.Success(Dice(newFaces))
    }

    fun replaceFace(dice: Dice, index: Int, newFace: DiceFace): CraftResult {
        if (index !in dice.faces.indices) {
            return CraftResult.Failure(CraftError.INVALID_FACE_INDEX)
        }
        val newFaces = dice.faces.mapIndexed { i, current ->
            if (i == index) newFace else current
        }
        return CraftResult.Success(Dice(newFaces))
    }

    fun removeAllOfType(dice: Dice, type: SkillType): CraftResult {
        val remaining = dice.faces.filterNot { it.skillType == type }
        if (remaining.isEmpty()) {
            return CraftResult.Failure(CraftError.WOULD_EMPTY_TYPE)
        }
        return CraftResult.Success(Dice(remaining))
    }
}
