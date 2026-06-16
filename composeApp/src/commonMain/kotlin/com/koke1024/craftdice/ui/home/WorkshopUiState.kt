package com.koke1024.craftdice.ui.home

import com.koke1024.craftdice.domain.meta.PersistentUpgrade
import com.koke1024.craftdice.domain.meta.StarterClass

/**
 * Immutable view-model of a single upgrade row in the workshop list.
 */
data class UpgradeRow(
    val upgrade: PersistentUpgrade,
    val owned: Boolean,
    val affordable: Boolean,
)

/**
 * Immutable view-model of a starter class chip.
 */
data class ClassRow(
    val starterClass: StarterClass,
    val unlocked: Boolean,
    val selected: Boolean,
)

/**
 * Full UI state for the workshop (home) screen.
 *
 * [balance]/[totalEarned] come straight from the persisted progression; the
 * upgrade and class lists are pre-computed projections so the composable stays
 * free of domain logic. [message] is a transient toast-like line set after a
 * purchase or selection.
 */
data class WorkshopUiState(
    val balance: Int = 0,
    val totalEarned: Int = 0,
    val upgrades: List<UpgradeRow> = emptyList(),
    val classes: List<ClassRow> = emptyList(),
    val message: String? = null,
)
