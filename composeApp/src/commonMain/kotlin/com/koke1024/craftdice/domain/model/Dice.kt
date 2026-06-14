package com.koke1024.craftdice.domain.model

import com.koke1024.craftdice.domain.model.DiceConstraints.MAX_FACES
import com.koke1024.craftdice.domain.model.DiceConstraints.MIN_FACES

/**
 * A craftable dice with a variable number of faces (1-6).
 *
 * In the handoff prototype every monster has exactly 6 faces.
 * The craft system allows adding, removing, and swapping faces to
 * manipulate the probability distribution.
 *
 * This class is immutable; craft operations return new [Dice] instances.
 */
data class Dice(
    val faces: List<DiceFace>,
) {
    val faceCount: Int get() = faces.size

    init {
        require(faces.size >= MIN_FACES) {
            "Dice must have at least $MIN_FACES face(s), got ${faces.size}"
        }
        require(faces.size <= MAX_FACES) {
            "Dice cannot have more than $MAX_FACES faces, got ${faces.size}"
        }
    }

    fun faceAt(index: Int): DiceFace = faces[index]

    fun countBySkillType(type: SkillType): Int =
        faces.count { it.skillType == type }

    fun facesOfSkillType(type: SkillType): List<DiceFace> =
        faces.filter { it.skillType == type }

    companion object {
        fun of(vararg faces: DiceFace): Dice = Dice(faces.toList())

        fun single(face: DiceFace): Dice = Dice(listOf(face))
    }
}
