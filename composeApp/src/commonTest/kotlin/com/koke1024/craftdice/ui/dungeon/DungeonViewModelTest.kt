package com.koke1024.craftdice.ui.dungeon

import app.cash.turbine.test
import com.koke1024.craftdice.data.repository.MetaProgressRepository
import com.koke1024.craftdice.domain.battle.model.BattleStatus
import com.koke1024.craftdice.domain.meta.MetaProgression
import com.koke1024.craftdice.domain.meta.MetaProgressionService
import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.roguelike.RunEngine
import com.koke1024.craftdice.domain.roguelike.event.EventResolver
import com.koke1024.craftdice.domain.roguelike.generation.DungeonGenerationConfig
import com.koke1024.craftdice.domain.roguelike.generation.DungeonGenerator
import com.koke1024.craftdice.domain.roguelike.generation.EnemyCatalog
import com.koke1024.craftdice.domain.roguelike.generation.RoomTypeWeight
import com.koke1024.craftdice.domain.roguelike.model.CombatSummary
import com.koke1024.craftdice.domain.roguelike.model.EnemyTemplate
import com.koke1024.craftdice.domain.roguelike.model.RoomType
import com.koke1024.craftdice.domain.roguelike.reward.RewardRoller
import com.koke1024.craftdice.ui.session.BattleSessionHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DungeonViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val regularEnemy = EnemyTemplate("reg", "Regular", Dice.of(DiceFace.attack(2), DiceFace.miss()), hp = 10)
    private val bossEnemy = EnemyTemplate("boss", "BOSS", Dice.of(DiceFace.attack(5)), hp = 30, rewardTier = 3)
    private val catalog = EnemyCatalog(
        regular = listOf(regularEnemy),
        elite = listOf(regularEnemy),
        bosses = listOf(bossEnemy),
    )

    private val playerDice = Dice.of(DiceFace.attack(4), DiceFace.miss())

    private val combatConfig = DungeonGenerationConfig(
        totalFloors = 3,
        nodesPerFloor = 1..1,
        roomTypeWeights = mapOf(RoomTypeWeight.COMBAT to 1),
        eventIds = listOf("mysterious_chest"),
    )

    private lateinit var runEngine: RunEngine
    private lateinit var viewModel: DungeonViewModel
    private lateinit var sessionHolder: BattleSessionHolder

    private val repository = object : MetaProgressRepository {
        override fun load(): MetaProgression = MetaProgression.DEFAULT
        override fun save(progress: MetaProgression) {}
    }
    private val metaService = MetaProgressionService()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        runEngine = RunEngine(DungeonGenerator(catalog), RewardRoller(), EventResolver())
        sessionHolder = BattleSessionHolder()
        viewModel = DungeonViewModel(runEngine, repository, metaService, sessionHolder)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun startAndEnterCombat() {
        runEngine.startRun(seed = 1L, playerDice = listOf(playerDice), config = combatConfig)
        val combatRoom = runEngine.state.map.nodes.values.first { it.type == RoomType.COMBAT }
        runEngine.moveTo(combatRoom.id)
    }

    @Test
    fun fightCurrentCombat_emitsNavigateToBattleAndStagesSetup() = runTest(dispatcher) {
        startAndEnterCombat()

        viewModel.navigationEvents.test {
            viewModel.fightCurrentCombat()
            assertEquals(DungeonNavigation.NavigateToBattle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val setup = sessionHolder.consumeSetup()
        assertNotNull(setup)
        assertEquals(regularEnemy, setup.enemyTemplate)
        assertTrue(setup.playerUnits.isNotEmpty())
        assertTrue(setup.enemyUnits.isNotEmpty())
        assertNull(sessionHolder.consumeSetup(), "setup should be consumed once")
    }

    @Test
    fun onCombatResult_victoryClearsRoomAndKeepsRunOngoing() = runTest(dispatcher) {
        startAndEnterCombat()
        viewModel.fightCurrentCombat()
        advanceUntilIdle()

        val summary = CombatSummary(
            status = BattleStatus.PLAYER1_WON,
            survivingPlayerUnits = runEngine.state.playerUnits,
            roundsFought = 2,
            defeatedTemplate = regularEnemy,
        )

        viewModel.onCombatResult(summary)

        val state = viewModel.uiState.value
        assertTrue(state is DungeonUiState.Running)
        assertTrue(state.currentRoom.cleared)
        assertEquals("Regularを撃破した！", state.lastMessage)
    }

    @Test
    fun onCombatResult_lossEndsRunAsDefeat() = runTest(dispatcher) {
        startAndEnterCombat()
        viewModel.fightCurrentCombat()
        advanceUntilIdle()

        val summary = CombatSummary(
            status = BattleStatus.PLAYER2_WON,
            survivingPlayerUnits = emptyList(),
            roundsFought = 1,
            defeatedTemplate = null,
        )

        viewModel.onCombatResult(summary)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DungeonUiState.Finished)
        assertFalse(state.victory)
    }
}
