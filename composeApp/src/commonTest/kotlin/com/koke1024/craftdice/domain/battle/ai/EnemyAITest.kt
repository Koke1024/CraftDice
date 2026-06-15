package com.koke1024.craftdice.domain.battle.ai

import com.koke1024.craftdice.domain.battle.model.BattleRule
import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleState
import com.koke1024.craftdice.domain.battle.model.BattleUnit
import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_HEIGHT
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_WIDTH
import com.koke1024.craftdice.domain.physics.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnemyAITest {

    private val enemyDice: Dice = Dice.of(
        DiceFace.attack(3),
        DiceFace.attack(3),
        DiceFace.heal(4),
        DiceFace.defense(),
    )

    private fun stateWithEnemies(count: Int): BattleState {
        val enemies = (1..count).map { BattleUnit.fromDice(it, BattleSide.PLAYER2, "E$it", enemyDice, 20) }
        val player = BattleUnit.fromDice(100, BattleSide.PLAYER1, "P", enemyDice, 20)
        return BattleState(
            mapOf(BattleSide.PLAYER1 to listOf(player), BattleSide.PLAYER2 to enemies),
        )
    }

    @Test
    fun planThrows_returnsOnePlanPerAliveEnemy() {
        val ai = EnemyAI(random = { 0.5 })
        val state = stateWithEnemies(3)

        val plans = ai.planThrows(state, BattleRule.BUMP)

        assertEquals(3, plans.size)
        assertEquals(listOf(1, 2, 3), plans.map { it.unitId })
    }

    @Test
    fun planThrows_emptyWhenNoAliveEnemies() {
        val ai = EnemyAI(random = { 0.5 })
        val dead = BattleUnit(1, BattleSide.PLAYER2, "E", enemyDice, 20, 0)
        val state = BattleState(
            mapOf(
                BattleSide.PLAYER1 to listOf(BattleUnit.fromDice(100, BattleSide.PLAYER1, "P", enemyDice, 20)),
                BattleSide.PLAYER2 to listOf(dead),
            ),
        )

        val plans = ai.planThrows(state, BattleRule.BUMP)

        assertTrue(plans.isEmpty())
    }

    @Test
    fun planSingleThrow_bumpAimsTowardHighValueTarget() {
        val targetPos = Vector2(TRAY_WIDTH * 0.8, TRAY_HEIGHT * 0.8)
        val targets = listOf(PlayerDiceInfo(unitId = 100, position = targetPos, faceValue = 9))
        val ai = EnemyAI(bumpChance = 1.0, bumpValueThreshold = 4, random = { 0.5 })

        val plan = ai.planSingleThrow(unitId = 1, rule = BattleRule.BUMP, playerTargets = targets)

        val toTarget = targetPos - plan.origin
        val dot = plan.velocity.x * toTarget.x + plan.velocity.y * toTarget.y
        assertTrue(dot > 0.0, "velocity should point toward the target")
    }

    @Test
    fun planSingleThrow_centerAimsNearCenter() {
        val ai = EnemyAI(random = { 0.5 })

        val plan = ai.planSingleThrow(unitId = 1, rule = BattleRule.CENTER, playerTargets = emptyList())

        val center = Vector2(TRAY_WIDTH / 2.0, TRAY_HEIGHT / 2.0)
        val toCenter = center - plan.origin
        val dot = plan.velocity.x * toCenter.x + plan.velocity.y * toCenter.y
        assertTrue(dot > 0.0, "velocity should point toward the tray center")
    }

    @Test
    fun planSingleThrow_speedStaysWithinConfiguredRange() {
        val ai = EnemyAI(minSpeed = 380.0, maxSpeed = 600.0, random = { 0.0 })

        val plan = ai.planSingleThrow(unitId = 1, rule = BattleRule.NO_BUMP, playerTargets = emptyList())

        val speed = plan.velocity.length()
        assertTrue(speed in 380.0..600.0)
    }

    @Test
    fun planSingleThrow_isDeterministicWithFixedRandom() {
        val ai = EnemyAI(random = { 0.3 })

        val a = ai.planSingleThrow(1, BattleRule.BUMP, emptyList())
        val b = ai.planSingleThrow(1, BattleRule.BUMP, emptyList())

        assertEquals(a.origin, b.origin)
        assertEquals(a.velocity, b.velocity)
    }
}
