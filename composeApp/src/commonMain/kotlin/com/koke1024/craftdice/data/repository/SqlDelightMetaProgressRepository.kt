package com.koke1024.craftdice.data.repository

import com.koke1024.craftdice.data.local.CraftDiceDatabase
import com.koke1024.craftdice.domain.meta.MetaProgression

/**
 * SQLDelight-backed [MetaProgressRepository].
 *
 * The scalar fields (shards, lifetime total, selected class) live in the
 * single-row `meta_progress` table; purchased upgrades are the `purchased_upgrade`
 * set. Because the whole [MetaProgression] is the unit of change, [save]
 * rewrites the upgrade rows within a transaction (delete-all + re-insert),
 * which is cheap for the small catalog and keeps the table an exact mirror of
 * the domain set.
 */
class SqlDelightMetaProgressRepository(
    private val database: CraftDiceDatabase,
) : MetaProgressRepository {

    private val queries get() = database.commonQueries

    override fun load(): MetaProgression {
        val row = queries.selectMetaProgress().executeAsOneOrNull()
            ?: return MetaProgression.DEFAULT

        val upgradeIds = queries.selectAllPurchasedUpgrades().executeAsList()
        return MetaProgressMapper.toDomain(
            diceShards = row.dice_shards,
            totalEarned = row.total_earned,
            selectedClassId = row.selected_class_id,
            purchasedUpgradeIds = upgradeIds,
        )
    }

    override fun save(progress: MetaProgression) {
        database.transaction {
            queries.upsertMetaProgress(
                id = ROW_ID,
                dice_shards = progress.diceShards.toLong(),
                total_earned = progress.totalEarned.toLong(),
                selected_class_id = progress.selectedClassId,
            )
            queries.deleteAllPurchasedUpgrades()
            progress.purchasedUpgradeIds.forEach { id ->
                queries.insertPurchasedUpgrade(id)
            }
        }
    }

    private companion object {
        const val ROW_ID = 1L
    }
}
