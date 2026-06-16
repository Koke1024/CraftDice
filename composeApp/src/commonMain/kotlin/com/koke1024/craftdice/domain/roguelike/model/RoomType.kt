package com.koke1024.craftdice.domain.roguelike.model

/**
 * Kind of room a dungeon node represents.
 *
 * Drives the encounter that fires when the player enters the node.
 * Order roughly reflects rising stakes within a floor.
 */
enum class RoomType {
    COMBAT,
    ELITE_COMBAT,
    REWARD,
    EVENT,
    REST,
    BOSS,
    START,
}
