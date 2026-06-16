package com.koke1024.craftdice.domain.roguelike

import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleState
import com.koke1024.craftdice.domain.battle.model.BattleStatus
import com.koke1024.craftdice.domain.battle.model.BattleUnit
import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.roguelike.model.BattleSetup
import com.koke1024.craftdice.domain.roguelike.model.EnemyTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CombatSummaryBuilderTest {

    private val dice = Dice.of(DiceFace.attack(3), DiceFace.defense(), DiceFace.miss())
    private val template = EnemyTemplate("goblin", "Goblin", dice, hp = 10, rewardTier = 1)

    private fun player(id: Int, hp: Int, maxHp: Int = 10) =
        BattleUnit(id, BattleSide.PLAYER1, "P$id", dice, maxHp, hp)

    private fun enemy(id: Int) =
        BattleUnit(id, BattleSide.PLAYER2, "E", dice, 10, 10)

    private fun state(
        playerUnits: List<BattleUnit>,
        enemyUnits: List<BattleUnit>,
        status: BattleStatus,
        round: Int = 2,
    ) = BattleState(
        unitsBySide = mapOf(BattleSide.PLAYER1 to playerUnits, BattleSide.PLAYER2 to enemyUnits),
        round = round,
        status = status,
    )

    private fun setup(playerUnits: List<BattleUnit>, enemyUnits: List<BattleUnit>) =
        BattleSetup(playerUnits, enemyUnits, template)

    @Test
    fun build_playerWin_returnsSurvivorsRoundsAndTemplate() {
        val players = listOf(player(1, 7), player(2, 0))
        val enemies = listOf(enemy(1000).withDamage(10))
        val st = state(players, enemies, BattleStatus.PLAYER1_WON, round = 4)

        val summary = CombatSummaryBuilder.build(st, setup(players, enemies))

        assertEquals(BattleStatus.PLAYER1_WON, summary.status)
        assertEquals(4, summary.roundsFought)
        assertEquals(template, summary.defeatedTemplate)
        assertEquals(listOf(1), summary.survivingPlayerUnits.map { it.id })
        assertTrue(summary.playerWon)
    }

    @Test
    fun build_playerLoss_hasNoDefeatedTemplateAndEmptySurvivors() {
        val players = listOf(player(1, 0), player(2, 0))
        val enemies = listOf(enemy(1000))
        val st = state(players, enemies, BattleStatus.PLAYER2_WON, round = 3)

        val summary = CombatSummaryBuilder.build(st, setup(players, enemies))

        assertEquals(BattleStatus.PLAYER2_WON, summary.status)
        assertNull(summary.defeatedTemplate)
        assertTrue(summary.survivingPlayerUnits.isEmpty())
        assertTrue(summary.playerLost)
    }

    @Test
    fun build_draw_hasNoDefeatedTemplate() {
        val players = listOf(player(1, 0))
        val enemies = listOf(enemy(1000).withDamage(10))
        val st = state(players, enemies, BattleStatus.DRAW, round = 1)

        val summary = CombatSummaryBuilder.build(st, setup(players, enemies))

        assertEquals(BattleStatus.DRAW, summary.status)
        assertNull(summary.defeatedTemplate)
        assertTrue(summary.playerLost)
    }
}
