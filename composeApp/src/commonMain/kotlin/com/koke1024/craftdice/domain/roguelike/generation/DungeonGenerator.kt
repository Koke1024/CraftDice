package com.koke1024.craftdice.domain.roguelike.generation

import com.koke1024.craftdice.domain.roguelike.model.DungeonMap
import com.koke1024.craftdice.domain.roguelike.model.EnemyTemplate
import com.koke1024.craftdice.domain.roguelike.model.FloorNode
import com.koke1024.craftdice.domain.roguelike.model.RoomType
import kotlin.random.Random

/**
 * Procedurally builds a branching [DungeonMap] from a seed.
 *
 * The layout is a directed acyclic graph layered by floor: a single START
 * node, a configurable number of intermediate floors (each with several
 * branching rooms), and a single BOSS node at the bottom. Connections always
 * point forward and every room is guaranteed reachable, so no run can dead-end.
 *
 * Injecting a deterministic [randomFactory] makes generation fully
 * reproducible for a given seed — the central Phase 4 guarantee.
 */
class DungeonGenerator(
    private val catalog: EnemyCatalog,
    private val randomFactory: (Long) -> Random = { Random(it) },
) {
    fun generate(seed: Long, config: DungeonGenerationConfig): DungeonMap {
        val random = randomFactory(seed)
        val nodes = mutableMapOf<Int, FloorNode>()
        val floors = mutableListOf<MutableList<Int>>()
        var nextId = 0

        val startId = nextId++
        nodes[startId] = FloorNode(id = startId, floorIndex = 0, type = RoomType.START)
        floors += mutableListOf(startId)

        val lastFloor = config.totalFloors - 1
        for (floorIndex in 1 until lastFloor) {
            val count = random.nextInt(
                config.nodesPerFloor.first,
                config.nodesPerFloor.last + 1,
            )
            val floorIds = mutableListOf<Int>()
            repeat(count) {
                val id = nextId++
                val type = pickRoomType(random, floorIndex, config)
                val enemy = pickEnemy(type, random)
                val eventId = if (type == RoomType.EVENT) {
                    config.eventIds.random(random)
                } else {
                    null
                }
                nodes[id] = FloorNode(
                    id = id,
                    floorIndex = floorIndex,
                    type = type,
                    enemy = enemy,
                    eventId = eventId,
                )
                floorIds += id
            }
            floors += floorIds
        }

        val bossId = nextId++
        nodes[bossId] = FloorNode(
            id = bossId,
            floorIndex = lastFloor,
            type = RoomType.BOSS,
            enemy = catalog.bosses.random(random),
        )
        floors += mutableListOf(bossId)

        connectFloors(floors, nodes, random)

        return DungeonMap(
            nodes = nodes.toMap(),
            floors = floors.map { it.toList() },
            startNodeId = startId,
            bossNodeId = bossId,
            seed = seed,
        )
    }

    private fun pickRoomType(
        random: Random,
        floorIndex: Int,
        config: DungeonGenerationConfig,
    ): RoomType {
        val entries = config.roomTypeWeights.entries
            .filter { it.key != RoomTypeWeight.ELITE_COMBAT || floorIndex >= config.eliteMinFloor }
            .map { it.key to it.value }
        return when (random.weightedPick(entries)) {
            RoomTypeWeight.COMBAT -> RoomType.COMBAT
            RoomTypeWeight.ELITE_COMBAT -> RoomType.ELITE_COMBAT
            RoomTypeWeight.REWARD -> RoomType.REWARD
            RoomTypeWeight.EVENT -> RoomType.EVENT
            RoomTypeWeight.REST -> RoomType.REST
        }
    }

    private fun pickEnemy(type: RoomType, random: Random): EnemyTemplate? = when (type) {
        RoomType.COMBAT -> catalog.regular.random(random)
        RoomType.ELITE_COMBAT -> catalog.elite.random(random)
        else -> null
    }

    private fun connectFloors(
        floors: List<MutableList<Int>>,
        nodes: MutableMap<Int, FloorNode>,
        random: Random,
    ) {
        for (i in 0 until floors.size - 1) {
            val prev = floors[i]
            val curr = floors[i + 1]
            val reached = curr.associateWith { false }.toMutableMap()

            prev.forEachIndexed { idx, fromId ->
                val base = mapIndex(idx, prev.size, curr.size)
                val lo = (base - 1).coerceAtLeast(0)
                val hi = (base + 1).coerceAtMost(curr.size - 1)
                val targets = mutableSetOf(curr[base])
                if (lo != hi && random.nextBoolean()) {
                    targets += curr[random.nextInt(lo, hi + 1)]
                }
                targets.forEach { to ->
                    appendEdge(nodes, fromId, to)
                    reached[to] = true
                }
            }

            curr.forEachIndexed { idx, nodeId ->
                if (reached[nodeId] != true) {
                    val sourceIndex = mapIndex(idx, curr.size, prev.size).coerceIn(0, prev.size - 1)
                    appendEdge(nodes, prev[sourceIndex], nodeId)
                }
            }
        }
    }

    private fun appendEdge(
        nodes: MutableMap<Int, FloorNode>,
        fromId: Int,
        toId: Int,
    ) {
        val from = nodes.getValue(fromId)
        if (toId !in from.nextNodeIds) {
            nodes[fromId] = from.copy(nextNodeIds = from.nextNodeIds + toId)
        }
    }

    private fun mapIndex(from: Int, fromSize: Int, toSize: Int): Int {
        if (toSize <= 1) return 0
        val scaled = ((from + 0.5) * toSize / fromSize).toInt()
        return scaled.coerceIn(0, toSize - 1)
    }

    private fun <T> List<T>.random(random: Random): T = this[random.nextInt(size)]

    private fun <T> Random.weightedPick(entries: List<Pair<T, Int>>): T {
        val total = entries.sumOf { it.second }.coerceAtLeast(1)
        var roll = nextInt(total)
        for ((item, weight) in entries) {
            roll -= weight
            if (roll < 0) return item
        }
        return entries.last().first
    }
}
