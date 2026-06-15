package com.koke1024.craftdice.domain.physics

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_HEIGHT
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_WIDTH
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiceRollSimulatorTest {

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

    @Test
    fun simulate_singleDiceStopsAndProducesResult() {
        val engine = DicePhysicsEngine(randomProvider = { 0.0 })
        val simulator = DiceRollSimulator(engine)

        val result =
            simulator.simulate(
                diceSetup =
                    listOf(
                        DiceSetup(sixFaceDice(), Vector2(TRAY_WIDTH / 2.0, TRAY_HEIGHT / 2.0)),
                    ),
                throws = listOf(ThrowInput(0, Vector2(400.0, 200.0))),
            )

        assertEquals(1, result.results.size)
        assertTrue(result.isComplete)
        assertTrue(engine.isAllStopped())
    }

    @Test
    fun simulate_multipleDiceAllStop() {
        val engine = DicePhysicsEngine(randomProvider = { 0.0 })
        val simulator = DiceRollSimulator(engine)

        val result =
            simulator.simulate(
                diceSetup =
                    listOf(
                        DiceSetup(sixFaceDice(), Vector2(80.0, 100.0)),
                        DiceSetup(sixFaceDice(), Vector2(160.0, 150.0)),
                        DiceSetup(sixFaceDice(), Vector2(240.0, 100.0)),
                    ),
                throws =
                    listOf(
                        ThrowInput(0, Vector2(300.0, 100.0)),
                        ThrowInput(1, Vector2(-200.0, 150.0)),
                        ThrowInput(2, Vector2(-100.0, -200.0)),
                    ),
            )

        assertEquals(3, result.results.size)
        assertTrue(engine.isAllStopped())
    }

    @Test
    fun simulate_diceStayInsideTray() {
        val engine = DicePhysicsEngine(randomProvider = { 0.0 })
        val simulator = DiceRollSimulator(engine)

        val result =
            simulator.simulate(
                diceSetup =
                    listOf(
                        DiceSetup(sixFaceDice(), Vector2(50.0, 50.0)),
                    ),
                throws = listOf(ThrowInput(0, Vector2(10000.0, 10000.0))),
            )

        val entry = result.results.first()
        assertTrue(entry.finalPosition.x >= 0.0)
        assertTrue(entry.finalPosition.x <= TRAY_WIDTH)
        assertTrue(entry.finalPosition.y >= 0.0)
        assertTrue(entry.finalPosition.y <= TRAY_HEIGHT)
    }

    @Test
    fun defaultTrayCenter_returnsCenterOfTray() {
        val center = DiceRollSimulator.defaultTrayCenter()
        assertEquals(TRAY_WIDTH / 2.0, center.x)
        assertEquals(TRAY_HEIGHT / 2.0, center.y)
    }
}
