package com.koke1024.craftdice.domain.roguelike.event

/**
 * One possible result of picking an [EventChoice].
 *
 * [weight] is a relative probability against the other outcomes of the same
 * choice (it is normalised at resolve time, so values need not sum to 1).
 * [effects] are applied in order; [message] is the human-readable narration.
 */
data class EventOutcome(
    val weight: Double,
    val effects: List<EventEffect>,
    val message: String,
) {
    init {
        require(weight >= 0.0) { "weight must be non-negative, got $weight" }
    }
}

/**
 * A selectable option in a [DungeonEvent].
 *
 * Outcomes are resolved probabilistically by [EventResolver], so a single
 * choice can carry both upside and downside ("risk and return"). A choice
 * with a single guaranteed outcome behaves deterministically.
 */
data class EventChoice(
    val label: String,
    val outcomes: List<EventOutcome>,
) {
    init {
        require(outcomes.isNotEmpty()) { "EventChoice '$label' must have at least one outcome" }
        require(outcomes.any { it.weight > 0.0 }) {
            "EventChoice '$label' must have at least one outcome with weight > 0"
        }
    }
}
