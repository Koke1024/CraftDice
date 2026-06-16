package com.koke1024.craftdice.domain.roguelike.reward

import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.model.Rarity
import com.koke1024.craftdice.domain.roguelike.model.Reward
import com.koke1024.craftdice.domain.roguelike.model.RewardSource

/**
 * Standard loot tables keyed by enemy [EnemyTemplate.rewardTier].
 *
 * The difficulty curve is intentionally gentle for Phase 4: regular enemies
 * mostly drop crafting material with a small chance of a common face, elites
 * reliably drop a face, and the boss guarantees meta currency plus a strong
 * face. Faces are chosen so that they can be plugged straight into the Phase 1
 * [com.koke1024.craftdice.domain.craft.DiceCraftService] (addFace).
 */
object DropTables {

    private val commonFaces = listOf(
        DiceFace.attack(2, Rarity.COMMON),
        DiceFace.attack(3, Rarity.COMMON),
        DiceFace.defense(Rarity.COMMON),
        DiceFace.heal(2, Rarity.COMMON),
    )

    private val rareFaces = listOf(
        DiceFace.attack(4, Rarity.RARE),
        DiceFace.attack(5, Rarity.RARE),
        DiceFace.critical(5, Rarity.RARE),
        DiceFace.heal(3, Rarity.RARE),
    )

    private val epicFaces = listOf(
        DiceFace.critical(7, Rarity.EPIC),
        DiceFace.critical(8, Rarity.EPIC),
        DiceFace.heal(5, Rarity.EPIC),
    )

    val regular: DropTable = DropTable(
        entries = buildList {
            add(DropEntry(Reward.DiceMaterial(amount = 1), weight = 50))
            commonFaces.forEach { face ->
                add(DropEntry(Reward.FaceFragment(face), weight = 20))
            }
            add(DropEntry(Reward.MetaCurrency(amount = 1), weight = 10))
        },
        rolls = 1,
    )

    val elite: DropTable = DropTable(
        entries = buildList {
            add(DropEntry(Reward.DiceMaterial(amount = 2), weight = 30))
            commonFaces.forEach { face ->
                add(DropEntry(Reward.FaceFragment(face), weight = 15))
            }
            rareFaces.forEach { face ->
                add(DropEntry(Reward.FaceFragment(face), weight = 25))
            }
            add(DropEntry(Reward.MetaCurrency(amount = 2), weight = 20))
        },
        rolls = 2,
    )

    val boss: DropTable = DropTable(
        entries = buildList {
            add(DropEntry(Reward.DiceMaterial(amount = 3), weight = 30))
            rareFaces.forEach { face ->
                add(DropEntry(Reward.FaceFragment(face), weight = 30))
            }
            epicFaces.forEach { face ->
                add(DropEntry(Reward.FaceFragment(face), weight = 40))
            }
        },
        rolls = 2,
        guaranteed = listOf(
            Reward.MetaCurrency(amount = 8, source = RewardSource.BOSS),
            Reward.FaceFragment(DiceFace.critical(8, Rarity.LEGENDARY), source = RewardSource.BOSS),
        ),
    )

    fun forEnemyTier(tier: Int): DropTable = when {
        tier >= 3 -> boss
        tier >= 1 -> elite
        else -> regular
    }
}
