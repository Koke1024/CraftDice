package com.koke1024.craftdice.ui.battle

import com.koke1024.craftdice.domain.roguelike.RunEngine
import com.koke1024.craftdice.domain.roguelike.event.EventResolver
import com.koke1024.craftdice.domain.roguelike.generation.DungeonGenerationConfig
import com.koke1024.craftdice.domain.roguelike.generation.DungeonGenerator
import com.koke1024.craftdice.domain.roguelike.generation.EnemyCatalog
import com.koke1024.craftdice.domain.roguelike.generation.RoomTypeWeight
import com.koke1024.craftdice.domain.roguelike.model.EnemyTemplate
import com.koke1024.craftdice.domain.roguelike.model.RoomType
import com.koke1024.craftdice.domain.roguelike.reward.RewardRoller
import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.ui.session.BattleSessionHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BattleViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val regularEnemy = EnemyTemplate("reg", "Regular", Dice.of(DiceFace.attack(2), DiceFace.miss()), hp = 10)
    private val bossEnemy = EnemyTemplate("boss", "BOSS", Dice.of(DiceFace.attack(5)), hp = 30, rewardTier = 3)
    private val catalog = EnemyCatalog(
        regular = listOf(regularEnemy),
        elite = listOf(regularEnemy),
        bosses = listOf(bossEnemy),
    )
    private val playerDice = Dice.of(
        DiceFace.attack(4), DiceFace.attack(4), DiceFace.defense(),
        DiceFace.heal(3), DiceFace.critical(6), DiceFace.miss(),
    )

    private val combatConfig = DungeonGenerationConfig(
        totalFloors = 3,
        nodesPerFloor = 1..1,
        roomTypeWeights = mapOf(RoomTypeWeight.COMBAT to 1),
        eventIds = listOf("mysterious_chest"),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_fromStagedSetup_seedsEngineAndExposesUnits() = runTest(dispatcher) {
        val runEngine = RunEngine(DungeonGenerator(catalog), RewardRoller(), EventResolver())
        runEngine.startRun(seed = 1L, playerDice = listOf(playerDice), config = combatConfig)
        val combatRoom = runEngine.state.map.nodes.values.first { it.type == RoomType.COMBAT }
        runEngine.moveTo(combatRoom.id)

        val setup = runEngine.currentBattleSetup()
        val holder = BattleSessionHolder()
        holder.launch(setup)

        val vm = BattleViewModel(holder)

        val state = vm.uiState.value
        assertTrue(state.launchedFromDungeon)
        assertEquals(BattleStatusUi.ONGOING, state.status)
        assertTrue(state.playerUnits.isNotEmpty())
        assertTrue(state.enemyUnits.isNotEmpty())
        assertEquals(setup.playerUnits.size, state.playerUnits.size)
        assertEquals(setup.enemyUnits.size, state.enemyUnits.size)
    }

    @Test
    fun init_withoutStagedSetup_fallsBackToDefault() = runTest(dispatcher) {
        val holder = BattleSessionHolder()

        val vm = BattleViewModel(holder)

        val state = vm.uiState.value
        assertTrue(!state.launchedFromDungeon)
        assertEquals(BattleStatusUi.ONGOING, state.status)
        assertTrue(state.playerUnits.isNotEmpty())
        assertTrue(state.enemyUnits.isNotEmpty())
    }
}
