package com.koke1024.craftdice.domain.physics

import com.koke1024.craftdice.domain.model.Dice

/**
 * Platform-agnostic 2D physics engine for dice simulation.
 *
 * The engine manages dice bodies inside a rectangular tray, steps the
 * simulation forward in fixed time increments, and reports when all dice
 * have come to rest.
 *
 * Implementations must be safe to call from a single thread (the simulation
 * loop). Snapshot reads are safe from any thread once produced.
 */
interface PhysicsEngine {
    val trayWidth: Double
    val trayHeight: Double

    fun addDice(id: Int, dice: Dice, position: Vector2)

    fun removeDice(id: Int)

    fun throwDice(input: ThrowInput)

    fun step(deltaTime: Double)

    fun isAllStopped(): Boolean

    fun getSnapshots(): List<DicePhysicsSnapshot>

    fun getResults(): DiceRollResult

    fun reset()

    fun getBody(id: Int): DicePhysicsBody?

    fun bodyCount(): Int
}
