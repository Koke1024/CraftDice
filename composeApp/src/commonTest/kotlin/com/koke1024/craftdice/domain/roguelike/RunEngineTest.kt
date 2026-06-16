package com.koke1024.craftdice.domain.roguelike

import com.koke1024.craftdice.domain.battle.model.BattleStatus
import com.koke1024.craftdice.domain.craft.DiceCraftService
import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.roguelike.event.EventResolver
import com.koke1024.craftdice.domain.roguelike.generation.DungeonGenerationConfig
import com.koke1024.craftdice.domain.roguelike.generation.DungeonGenerator
import com.koke1024.craftdice.domain.roguelike.generation.EnemyCatalog
import com.koke1024.craftdice.domain.roguelike.model.CombatSummary
import com.koke1024.craftdice.domain.roguelike.model.EnemyTemplate
import com.koke1024.craftdice.domain.roguelike.model.FloorNode
import com.koke1024.craftdice.domain.roguelike.model.RunStatus
import com.koke1024.craftdice.domain.roguelike.model.RoomType
import com.koke1024.craftdice.domain.roguelike.reward.RewardRoller
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RunEngineTest {

    private val regularEnemy = EnemyTemplate(
        "reg", "Regular", Dice.of(DiceFace.attack(2), DiceFace.miss()), hp = 10, rewardTier = 0,
    )
    private val bossEnemy = EnemyTemplate(
        "boss", "BOSS", Dice.of(DiceFace.attack(5), DiceFace.critical(7)), hp = 30, rewardTier = 3,
    )

    private val catalog = EnemyCatalog(
        regular = listOf(regularEnemy),
        elite = listOf(regularEnemy),
        bosses = listOf(bossEnemy),
    )

    private val playerDice = Dice.of(
        DiceFace.attack(4), DiceFace.attack(4), DiceFace.defense(), DiceFace.miss(),
    )

    private fun engine(): RunEngine = RunEngine(
        DungeonGenerator(catalog),
        RewardRoller(),
        EventResolver(),
    )

    private val smallConfig = DungeonGenerationConfig(
        totalFloors = 3,
        nodesPerFloor = 1..1,
        eventIds = listOf("mysterious_chest"),
    )

    private val restConfig = DungeonGenerationConfig(
        totalFloors = 3,
        nodesPerFloor = 1..1,
        roomTypeWeights = mapOf(com.koke1024.craftdice.domain.roguelike.generation.RoomTypeWeight.REST to 1),
        eventIds = listOf("mysterious_chest"),
    )

    private val eventOnlyConfig = DungeonGenerationConfig(
        totalFloors = 3,
        nodesPerFloor = 1..1,
        roomTypeWeights = mapOf(com.koke1024.craftdice.domain.roguelike.generation.RoomTypeWeight.EVENT to 1),
        eventIds = listOf("mysterious_chest"),
    )

    private val combatOnlyConfig = DungeonGenerationConfig(
        totalFloors = 3,
        nodesPerFloor = 1..1,
        roomTypeWeights = mapOf(com.koke1024.craftdice.domain.roguelike.generation.RoomTypeWeight.COMBAT to 1),
        eventIds = listOf("mysterious_chest"),
    )

    private fun winSummary(enemy: EnemyTemplate, state: com.koke1024.craftdice.domain.roguelike.model.RunState) =
        CombatSummary(
            status = BattleStatus.PLAYER1_WON,
            survivingPlayerUnits = state.playerUnits,
            roundsFought = 1,
            defeatedTemplate = enemy,
        )

    private fun lossSummary() = CombatSummary(
        status = BattleStatus.PLAYER2_WON,
        survivingPlayerUnits = emptyList(),
        roundsFought = 2,
        defeatedTemplate = null,
    )

    @Test
    fun startRun_initializesStateAtStartRoom() {
        val engine = engine()

        val state = engine.startRun(seed = 1L, playerDice = listOf(playerDice), config = smallConfig)

        assertEquals(RoomType.START, state.currentRoom.type)
        assertEquals(1, state.playerUnits.size)
        assertEquals(RunStatus.ONGOING, state.status)
    }

    @Test
    fun availableMoves_listsReachableNextRooms() {
        val engine = engine()
        engine.startRun(seed = 1L, playerDice = listOf(playerDice), config = smallConfig)

        val moves = engine.availableMoves()

        assertTrue(moves.isNotEmpty())
    }

    @Test
    fun applyCombatResult_victoryRollsRewardsAndClearsRoom() {
        val engine = engine()
        engine.startRun(seed = 1L, playerDice = listOf(playerDice), config = combatOnlyConfig)
        val combatRoom = firstRoomOf(engine, RoomType.COMBAT)!!
        engine.moveTo(combatRoom.id)

        engine.applyCombatResult(winSummary(combatRoom.enemy!!, engine.state))

        assertTrue(engine.state.inventory.isNotEmpty())
        assertEquals(1, engine.state.roomsCleared)
        assertTrue(engine.state.map.node(combatRoom.id).cleared)
    }

    @Test
    fun applyCombatResult_bossVictoryEndsRunAsVictory() {
        val engine = engine()
        engine.startRun(seed = 1L, playerDice = listOf(playerDice), config = smallConfig)
        reachBoss(engine)
        val bossRoom = engine.state.map.node(engine.state.map.bossNodeId)
        engine.moveTo(bossRoom.id)

        engine.applyCombatResult(winSummary(bossRoom.enemy!!, engine.state))

        assertEquals(RunStatus.VICTORY, engine.state.status)
    }

    @Test
    fun applyCombatResult_lossEndsRunAsDefeat() {
        val engine = engine()
        engine.startRun(seed = 1L, playerDice = listOf(playerDice), config = combatOnlyConfig)
        val combatRoom = firstRoomOf(engine, RoomType.COMBAT)!!
        engine.moveTo(combatRoom.id)

        engine.applyCombatResult(lossSummary())

        assertEquals(RunStatus.DEFEAT, engine.state.status)
    }

    @Test
    fun resolveEvent_clearsRoomAfterApplyingEffects() {
        val engine = engine()
        engine.startRun(seed = 42L, playerDice = listOf(playerDice), config = eventOnlyConfig)
        val eventRoom = firstRoomOf(engine, RoomType.EVENT)!!
        engine.moveTo(eventRoom.id)

        val resolution = engine.resolveEvent(choiceIndex = 1)

        assertTrue(engine.state.map.node(eventRoom.id).cleared)
        assertEquals(engine.state, resolution.state)
    }

    @Test
    fun rest_fullyHealsPartyAndClearsRoom() {
        val engine = engine()
        engine.startRun(seed = 7L, playerDice = listOf(playerDice), config = restConfig)
        val restRoom = firstRoomOf(engine, RoomType.REST)!!
        engine.moveTo(restRoom.id)
        val hurt = engine.state.playerUnits.map { it.withDamage(5) }
        injectState(engine, engine.state.copy(playerUnits = hurt))

        engine.rest()

        engine.state.playerUnits.forEach {
            assertEquals(it.maxHp, it.currentHp)
            assertEquals(emptySet(), it.brokenFaceIndices)
        }
        assertTrue(engine.state.map.node(restRoom.id).cleared)
    }

    @Test
    fun finish_victoryProducesOutcomeWithMetaCurrency() {
        val engine = engine()
        engine.startRun(seed = 1L, playerDice = listOf(playerDice), config = smallConfig)
        reachBoss(engine)
        val bossRoom = engine.state.map.node(engine.state.map.bossNodeId)
        engine.moveTo(bossRoom.id)
        engine.applyCombatResult(winSummary(bossRoom.enemy!!, engine.state))

        val outcome = engine.finish()

        assertTrue(outcome is com.koke1024.craftdice.domain.roguelike.model.RunOutcome.Victory)
        assertTrue(outcome.metaCurrency > 0)
    }

    @Test
    fun finish_defeatProducesOutcomeWithMetaCurrency() {
        val engine = engine()
        engine.startRun(seed = 1L, playerDice = listOf(playerDice), config = combatOnlyConfig)
        val combatRoom = firstRoomOf(engine, RoomType.COMBAT)!!
        engine.moveTo(combatRoom.id)
        engine.applyCombatResult(lossSummary())

        val outcome = engine.finish()

        assertTrue(outcome is com.koke1024.craftdice.domain.roguelike.model.RunOutcome.Defeat)
        assertTrue(outcome.metaCurrency >= 0)
    }

    @Test
    fun fullRunFlow_victoryClearsBossAndProducesMetaCurrency() {
        val engine = engine()
        engine.startRun(seed = 100L, playerDice = listOf(playerDice), config = smallConfig)

        val intermediate = engine.availableMoves().first()
        engine.moveTo(intermediate.id)
        clearCurrentRoomAnyType(engine, intermediate)
        check(engine.state.status == RunStatus.ONGOING) {
            "Run ended too early at intermediate room ${intermediate.type}"
        }

        val bossRoom = engine.state.map.node(engine.state.map.bossNodeId)
        engine.moveTo(bossRoom.id)
        engine.applyCombatResult(winSummary(bossRoom.enemy!!, engine.state))

        assertEquals(RunStatus.VICTORY, engine.state.status)
        val outcome = engine.finish()
        assertTrue(outcome.metaCurrency > 0)
    }

    @Test
    fun rewardFacesAreCraftableViaDiceCraftService() {
        val engine = engine()
        engine.startRun(seed = 1L, playerDice = listOf(playerDice), config = smallConfig)
        reachBoss(engine)
        val bossRoom = engine.state.map.node(engine.state.map.bossNodeId)
        engine.moveTo(bossRoom.id)
        engine.applyCombatResult(winSummary(bossRoom.enemy!!, engine.state))

        val collectedFaces = engine.state.collectedFaces()
        assertTrue(collectedFaces.isNotEmpty(), "Boss fight should drop at least one face")

        val base = Dice.of(DiceFace.attack(2), DiceFace.defense())
        collectedFaces.forEach { face ->
            val result = DiceCraftService.addFace(base, face)
            assertTrue(
                result is com.koke1024.craftdice.domain.craft.CraftResult.Success,
                "Collected face $face should be addable to a dice",
            )
        }
    }

    @Test
    fun currentBattleSetup_providesNonCollidingPlayerAndEnemyIds() {
        val engine = engine()
        engine.startRun(seed = 1L, playerDice = listOf(playerDice), config = combatOnlyConfig)
        val combatRoom = firstRoomOf(engine, RoomType.COMBAT)!!
        engine.moveTo(combatRoom.id)

        val setup = engine.currentBattleSetup()

        val playerIds = setup.playerUnits.map { it.id }
        val enemyIds = setup.enemyUnits.map { it.id }
        assertTrue(playerIds.none { it in enemyIds })
        assertEquals(1000, setup.enemyUnits.first().id)
    }

    @Test
    fun runIsReproducible_sameSeedProducesSameRewardSequence() {
        fun play(): List<com.koke1024.craftdice.domain.roguelike.model.Reward> {
            val engine = engine()
            engine.startRun(seed = 555L, playerDice = listOf(playerDice), config = smallConfig)
            reachBoss(engine)
            if (engine.state.isFinished) return engine.state.inventory
            val bossRoom = engine.state.map.node(engine.state.map.bossNodeId)
            engine.moveTo(bossRoom.id)
            engine.applyCombatResult(winSummary(bossRoom.enemy!!, engine.state))
            return engine.state.inventory
        }

        assertEquals(play(), play())
    }

    private fun firstRoomOf(
        engine: RunEngine,
        vararg types: RoomType,
    ): FloorNode? {
        val all = engine.state.map.nodes.values
        return all.firstOrNull { it.type in types && it.type != RoomType.START }
    }

    /** Advances through the single intermediate floor and stops at the boss door. */
    private fun reachBoss(engine: RunEngine) {
        val intermediate = engine.availableMoves().first()
        engine.moveTo(intermediate.id)
        clearCurrentRoomAnyType(engine, intermediate)
        check(engine.state.status == RunStatus.ONGOING) {
            "Intermediate room ended the run unexpectedly"
        }
    }

    private fun clearCurrentRoomAnyType(engine: RunEngine, room: FloorNode) {
        when (room.type) {
            RoomType.COMBAT, RoomType.ELITE_COMBAT, RoomType.BOSS -> {
                val enemy = room.enemy ?: return
                engine.applyCombatResult(winSummary(enemy, engine.state))
            }
            RoomType.EVENT -> {
                engine.resolveEvent(choiceIndex = 1)
            }
            RoomType.REWARD -> engine.claimReward()
            RoomType.REST -> engine.rest()
            RoomType.START -> Unit
        }
    }

    private fun injectState(engine: RunEngine, state: com.koke1024.craftdice.domain.roguelike.model.RunState) {
        val field = RunEngine::class.java.getDeclaredField("state")
        field.isAccessible = true
        field.set(engine, state)
    }
}
