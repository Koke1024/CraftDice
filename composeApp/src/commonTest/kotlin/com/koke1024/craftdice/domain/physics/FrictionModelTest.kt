package com.koke1024.craftdice.domain.physics

import com.koke1024.craftdice.domain.physics.PhysicsConstraints.FRICTION_DECAY_RATE
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals

class FrictionModelTest {

    @Test
    fun applyFriction_decaysVelocityExponentially() {
        val velocity = Vector2(100.0, 0.0)
        val dt = 0.5

        val result = FrictionModel.applyFriction(velocity, dt)

        val expected = 100.0 * exp(-FRICTION_DECAY_RATE * dt)
        assertEquals(expected, result.x, 1e-9)
    }

    @Test
    fun applyFriction_preservesDirection() {
        val velocity = Vector2(100.0, 200.0)
        val dt = 1.0

        val result = FrictionModel.applyFriction(velocity, dt)

        val ratio = result.x / result.y
        val originalRatio = velocity.x / velocity.y
        assertEquals(originalRatio, ratio, 1e-9)
    }

    @Test
    fun applyFriction_zeroVelocityStaysZero() {
        val result = FrictionModel.applyFriction(Vector2.ZERO, 1.0)
        assertEquals(Vector2.ZERO, result)
    }

    @Test
    fun applyFriction_customDecayRate() {
        val velocity = Vector2(100.0, 0.0)
        val dt = 1.0
        val customRate = 2.0

        val result = FrictionModel.applyFriction(velocity, dt, customRate)

        val expected = 100.0 * exp(-customRate * dt)
        assertEquals(expected, result.x, 1e-9)
    }
}
