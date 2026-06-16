package com.koke1024.craftdice.domain.meta

import com.koke1024.craftdice.data.repository.MetaProgressMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class MetaProgressMapperTest {

    private val service = MetaProgressionService()

    @Test
    fun roundtrip_defaultProgression_isLossless() {
        val original = MetaProgression.DEFAULT

        val restored = saveAndReload(original)

        assertEquals(original, restored)
    }

    @Test
    fun roundtrip_progressionWithPurchasesAndShards_isLossless() {
        val base = MetaProgression(
            diceShards = 100,
            totalEarned = 250,
            purchasedUpgradeIds = emptySet(),
            selectedClassId = "warrior",
        )
        // purchase two upgrades so the purchased set and balance change
        val afterFirst = (service.purchase(base, "material_boost_1") as PurchaseResult.Success).progress
        val afterSecond = (service.purchase(afterFirst, "unlock_mage") as PurchaseResult.Success).progress

        val restored = saveAndReload(afterSecond)

        assertEquals(afterSecond, restored)
        assertEquals(setOf("material_boost_1", "unlock_mage"), restored.purchasedUpgradeIds)
        assertEquals(base.diceShards - 15 - 50, restored.diceShards)
    }

    @Test
    fun roundtrip_selectedClass_isPreserved() {
        val unlocked = (service.purchase(
            MetaProgression(diceShards = 200, totalEarned = 200, emptySet(), "warrior"),
            "unlock_guardian",
        ) as PurchaseResult.Success).progress
        val selected = (service.selectClass(unlocked, "guardian") as ClassSelectResult.Success).progress

        val restored = saveAndReload(selected)

        assertEquals("guardian", restored.selectedClassId)
        assertEquals(selected, restored)
    }

    @Test
    fun roundtrip_largeShardCount_survivesIntLongConversion() {
        val original = MetaProgression(
            diceShards = 99999,
            totalEarned = 99999,
            purchasedUpgradeIds = setOf("material_boost_1"),
            selectedClassId = "warrior",
        )

        assertEquals(original, saveAndReload(original))
    }

    /**
     * Mirrors what [com.koke1024.craftdice.data.repository.SqlDelightMetaProgressRepository]
     * does on save then load: extract the scalar fields the way save writes
     * them (Int->Long, Set->List) and feed them back through the mapper the way
     * load reads them. A lossless roundtrip proves the persistence layer cannot
     * corrupt the domain state.
     */
    private fun saveAndReload(progress: MetaProgression): MetaProgression {
        val savedDiceShards: Long = progress.diceShards.toLong()
        val savedTotalEarned: Long = progress.totalEarned.toLong()
        val savedSelectedClassId: String = progress.selectedClassId
        val savedUpgradeIds: List<String> = progress.purchasedUpgradeIds.toList()

        return MetaProgressMapper.toDomain(
            diceShards = savedDiceShards,
            totalEarned = savedTotalEarned,
            selectedClassId = savedSelectedClassId,
            purchasedUpgradeIds = savedUpgradeIds,
        )
    }
}
