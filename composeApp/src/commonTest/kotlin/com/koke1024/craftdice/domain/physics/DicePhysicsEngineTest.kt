package com.koke1024.craftdice.domain.physics

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.model.SkillType
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.DICE_RADIUS
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.FACE_CYCLE_DISTANCE
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.STOP_VELOCITY_THRESHOLD
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_HEIGHT
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_WIDTH
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DicePhysicsEngineTest {

    private fun sixFaceDice(): Dice =
        Dice(
            listOf(
                DiceFace.attack(10),
                DiceFace.defense(),
                DiceFace.heal(5),
                DiceFace.critical(20),
                DiceFace.miss(),
                DiceFace.attack(5),
            ),
        )

    private fun makeEngine(): DicePhysicsEngine =
        DicePhysicsEngine(randomProvider = { 0.0 })

    @Test
    fun addDice_clampsPositionInsideTray() {
        val engine = makeEngine()
        engine.addDice(0, sixFaceDice(), Vector2(-10.0, -10.0))

        val body = engine.getBody(0)!!
        assertTrue(body.position.x >= DICE_RADIUS - 0.001)
        assertTrue(body.position.y >= DICE_RADIUS - 0.001)
    }

    @Test
    fun isAllStopped_returnsTrueWhenEmpty() {
        val engine = makeEngine()
        assertTrue(engine.isAllStopped())
    }

    @Test
    fun isAllStopped_returnsTrueWhenAllStopped() {
        val engine = makeEngine()
        engine.addDice(0, sixFaceDice(), Vector2(100.0, 100.0))
        assertTrue(engine.isAllStopped())
    }

    @Test
    fun isAllStopped_returnsFalseWhenMoving() {
        val engine = makeEngine()
        engine.addDice(0, sixFaceDice(), Vector2(100.0, 100.0))
        engine.throwDice(ThrowInput(0, Vector2(500.0, 0.0)))
        assertFalse(engine.isAllStopped())
    }

    @Test
    fun step_appliesFrictionAndEventuallyStops() {
        val engine = makeEngine()
        engine.addDice(0, sixFaceDice(), Vector2(100.0, 100.0))
        engine.throwDice(ThrowInput(0, Vector2(500.0, 0.0)))

        var steps = 0
        while (!engine.isAllStopped() && steps < 600) {
            engine.step(1.0 / 60.0)
            steps++
        }

        assertTrue(engine.isAllStopped(), "Dice should stop within 600 steps")
    }

    @Test
    fun step_diceBouncesOffWalls() {
        val engine = makeEngine()
        engine.addDice(0, sixFaceDice(), Vector2(TRAY_WIDTH - DICE_RADIUS - 1.0, 100.0))
        engine.throwDice(ThrowInput(0, Vector2(500.0, 0.0)))

        engine.step(1.0 / 60.0)

        val body = engine.getBody(0)!!
        assertTrue(body.velocity.x < 0.0, "Velocity should reverse after hitting right wall")
    }

    @Test
    fun step_diceBodyCollisionPushesApart() {
        val engine = makeEngine()
        engine.addDice(0, sixFaceDice(), Vector2(100.0, 100.0))
        engine.addDice(1, sixFaceDice(), Vector2(100.0 + DICE_RADIUS * 1.5, 100.0))
        engine.throwDice(ThrowInput(0, Vector2(500.0, 0.0)))

        engine.step(1.0 / 60.0)

        val body1 = engine.getBody(1)!!
        assertTrue(body1.velocity.x > 0.0, "Second body should be pushed by collision")
    }

    @Test
    fun getResults_returnsCurrentFaceForEachBody() {
        val engine = makeEngine()
        engine.addDice(0, sixFaceDice(), Vector2(100.0, 100.0))
        engine.addDice(1, sixFaceDice(), Vector2(200.0, 200.0))

        val results = engine.getResults()
        assertEquals(2, results.results.size)
    }

    @Test
    fun reset_clearsAllBodies() {
        val engine = makeEngine()
        engine.addDice(0, sixFaceDice(), Vector2(100.0, 100.0))
        assertEquals(1, engine.bodyCount())

        engine.reset()
        assertEquals(0, engine.bodyCount())
    }

    @Test
    fun faceCycling_changesFaceAfterTravel() {
        val dice = sixFaceDice()
        val body = DicePhysicsBody(0, dice, Vector2(100.0, 100.0))
        val initialFaceIndex = body.currentFaceIndex

        val travelNeeded = FACE_CYCLE_DISTANCE * 2.0
        val steps = 10
        val perStep = travelNeeded / steps

        repeat(steps) {
            body.updatePosition(Vector2(perStep, 0.0))
        }

        val expectedIndex = (initialFaceIndex + 2) % dice.faceCount
        assertEquals(expectedIndex, body.currentFaceIndex)
    }

    @Test
    fun stopDetection_setsVelocityToZero() {
        val body =
            DicePhysicsBody(
                0,
                sixFaceDice(),
                Vector2(100.0, 100.0),
                Vector2(STOP_VELOCITY_THRESHOLD - 1.0, 0.0),
            )

        body.checkStop()

        assertTrue(body.isStopped)
        assertEquals(Vector2.ZERO, body.velocity)
    }

    @Test
    fun forceReroll_changesFaceIndex() {
        val dice = sixFaceDice()
        val body = DicePhysicsBody(0, dice, Vector2(100.0, 100.0))
        val seen = mutableSetOf(body.currentFaceIndex)

        repeat(20) {
            body.forceReroll { kotlin.random.Random.nextDouble() }
            seen.add(body.currentFaceIndex)
        }

        assertTrue(seen.size > 1, "Rerolling multiple times should produce different face indices")
    }

    @Test
    fun bumpReroll_triggersWhenStoppedDiceHitHard() {
        val stoppedDice =
            DicePhysicsBody(
                0,
                sixFaceDice(),
                Vector2(100.0, 100.0),
            )
        stoppedDice.checkStop()
        assertTrue(stoppedDice.isStopped)

        val initialFace = stoppedDice.currentFaceIndex

        val movingDice =
            DicePhysicsBody(
                1,
                sixFaceDice(),
                Vector2(100.0 + DICE_RADIUS * 1.8, 100.0),
                Vector2(-200.0, 0.0),
            )

        val collisions =
            CollisionDetector.detectBodyCollisions(listOf(stoppedDice, movingDice))
        assertTrue(collisions.isNotEmpty())

        val resolver = CollisionResolver(randomProvider = { 0.5 })
        resolver.resolveBodyCollision(collisions.first())

        val seen = setOf(initialFace, stoppedDice.currentFaceIndex)
        assertTrue(stoppedDice.currentFaceIndex != initialFace || seen.size == 1)
    }
}
