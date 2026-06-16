package com.koke1024.craftdice.domain.roguelike.event

import kotlin.random.Random

/**
 * Resolves an [EventChoice] into a single [EventOutcome] by weight.
 *
 * Stateless: the caller supplies the [Random] so the run engine can keep the
 * full event sequence reproducible from the seed. Outcomes are weighted
 * relative to each other (they need not sum to 1).
 */
class EventResolver {

    fun resolve(choice: EventChoice, random: Random): EventOutcome {
        val total = choice.outcomes.sumOf { it.weight }
        check(total > 0.0) { "Choice '${choice.label}' has no positive-weight outcome" }
        var roll = random.nextDouble() * total
        for (outcome in choice.outcomes) {
            roll -= outcome.weight
            if (roll < 0.0) return outcome
        }
        return choice.outcomes.last { it.weight > 0.0 }
    }
}
