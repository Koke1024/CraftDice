package com.koke1024.craftdice.data.repository

import com.koke1024.craftdice.domain.meta.MetaProgression

/**
 * Persistence boundary for [MetaProgression].
 *
 * The common layer depends on this interface; the SQLDelight implementation
 * lives in the data layer. Keeping it an interface makes the workshop view
 * model unit-testable with an in-memory fake and leaves room for alternative
 * backing stores without touching domain code.
 */
interface MetaProgressRepository {

    /** Returns the persisted progression, or [MetaProgression.DEFAULT] if none exists yet. */
    fun load(): MetaProgression

    /** Persists the full progression atomically (scalar fields + purchased upgrades). */
    fun save(progress: MetaProgression)
}
