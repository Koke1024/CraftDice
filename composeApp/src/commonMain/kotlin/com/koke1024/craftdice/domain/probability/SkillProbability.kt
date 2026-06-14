package com.koke1024.craftdice.domain.probability

import com.koke1024.craftdice.domain.model.SkillType

/**
 * Aggregated probability for a [SkillType] across all faces of a dice.
 *
 * @param skillType   the skill category
 * @param probability combined probability of rolling any face of this type
 * @param faceCount   how many faces share this skill type
 */
data class SkillProbability(
    val skillType: SkillType,
    val probability: Double,
    val faceCount: Int,
)
