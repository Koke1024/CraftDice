package com.koke1024.craftdice.domain.battle.outcome

import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleState
import com.koke1024.craftdice.domain.battle.model.BattleUnit
import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.physics.DiceRollResult
import com.koke1024.craftdice.domain.physics.DiceRollEntry
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_HEIGHT
import com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_WIDTH
import com.koke1024.craftdice.domain.physics.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OutcomeMapperTest {

    private val mapper = OutcomeMapper()

    private fun stateWith(vararg units: BattleUnit): BattleState =
        BattleState(units.groupBy { it.owner })

    private fun unit(id: Int, owner: BattleSide, broken: Set<Int> = emptySet()): BattleUnit {
        val dice = Dice.of(
            DiceFace.attack(3),
            DiceFace.defense(),
            DiceFace.heal(5),
            DiceFace.miss(),
        )
        return BattleUnit(id, owner, "U$id", dice, 20, 20, broken)
    }

    private fun rollResult(vararg entries: DiceRollEntry): DiceRollResult = DiceRollResult(entries.toList())

    @Test
    fun map_attachesOwnerAndFaceFromRollResult() {
        val state = stateWith(unit(0, BattleSide.PLAYER1), unit(1, BattleSide.PLAYER2))
        val result = rollResult(
            DiceRollEntry(0, DiceFace.attack(3), 0, Vector2(10.0, 10.0)),
            DiceRollEntry(1, DiceFace.heal(5), 2, Vector2(10.0, 10.0)),
        )

        val outcomes = mapper.map(result, state)

        assertEquals(2, outcomes.size)
        assertEquals(BattleSide.PLAYER1, outcomes[0].owner)
        assertEquals(BattleSide.PLAYER2, outcomes[1].owner)
        assertEquals(0, outcomes[0].faceIndex)
        assertEquals(2, outcomes[1].faceIndex)
    }

    @Test
    fun map_flagsBrokenFaceWhenUnitHasBrokenIndex() {
        val state = stateWith(unit(0, BattleSide.PLAYER1, broken = setOf(1)))
        val result = rollResult(
            DiceRollEntry(0, DiceFace.defense(), 1, Vector2(10.0, 10.0)),
        )

        val outcome = mapper.map(result, state).single()

        assertTrue(outcome.isFaceBroken)
    }

    @Test
    fun map_intactFaceIsNotFlaggedBroken() {
        val state = stateWith(unit(0, BattleSide.PLAYER1, broken = setOf(1)))
        val result = rollResult(
            DiceRollEntry(0, DiceFace.attack(3), 0, Vector2(10.0, 10.0)),
        )

        val outcome = mapper.map(result, state).single()

        assertFalse(outcome.isFaceBroken)
    }

    @Test
    fun map_centerBonusTrueWhenLandedInCenterZone() {
        val centerMapper = OutcomeMapper()
        val state = stateWith(unit(0, BattleSide.PLAYER1))
        val center = Vector2(TRAY_WIDTH / 2.0, TRAY_HEIGHT / 2.0)
        val result = rollResult(DiceRollEntry(0, DiceFace.attack(3), 0, center))

        val outcome = centerMapper.map(result, state).single()

        assertTrue(outcome.centerBonus)
    }

    @Test
    fun map_centerBonusFalseWhenLandedInCorner() {
        val state = stateWith(unit(0, BattleSide.PLAYER1))
        val result = rollResult(DiceRollEntry(0, DiceFace.attack(3), 0, Vector2(2.0, 2.0)))

        val outcome = mapper.map(result, state).single()

        assertFalse(outcome.centerBonus)
    }

    @Test
    fun map_unknownDiceIdDefaultsToPlayer1Owner() {
        val state = stateWith(unit(0, BattleSide.PLAYER1))
        val result = rollResult(DiceRollEntry(99, DiceFace.attack(3), 0, Vector2(10.0, 10.0)))

        val outcome = mapper.map(result, state).single()

        assertEquals(BattleSide.PLAYER1, outcome.owner)
        assertTrue(outcome.isFaceBroken)
    }
}
