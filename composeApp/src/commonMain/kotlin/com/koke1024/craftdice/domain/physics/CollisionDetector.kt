package com.koke1024.craftdice.domain.physics

import com.koke1024.craftdice.domain.physics.PhysicsConstraints.DICE_RADIUS
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_HEIGHT
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_WIDTH

/**
 * Detects collisions between dice bodies and between bodies and tray walls.
 *
 * This class is stateless; all methods are pure functions of the input bodies.
 */
object CollisionDetector {

    data class BodyCollision(
        val bodyA: DicePhysicsBody,
        val bodyB: DicePhysicsBody,
        val normal: Vector2,
        val penetrationDepth: Double,
    )

    data class WallCollision(
        val body: DicePhysicsBody,
        val normal: Vector2,
        val penetrationDepth: Double,
    )

    fun detectBodyCollisions(bodies: List<DicePhysicsBody>): List<BodyCollision> {
        val collisions = mutableListOf<BodyCollision>()
        for (i in bodies.indices) {
            for (j in (i + 1) until bodies.size) {
                val a = bodies[i]
                val b = bodies[j]
                val collision = checkBodyPair(a, b)
                if (collision != null) {
                    collisions.add(collision)
                }
            }
        }
        return collisions
    }

    fun detectWallCollisions(bodies: List<DicePhysicsBody>): List<WallCollision> {
        val collisions = mutableListOf<WallCollision>()
        for (body in bodies) {
            checkWalls(body)?.let { collisions.add(it) }
        }
        return collisions
    }

    private fun checkBodyPair(
        a: DicePhysicsBody,
        b: DicePhysicsBody,
    ): BodyCollision? {
        val delta = b.position - a.position
        val distance = delta.length()
        val minDistance = a.radius + b.radius

        if (distance >= minDistance || distance < EPSILON) return null

        val normal = delta.normalize()
        val penetration = minDistance - distance
        return BodyCollision(a, b, normal, penetration)
    }

    private fun checkWalls(body: DicePhysicsBody): WallCollision? {
        val r = body.radius
        val pos = body.position

        if (pos.x - r < 0.0) {
            return WallCollision(body, Vector2.UNIT_X, r - pos.x)
        }
        if (pos.x + r > TRAY_WIDTH) {
            return WallCollision(body, Vector2(-1.0, 0.0), pos.x + r - TRAY_WIDTH)
        }
        if (pos.y - r < 0.0) {
            return WallCollision(body, Vector2.UNIT_Y, r - pos.y)
        }
        if (pos.y + r > TRAY_HEIGHT) {
            return WallCollision(body, Vector2(0.0, -1.0), pos.y + r - TRAY_HEIGHT)
        }
        return null
    }

    private const val EPSILON = 1e-9
}
