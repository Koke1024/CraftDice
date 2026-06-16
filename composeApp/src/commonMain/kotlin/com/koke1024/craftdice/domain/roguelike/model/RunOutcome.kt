package com.koke1024.craftdice.domain.roguelike.model

/**
 * Terminal result of a run, carrying the meta currency ("dice shards") the
 * player brings home.
 *
 * Both outcomes yield meta currency — clearing the dungeon awards a larger
 * sum, dying mid-run a smaller one — so Phase 5's permanent upgrades always
 * have something to consume. The dice loadout is forfeit on death ("the dice
 * shatter"); on victory the loadout could be preserved, but Phase 4 keeps
 * things uniform and drops it.
 */
sealed interface RunOutcome {
    val metaCurrency: Int
    val floorsCleared: Int
    val rewardsCollected: List<Reward>

    data class Victory(
        override val metaCurrency: Int,
        override val floorsCleared: Int,
        override val rewardsCollected: List<Reward>,
    ) : RunOutcome {
        init {
            require(metaCurrency >= 0) { "metaCurrency must be non-negative, got $metaCurrency" }
        }
    }

    data class Defeat(
        override val metaCurrency: Int,
        override val floorsCleared: Int,
        override val rewardsCollected: List<Reward>,
        val cause: String = "The party was wiped out.",
    ) : RunOutcome {
        init {
            require(metaCurrency >= 0) { "metaCurrency must be non-negative, got $metaCurrency" }
        }
    }
}
