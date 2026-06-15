package com.koke1024.craftdice.domain.physics

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Vector2Test {

    @Test
    fun plus_addsComponents() {
        val result = Vector2(1.0, 2.0) + Vector2(3.0, 4.0)
        assertEquals(Vector2(4.0, 6.0), result)
    }

    @Test
    fun minus_subtractsComponents() {
        val result = Vector2(5.0, 7.0) - Vector2(2.0, 3.0)
        assertEquals(Vector2(3.0, 4.0), result)
    }

    @Test
    fun times_scalesByScalar() {
        val result = Vector2(2.0, 3.0) * 2.0
        assertEquals(Vector2(4.0, 6.0), result)
    }

    @Test
    fun dot_returnsDotProduct() {
        val result = Vector2(1.0, 2.0).dot(Vector2(3.0, 4.0))
        assertEquals(11.0, result)
    }

    @Test
    fun length_returnsMagnitude() {
        assertEquals(5.0, Vector2(3.0, 4.0).length())
    }

    @Test
    fun normalize_returnsUnitVector() {
        val result = Vector2(3.0, 4.0).normalize()
        assertEquals(1.0, result.length(), 1e-9)
    }

    @Test
    fun normalize_zeroVectorReturnsZero() {
        assertEquals(Vector2.ZERO, Vector2.ZERO.normalize())
    }

    @Test
    fun distance_calculatesBetweenTwoPoints() {
        val dist = Vector2.distance(Vector2(0.0, 0.0), Vector2(3.0, 4.0))
        assertEquals(5.0, dist)
    }

    @Test
    fun rotate_rotatesByAngle() {
        val rotated = Vector2(1.0, 0.0).rotate(kotlin.math.PI / 2.0)
        assertEquals(0.0, rotated.x, 1e-9)
        assertEquals(1.0, rotated.y, 1e-9)
    }

    @Test
    fun fromAngle_createsUnitVectorAtGivenAngle() {
        val v = Vector2.fromAngle(0.0)
        assertTrue(abs(v.x - 1.0) < 1e-9)
        assertTrue(abs(v.y) < 1e-9)
    }
}
