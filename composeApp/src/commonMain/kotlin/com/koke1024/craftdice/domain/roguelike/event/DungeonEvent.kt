package com.koke1024.craftdice.domain.roguelike.event

/**
 * A narrative encounter offering risk-and-return choices.
 *
 * Stored on a [com.koke1024.craftdice.domain.roguelike.model.FloorNode] by
 * its [id] and resolved via [EventResolver] when the player commits to a
 * choice.
 */
data class DungeonEvent(
    val id: String,
    val name: String,
    val description: String,
    val choices: List<EventChoice>,
) {
    init {
        require(choices.isNotEmpty()) { "Event '$id' must offer at least one choice" }
    }
}
