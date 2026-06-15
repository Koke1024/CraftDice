package com.koke1024.craftdice.domain.physics

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_HEIGHT
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_WIDTH

/**
 * Pure-Kotlin 2D physics engine for dice simulation.
 *
 * Manages circular dice bodies in a rectangular tray. Replicates the
 * behavior of the handoff prototype (physics.js): exponential friction,
 * elastic wall/dice bounces, stop detection, bump-triggered rerolls,
 * and distance-based face cycling.
 *
 * Construct a fresh instance per roll via [DicePhysicsEngineFactory] or Koin.
 */
class DicePhysicsEngine(
    override val trayWidth: Double = TRAY_WIDTH,
    override val trayHeight: Double = TRAY_HEIGHT,
    private val randomProvider: () -> Double = ::defaultRandom,
) : PhysicsEngine {

    private val bodies = mutableMapOf<Int, DicePhysicsBody>()
    private val collisionResolver = CollisionResolver(randomProvider)
    private val bodyListCache = mutableListOf<DicePhysicsBody>()

    override fun addDice(id: Int, dice: Dice, position: Vector2) {
        val clamped = clampToTray(position, dice)
        val body = DicePhysicsBody(id, dice, clamped)
        body.forceReroll(randomProvider)
        body.checkStop()
        bodies[id] = body
        invalidateCache()
    }

    override fun removeDice(id: Int) {
        bodies.remove(id)
        invalidateCache()
    }

    override fun throwDice(input: ThrowInput) {
        val body = bodies[input.diceId] ?: return
        body.applyVelocity(input.velocity)
    }

    override fun step(deltaTime: Double) {
        val snapshot = getBodyList()

        applyFriction(snapshot, deltaTime)
        updatePositions(snapshot, deltaTime)
        resolveCollisions(snapshot)
        checkStops(snapshot)
    }

    override fun isAllStopped(): Boolean {
        if (bodies.isEmpty()) return true
        return bodies.values.all { it.isStopped }
    }

    override fun getSnapshots(): List<DicePhysicsSnapshot> =
        bodies.values.map { it.snapshot() }

    override fun getResults(): DiceRollResult {
        val entries =
            bodies.values.map { body ->
                DiceRollEntry(
                    diceId = body.id,
                    face = body.currentFace,
                    faceIndex = body.currentFaceIndex,
                    finalPosition = body.position,
                )
            }
        return DiceRollResult(entries)
    }

    override fun reset() {
        bodies.clear()
        invalidateCache()
    }

    override fun getBody(id: Int): DicePhysicsBody? = bodies[id]

    override fun bodyCount(): Int = bodies.size

    private fun applyFriction(bodyList: List<DicePhysicsBody>, deltaTime: Double) {
        for (body in bodyList) {
            if (body.isStopped) continue
            val newVelocity = FrictionModel.applyFriction(body.velocity, deltaTime)
            body.applyVelocity(newVelocity)
        }
    }

    private fun updatePositions(bodyList: List<DicePhysicsBody>, deltaTime: Double) {
        for (body in bodyList) {
            if (body.isStopped) continue
            val delta = body.velocity * deltaTime
            body.updatePosition(delta)
        }
    }

    private fun resolveCollisions(bodyList: List<DicePhysicsBody>) {
        val bodyCollisions = CollisionDetector.detectBodyCollisions(bodyList)
        for (collision in bodyCollisions) {
            collisionResolver.resolveBodyCollision(collision)
        }

        val wallCollisions = CollisionDetector.detectWallCollisions(bodyList)
        for (collision in wallCollisions) {
            collisionResolver.resolveWallCollision(collision)
        }
    }

    private fun checkStops(bodyList: List<DicePhysicsBody>) {
        for (body in bodyList) {
            body.checkStop()
        }
    }

    private fun clampToTray(position: Vector2, dice: Dice): Vector2 {
        val radius = PhysicsConstraints.DICE_RADIUS
        val clampedX = position.x.coerceIn(radius, trayWidth - radius)
        val clampedY = position.y.coerceIn(radius, trayHeight - radius)
        return Vector2(clampedX, clampedY)
    }

    private fun getBodyList(): List<DicePhysicsBody> {
        if (bodyListCache.size != bodies.size) {
            bodyListCache.clear()
            bodyListCache.addAll(bodies.values)
        }
        return bodyListCache
    }

    private fun invalidateCache() {
        bodyListCache.clear()
    }

    companion object {
        private fun defaultRandom(): Double = kotlin.random.Random.nextDouble()
    }
}
