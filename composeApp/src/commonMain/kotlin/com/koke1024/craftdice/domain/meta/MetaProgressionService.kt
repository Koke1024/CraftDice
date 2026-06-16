package com.koke1024.craftdice.domain.meta

/**
 * Pure state machine for meta-progression transitions.
 *
 * Holds no state of its own: every method takes the current [MetaProgression]
 * and returns either the next one (for the repository to persist) or a typed
 * result. This keeps the upgrade economy fully unit-testable without a
 * database, and lets the UI layer treat purchasing as a request/response.
 */
class MetaProgressionService {

    /** Attempts to buy [upgradeId]; on success deducts shards and records ownership. */
    fun purchase(progress: MetaProgression, upgradeId: String): PurchaseResult {
        val upgrade = UpgradeCatalog.byId(upgradeId) ?: return PurchaseResult.NotFound
        if (upgradeId in progress.purchasedUpgradeIds) return PurchaseResult.AlreadyOwned
        if (progress.diceShards < upgrade.cost) return PurchaseResult.InsufficientFunds

        val updated = progress.copy(
            diceShards = progress.diceShards - upgrade.cost,
            purchasedUpgradeIds = progress.purchasedUpgradeIds + upgradeId,
        )
        return PurchaseResult.Success(updated, upgrade)
    }

    /**
     * Selects the starter class for the next run. Free and reversible, but the
     * class must be unlocked (default or via a purchased unlock upgrade).
     */
    fun selectClass(progress: MetaProgression, classId: String): ClassSelectResult {
        val cls = StarterClassCatalog.byId(classId) ?: return ClassSelectResult.NotFound
        if (!isClassUnlocked(progress, classId)) return ClassSelectResult.Locked
        return ClassSelectResult.Success(progress.copy(selectedClassId = classId), cls)
    }

    /**
     * Credits the dice shards a run brought home (the value computed by
     * [com.koke1024.craftdice.domain.roguelike.meta.MetaCurrencyCalculator]).
     * Both the spendable balance and the lifetime total increase.
     */
    fun grantShards(progress: MetaProgression, amount: Int): MetaProgression {
        require(amount >= 0) { "grant amount must be non-negative, got $amount" }
        if (amount == 0) return progress
        return progress.copy(
            diceShards = progress.diceShards + amount,
            totalEarned = progress.totalEarned + amount,
        )
    }

    // --- derived view of the persistent state, read at run-start time ---

    fun isClassUnlocked(progress: MetaProgression, classId: String): Boolean {
        val cls = StarterClassCatalog.byId(classId) ?: return false
        if (cls.isDefault) return true
        return progress.purchasedUpgradeIds.any { id ->
            (UpgradeCatalog.byId(id)?.effect as? UpgradeEffect.UnlockClass)?.classId == classId
        }
    }

    fun unlockedClassIds(progress: MetaProgression): Set<String> =
        StarterClassCatalog.all
            .filter { isClassUnlocked(progress, it.id) }
            .map { it.id }
            .toSet()

    fun bonusStartingMaterial(progress: MetaProgression): Int =
        progress.purchasedUpgradeIds.sumOf { id ->
            (UpgradeCatalog.byId(id)?.effect as? UpgradeEffect.BonusStartingMaterial)?.amount ?: 0
        }

    fun bonusStartingHp(progress: MetaProgression): Int =
        progress.purchasedUpgradeIds.sumOf { id ->
            (UpgradeCatalog.byId(id)?.effect as? UpgradeEffect.BonusStartingHp)?.amount ?: 0
        }
}

sealed interface ClassSelectResult {

    data class Success(
        val progress: MetaProgression,
        val selectedClass: StarterClass,
    ) : ClassSelectResult

    data object Locked : ClassSelectResult

    data object NotFound : ClassSelectResult
}
