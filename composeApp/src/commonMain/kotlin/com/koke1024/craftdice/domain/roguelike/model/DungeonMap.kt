package com.koke1024.craftdice.domain.roguelike.model

/**
 * The generated dungeon: an immutable graph of [FloorNode]s plus the seed that
 * produced it.
 *
 * Two maps generated from the same seed must be structurally identical, which
 * is the core reproducibility guarantee of Phase 4's generation system.
 */
data class DungeonMap(
    val nodes: Map<Int, FloorNode>,
    val floors: List<List<Int>>,
    val startNodeId: Int,
    val bossNodeId: Int,
    val seed: Long,
) {
    val floorCount: Int get() = floors.size

    fun node(id: Int): FloorNode = nodes.getValue(id)

    fun nodeOrNull(id: Int): FloorNode? = nodes[id]

    fun nextChoices(fromNodeId: Int): List<FloorNode> =
        nodeOrNull(fromNodeId)?.nextNodeIds?.mapNotNull { nodes[it] } ?: emptyList()

    /**
     * Returns a copy with the given node replaced by [updated]; used when a
     * room is cleared.
     */
    fun withNode(updated: FloorNode): DungeonMap = copy(
        nodes = nodes + (updated.id to updated),
    )
}
