package com.koke1024.craftdice.domain.roguelike.model

/**
 * A single node in the dungeon map graph.
 *
 * Nodes are organised by [floorIndex] (0 = dungeon entrance, last floor = the
 * boss). [nextNodeIds] encodes the directed edges the player may follow when
 * leaving this node, enabling branching Slay-the-Spire style maps. Combat
 * nodes carry their resolved [enemy] template directly (picked at generation
 * time) so the encounter is fully reproducible from the seed.
 */
data class FloorNode(
    val id: Int,
    val floorIndex: Int,
    val type: RoomType,
    val nextNodeIds: List<Int> = emptyList(),
    val enemy: EnemyTemplate? = null,
    val eventId: String? = null,
    val cleared: Boolean = false,
) {
    init {
        require(floorIndex >= 0) { "floorIndex must be non-negative, got $floorIndex" }
        val needsEnemy = type == RoomType.COMBAT ||
            type == RoomType.ELITE_COMBAT ||
            type == RoomType.BOSS
        require(!needsEnemy || enemy != null) {
            "Room type $type requires an enemy template"
        }
        require(type != RoomType.EVENT || eventId != null) {
            "EVENT room requires an eventId"
        }
    }

    fun clear(): FloorNode = copy(cleared = true)
}
