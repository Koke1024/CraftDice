package com.koke1024.craftdice.domain.physics

/**
 * Physics constants derived from the handoff prototype (physics.js).
 *
 * These values define the "feel" of dice rolling in the battle tray.
 * All units are in pixels and seconds.
 */
object PhysicsConstraints {
    const val TRAY_WIDTH = 320.0
    const val TRAY_HEIGHT = 292.0

    const val DICE_RADIUS = 19.0

    const val WALL_RESTITUTION = 0.72
    const val DICE_RESTITUTION = 0.85

    const val FRICTION_DECAY_RATE = 1.6

    const val STOP_VELOCITY_THRESHOLD = 14.0

    const val BUMP_REROLL_VELOCITY = 55.0

    const val FACE_CYCLE_DISTANCE = 26.0

    const val FIXED_TIME_STEP = 1.0 / 60.0

    const val MAX_SIMULATION_STEPS = 600
}
