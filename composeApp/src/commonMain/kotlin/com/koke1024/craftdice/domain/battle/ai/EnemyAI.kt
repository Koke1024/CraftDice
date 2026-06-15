package com.koke1024.craftdice.domain.battle.ai

import com.koke1024.craftdice.domain.battle.model.BattleRule
import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleState
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_HEIGHT
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_WIDTH
import com.koke1024.craftdice.domain.physics.Vector2
import kotlin.math.hypot

/**
 * Enemy (CPU) AI for planning dice throws.
 *
 * Mirrors the handoff prototype's [cpuPlanThrow]:
 * - [BattleRule.BUMP]: aim at the player's highest-value settled dice to knock
 *   it off its face (the core "bump" mechanic).
 * - [BattleRule.CENTER]: aim for the central bonus zone.
 * - Otherwise: aim at a random point in the tray.
 *
 * Produces one [ThrowPlan] per alive enemy unit. Inject a fixed [random]
 * provider for deterministic tests.
 */
class EnemyAI(
    private val trayWidth: Double = TRAY_WIDTH,
    private val trayHeight: Double = TRAY_HEIGHT,
    private val minSpeed: Double = 380.0,
    private val maxSpeed: Double = 600.0,
    private val bumpChance: Double = 0.75,
    private val bumpValueThreshold: Int = 4,
    private val random: () -> Double = { kotlin.random.Random.nextDouble() },
) {
    fun planThrows(
        state: BattleState,
        rule: BattleRule,
        playerTargets: List<PlayerDiceInfo> = emptyList(),
    ): List<ThrowPlan> =
        state.aliveUnits(BattleSide.PLAYER2).map { unit ->
            planSingleThrow(unit.id, rule, playerTargets)
        }

    fun planSingleThrow(
        unitId: Int,
        rule: BattleRule,
        playerTargets: List<PlayerDiceInfo>,
    ): ThrowPlan {
        val origin = randomOrigin()
        val aim = chooseAim(rule, playerTargets)
        val velocity = velocityToward(origin, aim)
        return ThrowPlan(unitId, origin, velocity)
    }

    private fun chooseAim(rule: BattleRule, targets: List<PlayerDiceInfo>): Vector2 =
        when {
            rule == BattleRule.BUMP && targets.isNotEmpty() && random() < bumpChance -> {
                val best = targets.maxByOrNull { it.faceValue } ?: targets.first()
                if (best.faceValue >= bumpValueThreshold) best.position else randomAim()
            }
            rule == BattleRule.CENTER -> centerAim()
            else -> randomAim()
        }

    private fun randomOrigin(): Vector2 {
        val x = trayWidth * (0.25 + random() * 0.5)
        return Vector2(x, originY())
    }

    private fun originY(): Double = -DICE_DROP_OFFSET

    private fun randomAim(): Vector2 =
        Vector2(
            x = trayWidth * (0.3 + random() * 0.4),
            y = trayHeight * (0.35 + random() * 0.3),
        )

    private fun centerAim(): Vector2 =
        Vector2(
            x = trayWidth / 2.0 + (random() - 0.5) * CENTER_JITTER,
            y = trayHeight / 2.0 + (random() - 0.5) * CENTER_JITTER,
        )

    private fun velocityToward(origin: Vector2, aim: Vector2): Vector2 {
        val dx = aim.x - origin.x
        val dy = aim.y - origin.y
        val len = hypot(dx, dy).coerceAtLeast(MIN_VECTOR_LENGTH)
        val speed = minSpeed + random() * (maxSpeed - minSpeed)
        return Vector2(dx / len * speed, dy / len * speed)
    }

    private companion object {
        const val DICE_DROP_OFFSET = 20.0
        const val CENTER_JITTER = 30.0
        const val MIN_VECTOR_LENGTH = 1.0
    }
}
