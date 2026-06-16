package com.koke1024.craftdice.domain.meta

/**
 * Catalog of every permanent upgrade the workshop sells.
 *
 * Upgrades are grouped by [UpgradeKind] for display. A class-unlock upgrade is
 * the single source of truth for "is this class available" — there is no
 * separate unlock flag, [StarterClass.isDefault] aside.
 */
object UpgradeCatalog {

    enum class UpgradeKind { MATERIAL, DICE, CLASS }

    private val materialBoost1 = PersistentUpgrade(
        id = "material_boost_1",
        displayName = "素材強化 I",
        description = "ラン開始時のクラフト素材を +5 する。",
        cost = 15,
        effect = UpgradeEffect.BonusStartingMaterial(5),
    )

    private val materialBoost2 = PersistentUpgrade(
        id = "material_boost_2",
        displayName = "素材強化 II",
        description = "ラン開始時のクラフト素材をさらに +10 する。",
        cost = 40,
        effect = UpgradeEffect.BonusStartingMaterial(10),
    )

    private val vitalityBoost = PersistentUpgrade(
        id = "vitality_boost",
        displayName = "強靭なる肉体",
        description = "プレイヤー全ユニットの初期 HP を +6 する。",
        cost = 30,
        effect = UpgradeEffect.BonusStartingHp(6),
    )

    private val unlockMage = PersistentUpgrade(
        id = "unlock_mage",
        displayName = "魔法使いの教導",
        description = "「魔法使い」の職業を解放する。",
        cost = 50,
        effect = UpgradeEffect.UnlockClass("mage"),
    )

    private val unlockGuardian = PersistentUpgrade(
        id = "unlock_guardian",
        displayName = "守護者の誓い",
        description = "「守護者」の職業を解放する。",
        cost = 80,
        effect = UpgradeEffect.UnlockClass("guardian"),
    )

    val all: List<PersistentUpgrade> = listOf(
        materialBoost1,
        materialBoost2,
        vitalityBoost,
        unlockMage,
        unlockGuardian,
    )

    fun byId(id: String): PersistentUpgrade? = all.firstOrNull { it.id == id }

    /** The class-unlock upgrade that gates [classId], if any. */
    fun classUnlockFor(classId: String): PersistentUpgrade? =
        all.firstOrNull {
            (it.effect as? UpgradeEffect.UnlockClass)?.classId == classId
        }

    fun kindOf(upgrade: PersistentUpgrade): UpgradeKind = when (upgrade.effect) {
        is UpgradeEffect.BonusStartingMaterial -> UpgradeKind.MATERIAL
        is UpgradeEffect.BonusStartingHp -> UpgradeKind.DICE
        is UpgradeEffect.UnlockClass -> UpgradeKind.CLASS
    }
}
