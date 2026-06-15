package com.koke1024.craftdice.domain.physics

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.DICE_RADIUS
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_HEIGHT
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_WIDTH
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollisionDetectorTest {

    private fun makeBody(
        id: Int,
        x: Double,
        y: Double,
    ): DicePhysicsBody {
        val dice = Dice(listOf(DiceFace.attack(10)))
        return DicePhysicsBody(id, dice, Vector2(x, y))
    }

    @Test
    fun detectBodyCollisions_findsOverlappingDice() {
        val a = makeBody(0, 50.0, 50.0)
        val b = makeBody(1, 60.0, 50.0)
        val collisions = CollisionDetector.detectBodyCollisions(listOf(a, b))

        assertEquals(1, collisions.size)
        val c = collisions.first()
        assertEquals(a, c.bodyA)
        assertEquals(b, c.bodyB)
    }

    @Test
    fun detectBodyCollisions_returnsEmptyWhenNotTouching() {
        val a = makeBody(0, 50.0, 50.0)
        val b = makeBody(1, 200.0, 200.0)
        val collisions = CollisionDetector.detectBodyCollisions(listOf(a, b))
        assertTrue(collisions.isEmpty())
    }

    @Test
    fun detectBodyCollisions_returnsEmptyWhenExactlyTouching() {
        val a = makeBody(0, 50.0, 50.0)
        val b = makeBody(1, 50.0 + DICE_RADIUS * 2.0, 50.0)
        val collisions = CollisionDetector.detectBodyCollisions(listOf(a, b))
        assertTrue(collisions.isEmpty())
    }

    @Test
    fun detectWallCollisions_detectsLeftWall() {
        val body = makeBody(0, DICE_RADIUS - 5.0, 100.0)
        val collisions = CollisionDetector.detectWallCollisions(listOf(body))
        assertEquals(1, collisions.size)
        assertEquals(Vector2.UNIT_X, collisions.first().normal)
    }

    @Test
    fun detectWallCollisions_detectsRightWall() {
        val body = makeBody(0, TRAY_WIDTH - DICE_RADIUS + 5.0, 100.0)
        val collisions = CollisionDetector.detectWallCollisions(listOf(body))
        assertEquals(1, collisions.size)
    }

    @Test
    fun detectWallCollisions_detectsTopWall() {
        val body = makeBody(0, 100.0, DICE_RADIUS - 5.0)
        val collisions = CollisionDetector.detectWallCollisions(listOf(body))
        assertEquals(1, collisions.size)
        assertEquals(Vector2.UNIT_Y, collisions.first().normal)
    }

    @Test
    fun detectWallCollisions_detectsBottomWall() {
        val body = makeBody(0, 100.0, TRAY_HEIGHT - DICE_RADIUS + 5.0)
        val collisions = CollisionDetector.detectWallCollisions(listOf(body))
        assertEquals(1, collisions.size)
    }

    @Test
    fun detectWallCollisions_returnsEmptyWhenInsideTray() {
        val body = makeBody(0, TRAY_WIDTH / 2.0, TRAY_HEIGHT / 2.0)
        val collisions = CollisionDetector.detectWallCollisions(listOf(body))
        assertTrue(collisions.isEmpty())
    }
}
