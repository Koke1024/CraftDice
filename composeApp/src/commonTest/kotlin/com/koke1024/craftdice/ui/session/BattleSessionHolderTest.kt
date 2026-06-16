package com.koke1024.craftdice.ui.session

import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleStatus
import com.koke1024.craftdice.domain.battle.model.BattleUnit
import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.roguelike.model.BattleSetup
import com.koke1024.craftdice.domain.roguelike.model.CombatSummary
import com.koke1024.craftdice.domain.roguelike.model.EnemyTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BattleSessionHolderTest {

    private val dice = Dice.of(DiceFace.attack(2), DiceFace.miss())
    private val template = EnemyTemplate("goblin", "Goblin", dice, hp = 8)
    private val player = BattleUnit.fromDice(1, BattleSide.PLAYER1, "P", dice, 10)
    private val enemy = BattleUnit.fromDice(1000, BattleSide.PLAYER2, "E", dice, 8)

    private val setup = BattleSetup(listOf(player), listOf(enemy), template)
    private val summary = CombatSummary(
        status = BattleStatus.PLAYER1_WON,
        survivingPlayerUnits = listOf(player),
        roundsFought = 2,
        defeatedTemplate = template,
    )

    @Test
    fun consumeSetup_returnsNullWhenNothingStaged() {
        val holder = BattleSessionHolder()
        assertNull(holder.consumeSetup())
    }

    @Test
    fun launch_thenConsumeSetup_returnsStagedValueAndClears() {
        val holder = BattleSessionHolder()
        holder.launch(setup)

        assertEquals(setup, holder.consumeSetup())
        assertNull(holder.consumeSetup(), "setup slot should be cleared after consume")
    }

    @Test
    fun launch_overwritesPreviousStagedSetup() {
        val holder = BattleSessionHolder()
        holder.launch(setup)
        val other = setup.copy(enemyTemplate = template.copy(id = "orc"))
        holder.launch(other)

        assertEquals(other, holder.consumeSetup())
    }

    @Test
    fun consumeResult_returnsNullWhenNothingPublished() {
        val holder = BattleSessionHolder()
        assertNull(holder.consumeResult())
    }

    @Test
    fun publishResult_thenConsumeResult_returnsPublishedValueAndClears() {
        val holder = BattleSessionHolder()
        holder.publishResult(summary)

        assertEquals(summary, holder.consumeResult())
        assertNull(holder.consumeResult(), "result slot should be cleared after consume")
    }

    @Test
    fun launchAndPublishAreIndependentSlots() {
        val holder = BattleSessionHolder()
        holder.launch(setup)
        holder.publishResult(summary)

        assertEquals(setup, holder.consumeSetup())
        assertEquals(summary, holder.consumeResult())
    }
}
