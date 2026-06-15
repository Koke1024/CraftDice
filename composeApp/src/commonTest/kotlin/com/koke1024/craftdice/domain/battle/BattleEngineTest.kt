package com.koke1024.craftdice.domain.battle

import com.koke1024.craftdice.domain.battle.facedamage.FaceDamageSystem
import com.koke1024.craftdice.domain.battle.model.BattleRule
import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleStatus
import com.koke1024.craftdice.domain.battle.model.BattleUnit
import com.koke1024.craftdice.domain.battle.outcome.OutcomeMapper
import com.koke1024.craftdice.domain.battle.resolution.BattleResolver
import com.koke1024.craftdice.domain.battle.resolution.TargetSelector
import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.model.SkillType
import com.koke1024.craftdice.domain.physics.DiceRollEntry
import com.koke1024.craftdice.domain.physics.DiceRollResult
import com.koke1024.craftdice.domain.physics.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleEngineTest {

    private val p1 = BattleSide.PLAYER1
    private val p2 = BattleSide.PLAYER2

    private fun engine(targetId: Int = 2): BattleEngine = BattleEngine(
        outcomeMapper = OutcomeMapper(),
        resolver = BattleResolver(
            faceDamageSystem = FaceDamageSystem(facesPerHit = 0),
            targetSelector = FixedTargetSelector(targetId),
        ),
    )

    private fun sampleDice(): Dice = Dice.of(
        DiceFace.attack(3),
        DiceFace.defense(),
        DiceFace.heal(5),
        DiceFace.miss(),
    )

    private fun unit(id: Int, owner: BattleSide, hp: Int = 20, current: Int = hp): BattleUnit =
        BattleUnit(id, owner, "U$id", sampleDice(), hp, current)

    private fun roll(vararg entries: DiceRollEntry): DiceRollResult = DiceRollResult(entries.toList())

    private fun entry(id: Int, face: DiceFace, index: Int = 0): DiceRollEntry =
        DiceRollEntry(id, face, index, Vector2(10.0, 10.0))

    @Test
    fun setup_initializesStateWithBothSides() {
        val engine = engine()

        engine.setup(listOf(unit(1, p1)), listOf(unit(2, p2)))

        assertEquals(1, engine.state.aliveUnits(p1).size)
        assertEquals(1, engine.state.aliveUnits(p2).size)
        assertEquals(BattleStatus.ONGOING, engine.state.status)
        assertEquals(1, engine.round)
    }

    @Test
    fun throwOrder_alternatesPlayer1ThenPlayer2() {
        val engine = engine()
        engine.setup(listOf(unit(1, p1), unit(3, p1)), listOf(unit(2, p2)))

        val order = engine.throwOrder()

        assertEquals(
            listOf(p1, p2, p1),
            order.map { it.side },
        )
        assertEquals(listOf(1, 2, 3), order.map { it.unitId })
    }

    @Test
    fun resolveRound_appliesDamageAndAdvancesRound() {
        val engine = engine()
        engine.setup(listOf(unit(1, p1)), listOf(unit(2, p2, hp = 100)))

        val result = engine.resolveRoll(
            roll(
                entry(1, DiceFace.attack(10)),
                entry(2, DiceFace.miss()),
            ),
        )

        assertEquals(90, result.state.unitById(2)!!.currentHp)
        assertEquals(2, engine.round)
        assertFalse(engine.isFinished)
    }

    @Test
    fun resolveRound_setsWinStatusWhenEnemyDefeated() {
        val engine = engine()
        engine.setup(listOf(unit(1, p1)), listOf(unit(2, p2, hp = 5)))

        engine.resolveRoll(
            roll(
                entry(1, DiceFace.attack(10)),
                entry(2, DiceFace.miss()),
            ),
        )

        assertEquals(BattleStatus.PLAYER1_WON, engine.state.status)
        assertTrue(engine.isFinished)
    }

    @Test
    fun resolveRound_multipleRoundsProgressUntilVictory() {
        val engine = engine()
        engine.setup(listOf(unit(1, p1)), listOf(unit(2, p2, hp = 30)))

        engine.resolveRoll(roll(entry(1, DiceFace.attack(10)), entry(2, DiceFace.miss())))
        assertEquals(2, engine.round)
        assertEquals(BattleStatus.ONGOING, engine.state.status)

        engine.resolveRoll(roll(entry(1, DiceFace.attack(10)), entry(2, DiceFace.miss())))
        assertEquals(3, engine.round)

        engine.resolveRoll(roll(entry(1, DiceFace.attack(10)), entry(2, DiceFace.miss())))
        assertEquals(BattleStatus.PLAYER1_WON, engine.state.status)
        assertTrue(engine.isFinished)
    }

    @Test
    fun resolveRound_centerRuleAppliesCenterBonusFromPosition() {
        val engine = BattleEngine(
            outcomeMapper = OutcomeMapper(),
            resolver = BattleResolver(
                faceDamageSystem = FaceDamageSystem(facesPerHit = 0),
                targetSelector = FixedTargetSelector(2),
            ),
            initialRule = BattleRule.CENTER,
        )
        engine.setup(listOf(unit(1, p1)), listOf(unit(2, p2, hp = 100)), rule = BattleRule.CENTER)

        val center = Vector2(
            com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_WIDTH / 2.0,
            com.koke1024.craftdice.domain.physics.PhysicsConstraints.TRAY_HEIGHT / 2.0,
        )
        val result = engine.resolveRoll(
            roll(
                DiceRollEntry(1, DiceFace.attack(5), 0, center),
                entry(2, DiceFace.miss()),
            ),
        )

        assertEquals(93, result.state.unitById(2)!!.currentHp)
    }

    private fun BattleEngine.resolveRoll(result: DiceRollResult) = resolveRound(result)

    private class FixedTargetSelector(private val attackTargetId: Int) : TargetSelector {
        override fun selectAttackTarget(
            state: com.koke1024.craftdice.domain.battle.model.BattleState,
            attackerSide: BattleSide,
        ): BattleUnit? = state.allUnits.find { it.id == attackTargetId }

        override fun selectHealTarget(
            state: com.koke1024.craftdice.domain.battle.model.BattleState,
            healer: BattleUnit,
        ): BattleUnit? = state.aliveUnits(healer.owner).minByOrNull { it.hpRatio } ?: healer
    }
}
