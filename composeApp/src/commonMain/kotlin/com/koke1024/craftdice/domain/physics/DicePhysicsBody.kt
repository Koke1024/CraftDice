package com.koke1024.craftdice.domain.physics

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.DICE_RADIUS
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.FACE_CYCLE_DISTANCE
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.STOP_VELOCITY_THRESHOLD

/**
 * A circular dice body tracked by the physics engine.
 *
 * Each body stores its [Dice] (face configuration) alongside the runtime
 * physics state: position, velocity, cumulative travel distance, and the
 * index of the face currently shown on top.
 *
 * This class is mutable for performance during simulation stepping; the
 * [DicePhysicsEngine] owns the lifecycle and exposes snapshots via
 * [DicePhysicsSnapshot].
 */
class DicePhysicsBody(
    val id: Int,
    val dice: Dice,
    position: Vector2,
    velocity: Vector2 = Vector2.ZERO,
) {
    var position: Vector2 = position
        internal set

    var velocity: Vector2 = velocity
        internal set

    val radius: Double = DICE_RADIUS

    var currentFaceIndex: Int = 0
        internal set

    var travelDistance: Double = 0.0
        internal set

    private var stopped: Boolean = false

    val isStopped: Boolean
        get() = stopped

    val currentFace: DiceFace
        get() = dice.faces[currentFaceIndex.coerceIn(0, dice.faceCount - 1)]

    internal fun applyVelocity(newVelocity: Vector2) {
        velocity = newVelocity
        if (newVelocity.length() > STOP_VELOCITY_THRESHOLD) {
            stopped = false
        }
    }

    internal fun updatePosition(deltaPosition: Vector2) {
        position += deltaPosition
        val distance = deltaPosition.length()
        travelDistance += distance

        val cyclesPassed = (travelDistance / FACE_CYCLE_DISTANCE).toInt()
        val previousCycles = ((travelDistance - distance) / FACE_CYCLE_DISTANCE).toInt()
        if (cyclesPassed > previousCycles && dice.faceCount > 1) {
            val cycleCount = cyclesPassed - previousCycles
            currentFaceIndex = (currentFaceIndex + cycleCount) % dice.faceCount
        }
    }

    internal fun checkStop() {
        if (!stopped && velocity.length() <= STOP_VELOCITY_THRESHOLD) {
            stopped = true
            velocity = Vector2.ZERO
        }
    }

    internal fun forceReroll(randomProvider: () -> Double) {
        if (dice.faceCount > 1) {
            currentFaceIndex = (randomProvider() * dice.faceCount).toInt() % dice.faceCount
        }
    }

    fun snapshot(): DicePhysicsSnapshot =
        DicePhysicsSnapshot(
            id = id,
            position = position,
            radius = radius,
            faceIndex = currentFaceIndex,
            face = currentFace,
            isStopped = isStopped,
        )
}

data class DicePhysicsSnapshot(
    val id: Int,
    val position: Vector2,
    val radius: Double,
    val faceIndex: Int,
    val face: DiceFace,
    val isStopped: Boolean,
)
