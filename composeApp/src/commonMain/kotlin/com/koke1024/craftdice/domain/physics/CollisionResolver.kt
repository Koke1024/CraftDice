package com.koke1024.craftdice.domain.physics

import com.koke1024.craftdice.domain.physics.PhysicsConstraints.BUMP_REROLL_VELOCITY
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.DICE_RESTITUTION
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.WALL_RESTITUTION

/**
 * Resolves collisions by applying impulse-based responses.
 *
 * Handles two collision types:
 * - Body-body: dice bounce off each other with [DICE_RESTITUTION], and a
 *   fast-moving dice can trigger a reroll on a stopped one.
 * - Body-wall: dice bounce off tray walls with [WALL_RESTITUTION].
 */
class CollisionResolver(
    private val randomProvider: () -> Double = ::defaultRandom,
) {

    fun resolveBodyCollision(collision: CollisionDetector.BodyCollision) {
        val (bodyA, bodyB, normal, penetration) = collision

        positionalCorrection(bodyA, bodyB, normal, penetration)

        val relativeVelocity = bodyB.velocity - bodyA.velocity
        val velocityAlongNormal = relativeVelocity.dot(normal)

        if (velocityAlongNormal > 0.0) return

        handleBumpReroll(bodyA, bodyB, normal, velocityAlongNormal)

        val impulseMagnitude = -(1.0 + DICE_RESTITUTION) * velocityAlongNormal / 2.0
        val impulse = normal * impulseMagnitude

        bodyA.applyVelocity(bodyA.velocity - impulse)
        bodyB.applyVelocity(bodyB.velocity + impulse)
    }

    fun resolveWallCollision(collision: CollisionDetector.WallCollision) {
        val (body, normal, penetration) = collision

        body.position += normal * penetration

        val velocityAlongNormal = body.velocity.dot(normal)
        if (velocityAlongNormal < 0.0) {
            val reflected =
                body.velocity - normal * (1.0 + WALL_RESTITUTION) * velocityAlongNormal
            body.applyVelocity(reflected)
        }
    }

    private fun handleBumpReroll(
        bodyA: DicePhysicsBody,
        bodyB: DicePhysicsBody,
        normal: Vector2,
        velocityAlongNormal: Double,
    ) {
        val impactSpeed = kotlin.math.abs(velocityAlongNormal)
        if (impactSpeed < BUMP_REROLL_VELOCITY) return

        if (bodyA.isStopped) {
            bodyA.forceReroll(randomProvider)
        }
        if (bodyB.isStopped) {
            bodyB.forceReroll(randomProvider)
        }
    }

    private fun positionalCorrection(
        bodyA: DicePhysicsBody,
        bodyB: DicePhysicsBody,
        normal: Vector2,
        penetration: Double,
    ) {
        val correction = normal * (penetration / 2.0)
        bodyA.position -= correction
        bodyB.position += correction
    }

    companion object {
        private fun defaultRandom(): Double = kotlin.random.Random.nextDouble()
    }
}
