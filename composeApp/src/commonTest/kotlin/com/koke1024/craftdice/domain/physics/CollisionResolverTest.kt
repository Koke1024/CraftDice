package com.koke1024.craftdice.domain.physics

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.DICE_RADIUS
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.DICE_RESTITUTION
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.WALL_RESTITUTION
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollisionResolverTest {

    private val resolver = CollisionResolver(randomProvider = { 0.5 })

    private fun makeBody(
        id: Int,
        x: Double,
        y: Double,
        vx: Double = 0.0,
        vy: Double = 0.0,
    ): DicePhysicsBody {
        val dice = Dice(listOf(DiceFace.attack(10)))
        return DicePhysicsBody(id, dice, Vector2(x, y), Vector2(vx, vy))
    }

    @Test
    fun resolveWallCollision_reflectsVelocityWithRestitution() {
        val body = makeBody(0, DICE_RADIUS - 5.0, 100.0, vx = -100.0)
        val collision = CollisionDetector.detectWallCollisions(listOf(body)).first()

        resolver.resolveWallCollision(collision)

        val expectedVx = 100.0 * WALL_RESTITUTION
        assertEquals(expectedVx, body.velocity.x, 1e-9)
    }

    @Test
    fun resolveWallCollision_pushesBodyInsideTray() {
        val body = makeBody(0, DICE_RADIUS - 5.0, 100.0)
        val collision = CollisionDetector.detectWallCollisions(listOf(body)).first()

        resolver.resolveWallCollision(collision)

        assertTrue(body.position.x >= DICE_RADIUS - 0.001)
    }

    @Test
    fun resolveBodyCollision_bouncesBothBodies() {
        val a = makeBody(0, 50.0, 50.0, vx = 100.0)
        val b = makeBody(1, 55.0, 50.0, vx = -50.0)
        val collision = CollisionDetector.detectBodyCollisions(listOf(a, b)).first()

        resolver.resolveBodyCollision(collision)

        assertTrue(abs(a.velocity.x) > 0.0 || abs(b.velocity.x) > 0.0)
    }

    @Test
    fun resolveBodyCollision_separatesOverlappingBodies() {
        val a = makeBody(0, 50.0, 50.0)
        val b = makeBody(1, 55.0, 50.0)
        val initialDistance = Vector2.distance(a.position, b.position)
        val collision = CollisionDetector.detectBodyCollisions(listOf(a, b)).first()

        resolver.resolveBodyCollision(collision)

        val finalDistance = Vector2.distance(a.position, b.position)
        assertTrue(finalDistance >= initialDistance)
    }

    @Test
    fun resolveBodyCollision_headOnCollisionConservesMomentum() {
        val a = makeBody(0, 50.0, 50.0, vx = 100.0)
        val b = makeBody(1, 55.0, 50.0, vx = -100.0)
        val initialTotalMomentum = a.velocity.x + b.velocity.x
        val collision = CollisionDetector.detectBodyCollisions(listOf(a, b)).first()

        resolver.resolveBodyCollision(collision)

        val finalTotalMomentum = a.velocity.x + b.velocity.x
        assertEquals(initialTotalMomentum, finalTotalMomentum, 1e-9)
    }
}
