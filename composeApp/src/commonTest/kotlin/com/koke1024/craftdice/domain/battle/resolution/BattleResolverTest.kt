package com.koke1024.craftdice.domain.battle.resolution

import com.koke1024.craftdice.domain.battle.facedamage.FaceDamageSystem
import com.koke1024.craftdice.domain.battle.model.BattleRule
import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleState
import com.koke1024.craftdice.domain.battle.model.BattleStatus
import com.koke1024.craftdice.domain.battle.model.BattleUnit
import com.koke1024.craftdice.domain.battle.outcome.DiceOutcome
import com.koke1024.craftdice.domain.battle.synergy.SynergyDetector
import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.model.SkillType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleResolverTest {

    private val p1 = BattleSide.PLAYER1
    private val p2 = BattleSide.PLAYER2

    private fun noFaceBreakResolver(attackTargetId: Int): BattleResolver = BattleResolver(
        synergyDetector = SynergyDetector(),
        faceDamageSystem = FaceDamageSystem(facesPerHit = 0),
        targetSelector = FixedTargetSelector(attackTargetId),
    )

    @Test
    fun resolve_defenseHalvesIncomingDamage() {
        val state = BattleState(group(unit(1, p1, 20), unit(2, p2, 20)))
        val outcomes = listOf(atkOutcome(1, p1, 10), defOutcome(2, p2))

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        assertEquals(15, result.state.unitById(2)!!.currentHp)
    }

    @Test
    fun resolve_defendsResolveBeforeAttacks() {
        val state = BattleState(group(unit(1, p1, 20), unit(2, p2, 20)))
        val outcomes = listOf(atkOutcome(1, p1, 10), defOutcome(2, p2))

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        assertEquals(StepKind.DEFEND, result.steps.first().kind)
        val defendIdx = result.steps.indexOfFirst { it.kind == StepKind.DEFEND }
        val attackIdx = result.steps.indexOfFirst { it.kind == StepKind.ATTACK }
        assertTrue(defendIdx < attackIdx)
    }

    @Test
    fun resolve_actionsInterleavePlayer1First() {
        val state = BattleState(group(unit(1, p1, 30), unit(3, p1, 30), unit(2, p2, 100)))
        val outcomes = listOf(atkOutcome(1, p1, 5), atkOutcome(3, p1, 5), atkOutcome(2, p2, 5))

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        val attackOwners = result.steps.filter { it.kind == StepKind.ATTACK }.map { it.owner }
        assertEquals(listOf(p1, p2, p1), attackOwners)
    }

    @Test
    fun resolve_nonDefendingTargetTakesFullDamage() {
        val state = BattleState(group(unit(1, p1, 20), unit(2, p2, 20)))
        val outcomes = listOf(atkOutcome(1, p1, 10), missOutcome(2, p2))

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        assertEquals(10, result.state.unitById(2)!!.currentHp)
    }

    @Test
    fun resolve_healTargetsMostDamagedAlly() {
        val state = BattleState(group(unit(1, p1, 20), unit(3, p1, 20, current = 5), unit(2, p2, 20)))
        val outcomes = listOf(healOutcome(1, p1, 6), missOutcome(2, p2))

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        assertEquals(11, result.state.unitById(3)!!.currentHp)
    }

    @Test
    fun resolve_deadActorDoesNotAct() {
        val state = BattleState(group(unit(1, p1, 20, current = 0), unit(2, p2, 20)))
        val outcomes = listOf(atkOutcome(1, p1, 10))

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        assertEquals(20, result.state.unitById(2)!!.currentHp)
        assertTrue(result.steps.none { it.kind == StepKind.ATTACK })
    }

    @Test
    fun resolve_zoromeDoublesDamage() {
        val state = BattleState(group(unit(1, p1, 20), unit(4, p1, 20), unit(2, p2, 100)))
        val outcomes = listOf(atkOutcome(1, p1, 5), atkOutcome(4, p1, 5))

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        val totalDamage = result.steps.filter { it.kind == StepKind.ATTACK }.sumOf { it.damageDealt }
        assertEquals(20, totalDamage)
    }

    @Test
    fun resolve_brokenFaceSelfDamagesActor() {
        val state = BattleState(group(unit(1, p1, 20, broken = setOf(0)), unit(2, p2, 20)))
        val outcomes = listOf(DiceOutcome(1, p1, atkFace(10), 0, isFaceBroken = true, centerBonus = false))

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        assertTrue(result.state.unitById(1)!!.currentHp < 20)
        assertTrue(result.steps.any { it.kind == StepKind.BROKEN_FACE })
    }

    @Test
    fun resolve_faceDamageBreaksFacesOnHit() {
        val state = BattleState(group(unit(1, p1, 30), unit(2, p2, 30)))
        val outcomes = listOf(atkOutcome(1, p1, 10), missOutcome(2, p2))

        val resolver = BattleResolver(
            synergyDetector = SynergyDetector(),
            faceDamageSystem = FaceDamageSystem(facesPerHit = 1, random = { 0.0 }),
            targetSelector = FixedTargetSelector(2),
        )
        val result = resolver.resolve(state, outcomes)

        assertEquals(1, result.state.unitById(2)!!.brokenFaceIndices.size)
    }

    @Test
    fun resolve_eliminatingLastEnemyWinsBattle() {
        val state = BattleState(group(unit(1, p1, 20), unit(2, p2, 5)))
        val outcomes = listOf(atkOutcome(1, p1, 10), missOutcome(2, p2))

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        assertEquals(BattleStatus.PLAYER1_WON, result.state.status)
    }

    @Test
    fun resolve_centerBonusAddsDamageUnderCenterRule() {
        val state = BattleState(group(unit(1, p1, 20), unit(2, p2, 100)), rule = BattleRule.CENTER)
        val outcomes = listOf(
            DiceOutcome(1, p1, atkFace(5), 0, isFaceBroken = false, centerBonus = true),
            missOutcome(2, p2),
        )

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        assertEquals(7, result.steps.single { it.kind == StepKind.ATTACK }.damageDealt)
    }

    @Test
    fun resolve_centerBonusIgnoredUnderBumpRule() {
        val state = BattleState(group(unit(1, p1, 20), unit(2, p2, 100)), rule = BattleRule.BUMP)
        val outcomes = listOf(
            DiceOutcome(1, p1, atkFace(5), 0, isFaceBroken = false, centerBonus = true),
            missOutcome(2, p2),
        )

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        assertEquals(5, result.steps.single { it.kind == StepKind.ATTACK }.damageDealt)
    }

    @Test
    fun resolve_straightBonusDealsExtraDamage() {
        val state = BattleState(group(unit(1, p1, 30), unit(4, p1, 30), unit(5, p1, 30), unit(2, p2, 100)))
        val outcomes = listOf(atkOutcome(1, p1, 3), atkOutcome(4, p1, 4), atkOutcome(5, p1, 5))

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        assertTrue(result.steps.any { it.kind == StepKind.SYNERGY_BONUS })
    }

    @Test
    fun resolve_clearsDefendingAfterRound() {
        val state = BattleState(group(unit(1, p1, 20), unit(2, p2, 20)))
        val outcomes = listOf(atkOutcome(1, p1, 5), defOutcome(2, p2))

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        assertFalse(result.state.unitById(2)!!.isDefending)
    }

    @Test
    fun resolve_critUsesCritStepKind() {
        val state = BattleState(group(unit(1, p1, 20), unit(2, p2, 100)))
        val outcomes = listOf(critOutcome(1, p1, 15), missOutcome(2, p2))

        val result = noFaceBreakResolver(2).resolve(state, outcomes)

        assertTrue(result.steps.any { it.kind == StepKind.CRIT })
    }

    private class FixedTargetSelector(private val attackTargetId: Int) : TargetSelector {
        override fun selectAttackTarget(state: BattleState, attackerSide: BattleSide): BattleUnit? =
            state.allUnits.find { it.id == attackTargetId }

        override fun selectHealTarget(state: BattleState, healer: BattleUnit): BattleUnit? =
            state.aliveUnits(healer.owner).minByOrNull { it.hpRatio } ?: healer
    }

    private fun group(vararg units: BattleUnit): Map<BattleSide, List<BattleUnit>> =
        units.toList().groupBy { it.owner }

    private fun unit(
        id: Int,
        owner: BattleSide,
        hp: Int,
        current: Int = hp,
        broken: Set<Int> = emptySet(),
    ): BattleUnit {
        val dice = Dice.of(
            DiceFace.attack(1),
            DiceFace.attack(2),
            DiceFace.heal(3),
            DiceFace.defense(),
        )
        return BattleUnit(id, owner, "U$id", dice, hp, current, broken)
    }

    private fun atkFace(value: Int) = DiceFace(SkillType.ATK, value)

    private fun atkOutcome(id: Int, owner: BattleSide, value: Int) =
        DiceOutcome(id, owner, atkFace(value), 0, false, false)

    private fun critOutcome(id: Int, owner: BattleSide, value: Int) =
        DiceOutcome(id, owner, DiceFace(SkillType.CRIT, value), 0, false, false)

    private fun healOutcome(id: Int, owner: BattleSide, value: Int) =
        DiceOutcome(id, owner, DiceFace(SkillType.HEAL, value), 2, false, false)

    private fun missOutcome(id: Int, owner: BattleSide) =
        DiceOutcome(id, owner, DiceFace.miss(), 3, false, false)

    private fun defOutcome(id: Int, owner: BattleSide) =
        DiceOutcome(id, owner, DiceFace.defense(), 3, false, false)
}
