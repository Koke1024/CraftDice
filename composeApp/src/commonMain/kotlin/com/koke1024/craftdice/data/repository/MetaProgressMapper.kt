package com.koke1024.craftdice.data.repository

import com.koke1024.craftdice.domain.meta.MetaProgression

/**
 * Pure mapping helpers between SQLDelight row primitives and the domain
 * [MetaProgression].
 *
 * Kept side-effect-free and free of any SqlDriver reference so the Int/Long and
 * Set/List conversions can be unit-tested directly (a save->load roundtrip is
 * lossless if and only if these helpers are inverses).
 */
internal object MetaProgressMapper {

    fun toDomain(
        diceShards: Long,
        totalEarned: Long,
        selectedClassId: String,
        purchasedUpgradeIds: List<String>,
    ): MetaProgression = MetaProgression(
        diceShards = diceShards.toInt(),
        totalEarned = totalEarned.toInt(),
        selectedClassId = selectedClassId,
        purchasedUpgradeIds = purchasedUpgradeIds.toSet(),
    )
}
