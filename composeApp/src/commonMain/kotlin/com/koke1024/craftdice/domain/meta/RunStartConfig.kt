package com.koke1024.craftdice.domain.meta

import com.koke1024.craftdice.domain.model.Dice

/**
 * Resolved starting conditions for a new run, derived from the player's
 * [MetaProgression].
 *
 * The dungeon layer reads this instead of hard-coding a loadout, so purchased
 * upgrades (selected class, bonus material, bonus HP) take effect automatically.
 */
data class RunStartConfig(
    val starterClass: StarterClass,
    val dice: List<Dice>,
    val bonusStartingMaterial: Int,
    val bonusStartingHp: Int,
)

/**
 * Builds the [RunStartConfig] for the next run from the persisted progression.
 *
 * Falls back to the default class if the selected id is somehow unknown, so a
 * corrupted or stale save never blocks starting a run.
 */
fun MetaProgressionService.runStartConfig(progress: MetaProgression): RunStartConfig {
    val cls = StarterClassCatalog.byId(progress.selectedClassId) ?: StarterClassCatalog.default()
    return RunStartConfig(
        starterClass = cls,
        dice = cls.dice,
        bonusStartingMaterial = bonusStartingMaterial(progress),
        bonusStartingHp = bonusStartingHp(progress),
    )
}
