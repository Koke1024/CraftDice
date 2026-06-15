package com.koke1024.craftdice.domain.physics

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.FIXED_TIME_STEP
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.MAX_SIMULATION_STEPS
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_HEIGHT
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_WIDTH

/**
 * Orchestrates a full dice roll: throws dice, steps the simulation until
 * all bodies stop (or the step budget is exhausted), and returns results.
 *
 * This is the high-level entry point used by the battle flow.
 */
class DiceRollSimulator(
    private val engine: PhysicsEngine = DicePhysicsEngine(),
    private val fixedTimeStep: Double = FIXED_TIME_STEP,
    private val maxSteps: Int = MAX_SIMULATION_STEPS,
) {

    fun simulate(
        diceSetup: List<DiceSetup>,
        throws: List<ThrowInput>,
    ): DiceRollResult {
        engine.reset()
        placeDice(diceSetup)
        applyThrows(throws)
        runSimulation()
        return engine.getResults()
    }

    fun placeDice(setup: List<DiceSetup>) {
        setup.forEachIndexed { index, entry ->
            engine.addDice(index, entry.dice, entry.position)
        }
    }

    fun applyThrows(throws: List<ThrowInput>) {
        for (input in throws) {
            engine.throwDice(input)
        }
    }

    fun runSimulation() {
        var steps = 0
        while (!engine.isAllStopped() && steps < maxSteps) {
            engine.step(fixedTimeStep)
            steps++
        }
    }

    fun getEngine(): PhysicsEngine = engine

    companion object {
        fun defaultTrayCenter(): Vector2 = Vector2(TRAY_WIDTH / 2.0, TRAY_HEIGHT / 2.0)
    }
}

data class DiceSetup(
    val dice: Dice,
    val position: Vector2,
)
