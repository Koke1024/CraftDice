package com.koke1024.craftdice.domain.model

import com.koke1024.craftdice.domain.model.DiceConstraints.MAX_FACE_VALUE
import com.koke1024.craftdice.domain.model.DiceConstraints.MIN_FACE_VALUE
import com.koke1024.craftdice.domain.model.DiceConstraints.NO_VALUE

/**
 * A single face of a dice.
 *
 * Follows the handoff prototype (game-data.js) structure:
 * - [skillType] determines the action when this face is rolled.
 * - [value] is the magnitude for ATK/HEAL/CRIT faces; always 0 for DEF/MISS.
 * - [rarity] is the quality tier used by the craft/reward system.
 */
data class DiceFace(
    val skillType: SkillType,
    val value: Int = NO_VALUE,
    val rarity: Rarity = Rarity.COMMON,
) {
    init {
        if (skillType.hasValue) {
            require(value in MIN_FACE_VALUE..MAX_FACE_VALUE) {
                "${skillType.displayName} face value must be in $MIN_FACE_VALUE..$MAX_FACE_VALUE, got $value"
            }
        } else {
            require(value == NO_VALUE) {
                "${skillType.displayName} face must have value $NO_VALUE, got $value"
            }
        }
    }

    companion object {
        fun attack(value: Int, rarity: Rarity = Rarity.COMMON): DiceFace =
            DiceFace(SkillType.ATK, value, rarity)

        fun defense(rarity: Rarity = Rarity.COMMON): DiceFace =
            DiceFace(SkillType.DEF, NO_VALUE, rarity)

        fun heal(value: Int, rarity: Rarity = Rarity.COMMON): DiceFace =
            DiceFace(SkillType.HEAL, value, rarity)

        fun critical(value: Int, rarity: Rarity = Rarity.COMMON): DiceFace =
            DiceFace(SkillType.CRIT, value, rarity)

        fun miss(rarity: Rarity = Rarity.COMMON): DiceFace =
            DiceFace(SkillType.MISS, NO_VALUE, rarity)
    }
}
