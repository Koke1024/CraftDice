package com.koke1024.craftdice.domain.physics

import com.koke1024.craftdice.domain.physics.PhysicsConstraints.FRICTION_DECAY_RATE

/**
 * Applies exponential velocity decay to simulate surface friction.
 *
 * Following the handoff prototype: `velocity *= exp(-rate * dt)`.
 */
object FrictionModel {

    fun applyFriction(
        velocity: Vector2,
        deltaTime: Double,
        decayRate: Double = FRICTION_DECAY_RATE,
    ): Vector2 {
        val factor = kotlin.math.exp(-decayRate * deltaTime)
        return velocity * factor
    }
}
