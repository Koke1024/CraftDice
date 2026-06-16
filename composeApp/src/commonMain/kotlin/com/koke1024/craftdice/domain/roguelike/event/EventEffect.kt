package com.koke1024.craftdice.domain.roguelike.event

import com.koke1024.craftdice.domain.roguelike.model.Reward

/**
 * A concrete consequence applied to the run state when an event resolves.
 *
 * Events are the "risk and return" lever of the roguelike loop: choices map
 * to one or more of these effects, possibly probabilistically. The run
 * engine owns application (mutating [com.koke1024.craftdice.domain.roguelike.model.RunState]);
 * this type only describes intent.
 */
sealed interface EventEffect {

    /** Restores [amount] HP to every surviving player unit. */
    data class Heal(val amount: Int) : EventEffect {
        init {
            require(amount >= 0) { "Heal amount must be non-negative, got $amount" }
        }
    }

    /** Deals [amount] damage to every surviving player unit. */
    data class Damage(val amount: Int) : EventEffect {
        init {
            require(amount >= 0) { "Damage amount must be non-negative, got $amount" }
        }
    }

    /** Restores all player units to full HP and clears broken faces. */
    data object FullHeal : EventEffect

    /** Adds [reward] to the run inventory. */
    data class GainReward(val reward: Reward) : EventEffect

    /** Removes up to [amount] dice material from the inventory. */
    data class LoseDiceMaterial(val amount: Int) : EventEffect {
        init {
            require(amount >= 0) { "LoseDiceMaterial amount must be non-negative, got $amount" }
        }
    }

    /** Grants [amount] meta currency directly into the inventory. */
    data class GainMetaCurrency(val amount: Int) : EventEffect {
        init {
            require(amount >= 0) { "GainMetaCurrency amount must be non-negative, got $amount" }
        }
    }
}
