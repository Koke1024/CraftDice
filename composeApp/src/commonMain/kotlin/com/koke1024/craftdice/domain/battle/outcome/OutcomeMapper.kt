package com.koke1024.craftdice.domain.battle.outcome

import com.koke1024.craftdice.domain.battle.model.BattleConfig
import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleState
import com.koke1024.craftdice.domain.physics.DiceRollResult
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_HEIGHT
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_WIDTH
import com.koke1024.craftdice.domain.physics.Vector2
import kotlin.math.abs

/**
 * Maps a Phase 2 [DiceRollResult] into a list of [DiceOutcome]s enriched with
 * battle context (owning unit, broken-face status, center bonus).
 *
 * The center bonus zone is a square region centered in the tray whose size is
 * governed by [BattleConfig.CENTER_ZONE_RATIO].
 */
class OutcomeMapper(
    private val trayWidth: Double = TRAY_WIDTH,
    private val trayHeight: Double = TRAY_HEIGHT,
    private val centerZoneRatio: Double = BattleConfig.CENTER_ZONE_RATIO,
) {
    fun map(rollResult: DiceRollResult, state: BattleState): List<DiceOutcome> =
        rollResult.results.map { entry ->
            val unit = state.unitById(entry.diceId)
            DiceOutcome(
                unitId = entry.diceId,
                owner = unit?.owner ?: BattleSide.PLAYER1,
                face = entry.face,
                faceIndex = entry.faceIndex,
                isFaceBroken = unit?.isFaceBroken(entry.faceIndex) ?: true,
                centerBonus = isInCenterZone(entry.finalPosition),
            )
        }

    private fun isInCenterZone(position: Vector2): Boolean {
        val halfW = trayWidth * centerZoneRatio / 2.0
        val halfH = trayHeight * centerZoneRatio / 2.0
        val dx = abs(position.x - trayWidth / 2.0)
        val dy = abs(position.y - trayHeight / 2.0)
        return dx <= halfW && dy <= halfH
    }
}
