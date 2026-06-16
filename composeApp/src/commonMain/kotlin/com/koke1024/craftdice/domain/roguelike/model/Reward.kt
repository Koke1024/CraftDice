package com.koke1024.craftdice.domain.roguelike.model

import com.koke1024.craftdice.domain.model.DiceFace

/**
 * A reward carried in the run inventory until it is spent at the workshop.
 *
 * Phase 4 only models acquisition and accrual; spending (crafting, permanent
 * upgrades) belongs to Phase 5. Three concrete kinds:
 * - [FaceFragment]: a "skill fragment" — a concrete [DiceFace] that can later
 *   be added to a dice via the Phase 1 [com.koke1024.craftdice.domain.craft.DiceCraftService].
 * - [DiceMaterial]: generic crafting material, gating how many faces can be
 *   attached at the workshop.
 * - [MetaCurrency]: "dice shards" — the meta currency brought home on death,
 *   consumed by permanent upgrades in Phase 5.
 */
sealed interface Reward {
    val source: RewardSource

    data class FaceFragment(
        val face: DiceFace,
        override val source: RewardSource = RewardSource.COMBAT,
    ) : Reward

    data class DiceMaterial(
        val amount: Int,
        override val source: RewardSource = RewardSource.COMBAT,
    ) : Reward {
        init {
            require(amount >= 0) { "DiceMaterial amount must be non-negative, got $amount" }
        }
    }

    data class MetaCurrency(
        val amount: Int,
        override val source: RewardSource = RewardSource.COMBAT,
    ) : Reward {
        init {
            require(amount >= 0) { "MetaCurrency amount must be non-negative, got $amount" }
        }
    }
}

/**
 * Where a reward originated, for display and meta-currency weighting.
 */
enum class RewardSource {
    COMBAT,
    ELITE,
    EVENT,
    BOSS,
    STARTING,
}
