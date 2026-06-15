package com.koke1024.craftdice.domain.physics

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Immutable 2D vector used by the physics engine.
 *
 * All arithmetic operations return new instances; the original is never mutated.
 */
data class Vector2(
    val x: Double,
    val y: Double,
) {
    operator fun plus(other: Vector2): Vector2 = Vector2(x + other.x, y + other.y)

    operator fun minus(other: Vector2): Vector2 = Vector2(x - other.x, y - other.y)

    operator fun times(scalar: Double): Vector2 = Vector2(x * scalar, y * scalar)

    operator fun div(scalar: Double): Vector2 = Vector2(x / scalar, y / scalar)

    fun dot(other: Vector2): Double = x * other.x + y * other.y

    fun lengthSquared(): Double = x * x + y * y

    fun length(): Double = sqrt(lengthSquared())

    fun normalize(): Vector2 {
        val len = length()
        if (len < EPSILON) return ZERO
        return this / len
    }

    fun rotate(radians: Double): Vector2 =
        Vector2(
            x = x * cos(radians) - y * sin(radians),
            y = x * sin(radians) + y * cos(radians),
        )

    companion object {
        val ZERO = Vector2(0.0, 0.0)
        val UNIT_X = Vector2(1.0, 0.0)
        val UNIT_Y = Vector2(0.0, 1.0)

        private const val EPSILON = 1e-9

        fun fromAngle(radians: Double, magnitude: Double = 1.0): Vector2 =
            Vector2(cos(radians) * magnitude, sin(radians) * magnitude)

        fun distance(a: Vector2, b: Vector2): Double = hypot(a.x - b.x, a.y - b.y)
    }
}
