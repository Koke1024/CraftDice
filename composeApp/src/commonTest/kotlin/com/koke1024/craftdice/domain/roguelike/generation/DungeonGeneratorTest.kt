package com.koke1024.craftdice.domain.roguelike.generation

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.roguelike.model.EnemyTemplate
import com.koke1024.craftdice.domain.roguelike.model.RoomType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DungeonGeneratorTest {

    private val catalog = EnemyCatalog(
        regular = listOf(
            EnemyTemplate("r1", "R1", Dice.of(DiceFace.attack(2), DiceFace.miss()), hp = 10, rewardTier = 0),
            EnemyTemplate("r2", "R2", Dice.of(DiceFace.attack(3)), hp = 12, rewardTier = 1),
        ),
        elite = listOf(
            EnemyTemplate("e1", "E1", Dice.of(DiceFace.attack(4), DiceFace.defense()), hp = 16, rewardTier = 1),
        ),
        bosses = listOf(
            EnemyTemplate("boss", "BOSS", Dice.of(DiceFace.attack(5), DiceFace.critical(7)), hp = 30, rewardTier = 3),
        ),
    )

    private fun generator(): DungeonGenerator = DungeonGenerator(catalog)

    private val config = DungeonGenerationConfig(
        totalFloors = 5,
        nodesPerFloor = 2..3,
        eventIds = listOf("ev1", "ev2"),
    )

    private fun layoutSignature(m: com.koke1024.craftdice.domain.roguelike.model.DungeonMap): String =
        m.floors.joinToString("|") { it.joinToString(",") } + "/" +
            m.nodes.values.joinToString(";") {
                "${it.type}:${it.enemy?.id ?: '-'}:${it.nextNodeIds.sorted()}"
            }

    @Test
    fun generate_sameSeedProducesIdenticalMaps() {
        val gen = generator()

        val a = gen.generate(seed = 42L, config = config)
        val b = gen.generate(seed = 42L, config = config)

        assertEquals(a.seed, b.seed)
        assertEquals(a.nodes.keys, b.nodes.keys)
        a.nodes.forEach { (id, node) ->
            assertEquals(node, b.node(id))
        }
        assertEquals(a.floors, b.floors)
    }

    @Test
    fun generate_differentSeedsProduceVariedLayouts() {
        val gen = generator()
        val layouts = (1..20).map { seed ->
            layoutSignature(gen.generate(seed.toLong(), config))
        }.toSet()
        assertTrue(
            layouts.size > 1,
            "Different seeds should produce more than one layout, got ${layouts.size}",
        )
    }

    @Test
    fun generate_placesStartAtFloorZeroAndBossAtDeepestFloor() {
        val map = generator().generate(seed = 7L, config = config)

        val start = map.node(map.startNodeId)
        assertEquals(RoomType.START, start.type)
        assertEquals(0, start.floorIndex)

        val boss = map.node(map.bossNodeId)
        assertEquals(RoomType.BOSS, boss.type)
        assertEquals(config.totalFloors - 1, boss.floorIndex)
        assertNotNull(boss.enemy)
    }

    @Test
    fun generate_hasExactlyOneBossNode() {
        val map = generator().generate(seed = 7L, config = config)

        val bossCount = map.nodes.values.count { it.type == RoomType.BOSS }
        assertEquals(1, bossCount)
    }

    @Test
    fun generate_combatAndEliteNodesCarryEnemyTemplates() {
        val map = generator().generate(seed = 3L, config = config)

        map.nodes.values
            .filter { it.type == RoomType.COMBAT || it.type == RoomType.ELITE_COMBAT }
            .forEach { assertNotNull(it.enemy, "Combat node ${it.id} should have an enemy") }
    }

    @Test
    fun generate_eventNodesCarryEventIds() {
        val map = generator().generate(seed = 11L, config = config)

        map.nodes.values
            .filter { it.type == RoomType.EVENT }
            .forEach { assertNotNull(it.eventId, "Event node ${it.id} should have an eventId") }
        assertTrue(
            map.nodes.values.any { it.type == RoomType.EVENT } ||
                config.eventIds.isNotEmpty(),
        )
    }

    @Test
    fun generate_totalFloorCountMatchesConfig() {
        val map = generator().generate(seed = 5L, config = config)

        assertEquals(config.totalFloors, map.floorCount)
        assertEquals(config.totalFloors, map.floors.size)
    }

    @Test
    fun generate_intermediateNodeCountStaysWithinConfigRange() {
        val map = generator().generate(seed = 5L, config = config)

        map.floors.forEachIndexed { index, floor ->
            if (index == 0 || index == config.totalFloors - 1) return@forEachIndexed
            assertTrue(
                floor.size in config.nodesPerFloor,
                "Floor $index has ${floor.size} nodes, outside ${config.nodesPerFloor}",
            )
        }
    }

    @Test
    fun generate_noOrphanNodesEveryRoomReachable() {
        val map = generator().generate(seed = 21L, config = config)

        val reachable = mutableSetOf(map.startNodeId)
        var frontier = listOf(map.startNodeId)
        while (frontier.isNotEmpty()) {
            val next = frontier.flatMap { map.nextChoices(it) }.map { it.id }
            val fresh = next.filter { it !in reachable }
            reachable += fresh
            frontier = fresh
        }

        map.nodes.keys.forEach { id ->
            assertTrue(id in reachable, "Node $id is unreachable from the start")
        }
    }

    @Test
    fun generate_startNodeHasNoIncomingEdgeAndEveryNodeHasAnOutgoingEdgeExceptBoss() {
        val map = generator().generate(seed = 8L, config = config)

        val incoming = mutableMapOf<Int, Int>()
        map.nodes.values.forEach { node ->
            node.nextNodeIds.forEach { target ->
                incoming[target] = (incoming[target] ?: 0) + 1
            }
        }

        // Every non-start node should be reachable from the previous floor.
        map.nodes.values.forEach { node ->
            if (node.type != RoomType.START) {
                assertTrue(
                    (incoming[node.id] ?: 0) > 0,
                    "Node ${node.id} (${node.type}) has no incoming edge",
                )
            }
        }
        // Every non-boss node should lead somewhere.
        map.nodes.values.forEach { node ->
            if (node.type != RoomType.BOSS) {
                assertTrue(
                    node.nextNodeIds.isNotEmpty(),
                    "Node ${node.id} (${node.type}) has no outgoing edge",
                )
            }
        }
    }

    @Test
    fun generate_eliteDoesNotAppearBeforeEliteMinFloor() {
        val cfg = config.copy(eliteMinFloor = 3)
        val map = generator().generate(seed = 100L, config = cfg)

        map.nodes.values.forEach { node ->
            if (node.type == RoomType.ELITE_COMBAT) {
                assertTrue(
                    node.floorIndex >= cfg.eliteMinFloor,
                    "Elite combat appeared on floor ${node.floorIndex}, before min ${cfg.eliteMinFloor}",
                )
            }
        }
    }

    @Test
    fun generate_rejectsTooFewFloors() {
        try {
            DungeonGenerationConfig(totalFloors = 2)
            error("Should have thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("totalFloors"))
        }
    }

    @Test
    fun generate_bossPickIsReproducibleForSameSeed() {
        val gen = generator()

        val a = gen.generate(seed = 13L, config = config)
        val b = gen.generate(seed = 13L, config = config)

        assertEquals(a.node(a.bossNodeId).enemy, b.node(b.bossNodeId).enemy)
    }
}
