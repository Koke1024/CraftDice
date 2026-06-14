package com.koke1024.craftdice.domain.probability

import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.model.SkillType

/**
 * Probability data for a single face slot within a dice.
 *
 * @param faceIndex position in the dice face list (0-based)
 * @param face      the [DiceFace] at this slot
 * @param probability  raw probability of rolling this face (0.0 - 1.0)
 */
data class FaceProbability(
    val faceIndex: Int,
    val face: DiceFace,
    val probability: Double,
)
