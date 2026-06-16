package com.koke1024.craftdice.domain.roguelike.meta

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.roguelike.model.DungeonMap
import com.koke1024.craftdice.domain.roguelike.model.EnemyTemplate
import com.koke1024.craftdice.domain.roguelike.model.FloorNode
import com.koke1024.craftdice.domain.roguelike.model.RoomType
import com.koke1024.craftdice.domain.roguelike.model.RunState
import com.koke1024.craftdice.domain.roguelike.model.RunStatus
import com.koke1024.craftdice.domain.roguelike.model.Reward
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetaCurrencyCalculatorTest {

    private val dummyEnemy = EnemyTemplate("boss", "BOSS", Dice.of(DiceFace.attack(5)), hp = 30)

    private fun map(): DungeonMap {
        val start = FloorNode(0, 0, RoomType.START, nextNodeIds = listOf(1))
        val boss = FloorNode(1, 1, RoomType.BOSS, enemy = dummyEnemy)
        return DungeonMap(
            nodes = mapOf(0 to start, 1 to boss),
            floors = listOf(listOf(0), listOf(1)),
            startNodeId = 0,
            bossNodeId = 1,
            seed = 0L,
        )
    }

    private fun state(
        roomsCleared: Int = 0,
        enemiesDefeated: Int = 0,
        metaInInventory: Int = 0,
    ): RunState = RunState(
        seed = 0L,
        playerUnits = listOf(
            com.koke1024.craftdice.domain.battle.model.BattleUnit.fromDice(
                id = 1,
                owner = com.koke1024.craftdice.domain.battle.model.BattleSide.PLAYER1,
                name = "P1",
                dice = Dice.of(DiceFace.attack(3)),
            ),
        ),
        inventory = if (metaInInventory > 0) listOf(Reward.MetaCurrency(metaInInventory)) else emptyList(),
        map = map(),
        currentRoomId = 0,
        status = RunStatus.ONGOING,
        enemiesDefeated = enemiesDefeated,
        roomsCleared = roomsCleared,
    )

    @Test
    fun forVictory_includesInventoryPlusProgressPlusBonus() {
        val s = state(roomsCleared = 4, enemiesDefeated = 3, metaInInventory = 5)

        val total = MetaCurrencyCalculator.forVictory(s)

        // 5 (inventory) + 4*2 (floors) + 3*1 (enemies) + 20 (bonus) = 36
        assertEquals(36, total)
    }

    @Test
    fun forDefeat_includesInventoryPlusHalvedProgress() {
        val s = state(roomsCleared = 4, enemiesDefeated = 3, metaInInventory = 5)

        val total = MetaCurrencyCalculator.forDefeat(s)

        // 5 (inventory) + (4*2 + 3*1)/2 = 5 + 11/2 = 5 + 5 = 10
        assertEquals(10, total)
    }

    @Test
    fun forVictory_isAlwaysGreaterThanOrEqualToDefeat() {
        val s = state(roomsCleared = 6, enemiesDefeated = 8, metaInInventory = 2)

        assertTrue(MetaCurrencyCalculator.forVictory(s) >= MetaCurrencyCalculator.forDefeat(s))
    }

    @Test
    fun forOutcome_routesByStatus() {
        val s = state(roomsCleared = 2, enemiesDefeated = 1, metaInInventory = 0)

        assertEquals(MetaCurrencyCalculator.forVictory(s), MetaCurrencyCalculator.forOutcome(s, RunStatus.VICTORY))
        assertEquals(MetaCurrencyCalculator.forDefeat(s), MetaCurrencyCalculator.forOutcome(s, RunStatus.DEFEAT))
        assertEquals(0, MetaCurrencyCalculator.forOutcome(s, RunStatus.ONGOING))
    }

    @Test
    fun forDefeat_minimumZeroWhenNothingEarned() {
        val s = state(roomsCleared = 0, enemiesDefeated = 0, metaInInventory = 0)

        assertEquals(0, MetaCurrencyCalculator.forDefeat(s))
    }
}
