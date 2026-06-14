package com.koke1024.craftdice.domain.model

/**
 * Physical and logical constraints for a craftable dice.
 */
object DiceConstraints {
    const val MIN_FACES = 1
    const val MAX_FACES = 6
    const val DEFAULT_FACE_COUNT = 6

    const val NO_VALUE = 0
    const val MIN_FACE_VALUE = 1
    const val MAX_FACE_VALUE = 99
}
