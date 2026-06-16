package com.koke1024.craftdice.domain.meta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MetaProgressionServiceTest {

    private val service = MetaProgressionService()

    private fun progress(shards: Int = 100) = MetaProgression(
        diceShards = shards,
        totalEarned = shards,
        purchasedUpgradeIds = emptySet(),
        selectedClassId = StarterClassCatalog.DEFAULT_ID,
    )

    // --- purchase ---

    @Test
    fun purchase_success_deductsShardsAndRecordsOwnership() {
        val result = service.purchase(progress(100), "material_boost_1")

        require(result is PurchaseResult.Success)
        assertEquals(85, result.progress.diceShards)
        assertEquals(100, result.progress.totalEarned)
        assertTrue("material_boost_1" in result.progress.purchasedUpgradeIds)
        assertEquals("material_boost_1", result.upgrade.id)
    }

    @Test
    fun purchase_insufficientFunds_leavesProgressUntouched() {
        val original = progress(10)

        val result = service.purchase(original, "material_boost_1")

        assertEquals(PurchaseResult.InsufficientFunds, result)
    }

    @Test
    fun purchase_alreadyOwned_returnsAlreadyOwned() {
        val owned = progress(100).copy(purchasedUpgradeIds = setOf("material_boost_1"))

        val result = service.purchase(owned, "material_boost_1")

        assertEquals(PurchaseResult.AlreadyOwned, result)
    }

    @Test
    fun purchase_unknownId_returnsNotFound() {
        assertEquals(PurchaseResult.NotFound, service.purchase(progress(100), "does_not_exist"))
    }

    @Test
    fun purchase_twoUpgrades_stacksMaterialBonus() {
        val afterFirst = (service.purchase(progress(100), "material_boost_1") as PurchaseResult.Success).progress
        val afterSecond = (service.purchase(afterFirst, "material_boost_2") as PurchaseResult.Success).progress

        assertEquals(5 + 10, service.bonusStartingMaterial(afterSecond))
    }

    @Test
    fun purchase_vitalityBoost_addsBonusHp() {
        val afterPurchase = (service.purchase(progress(100), "vitality_boost") as PurchaseResult.Success).progress

        assertEquals(6, service.bonusStartingHp(afterPurchase))
    }

    @Test
    fun purchase_unlocksClass_makesClassAvailable() {
        val afterPurchase = (service.purchase(progress(100), "unlock_mage") as PurchaseResult.Success).progress

        assertTrue(service.isClassUnlocked(afterPurchase, "mage"))
    }

    // --- grantShards ---

    @Test
    fun grantShards_increasesBothBalanceAndLifetimeTotal() {
        val granted = service.grantShards(progress(30), 20)

        assertEquals(50, granted.diceShards)
        assertEquals(50, granted.totalEarned)
    }

    @Test
    fun grantShards_zeroAmount_isNoOp() {
        val original = progress(30)

        assertEquals(original, service.grantShards(original, 0))
    }

    @Test
    fun grantShards_thenPurchase_balanceDecreasesButTotalDoesNot() {
        val granted = service.grantShards(progress(0), 30)
        val afterPurchase = (service.purchase(granted, "material_boost_1") as PurchaseResult.Success).progress

        assertEquals(15, afterPurchase.diceShards)
        assertEquals(30, afterPurchase.totalEarned)
    }

    // --- class selection ---

    @Test
    fun selectClass_default_isAlwaysAvailable() {
        val result = service.selectClass(progress(0), StarterClassCatalog.DEFAULT_ID)

        require(result is ClassSelectResult.Success)
        assertEquals(StarterClassCatalog.DEFAULT_ID, result.progress.selectedClassId)
    }

    @Test
    fun selectClass_lockedClass_isRejected() {
        val result = service.selectClass(progress(0), "mage")

        assertEquals(ClassSelectResult.Locked, result)
    }

    @Test
    fun selectClass_unlockedClass_succeeds() {
        val unlocked = (service.purchase(progress(100), "unlock_mage") as PurchaseResult.Success).progress

        val result = service.selectClass(unlocked, "mage")

        require(result is ClassSelectResult.Success)
        assertEquals("mage", result.progress.selectedClassId)
    }

    @Test
    fun selectClass_unknownId_returnsNotFound() {
        assertEquals(ClassSelectResult.NotFound, service.selectClass(progress(0), "ninja"))
    }

    // --- derived helpers ---

    @Test
    fun isClassUnlocked_defaultClass_isTrueOnFreshSave() {
        assertTrue(service.isClassUnlocked(MetaProgression.DEFAULT, StarterClassCatalog.DEFAULT_ID))
    }

    @Test
    fun unlockedClassIds_onFreshSave_containsOnlyDefault() {
        assertEquals(setOf(StarterClassCatalog.DEFAULT_ID), service.unlockedClassIds(MetaProgression.DEFAULT))
    }

    @Test
    fun bonusStartingMaterial_onFreshSave_isZero() {
        assertEquals(0, service.bonusStartingMaterial(MetaProgression.DEFAULT))
    }

    @Test
    fun bonusStartingHp_onFreshSave_isZero() {
        assertEquals(0, service.bonusStartingHp(MetaProgression.DEFAULT))
    }

    @Test
    fun runStartConfig_usesSelectedClassLoadout() {
        val unlocked = (service.purchase(progress(100), "unlock_guardian") as PurchaseResult.Success).progress
        val selected = (service.selectClass(unlocked, "guardian") as ClassSelectResult.Success).progress

        val config = service.runStartConfig(selected)

        assertEquals("guardian", config.starterClass.id)
        assertEquals(selected.selectedClassId, config.starterClass.id)
        assertNotNull(config.dice)
    }
}
