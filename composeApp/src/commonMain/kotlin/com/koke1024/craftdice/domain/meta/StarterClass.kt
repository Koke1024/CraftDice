package com.koke1024.craftdice.domain.meta

import com.koke1024.craftdice.domain.model.Dice

/**
 * A named starting loadout the player can begin a run with.
 *
 * Each class binds a flavor identity to a concrete set of dice. The default
 * class ([isDefault]) is available from a fresh save; the rest are gated behind
 * a [PersistentUpgrade] whose [UpgradeEffect.UnlockClass] targets this class's
 * [id]. Selecting a class is a free, reversible choice (no shard cost); only
 * unlocking it costs shards.
 */
data class StarterClass(
    val id: String,
    val displayName: String,
    val description: String,
    val dice: List<Dice>,
    val isDefault: Boolean = false,
)
