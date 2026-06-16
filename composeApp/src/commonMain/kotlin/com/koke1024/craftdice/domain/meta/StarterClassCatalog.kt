package com.koke1024.craftdice.domain.meta

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace

/**
 * Catalog of playable starter classes.
 *
 * The [DEFAULT_ID] class is unlocked on a fresh save; every other class is
 * unlocked by purchasing the matching [PersistentUpgrade] (see
 * [UpgradeCatalog.classUnlockFor]).
 */
object StarterClassCatalog {

    const val DEFAULT_ID = "warrior"

    private val warrior = StarterClass(
        id = DEFAULT_ID,
        displayName = "戦士",
        description = "攻撃と防御のバランス型。初心者におすすめ。",
        dice = listOf(
            Dice.of(
                DiceFace.attack(4),
                DiceFace.attack(4),
                DiceFace.defense(),
                DiceFace.heal(3),
                DiceFace.critical(6),
                DiceFace.miss(),
            ),
        ),
        isDefault = true,
    )

    private val mage = StarterClass(
        id = "mage",
        displayName = "魔法使い",
        description = "高火力の必殺と回復に特化。打たれ弱い運用になる。",
        dice = listOf(
            Dice.of(
                DiceFace.attack(3),
                DiceFace.heal(5),
                DiceFace.heal(5),
                DiceFace.critical(8),
                DiceFace.critical(8),
                DiceFace.miss(),
            ),
        ),
    )

    private val guardian = StarterClass(
        id = "guardian",
        displayName = "守護者",
        description = "防御と持久戦のエキスパート。長期的な勝負に強い。",
        dice = listOf(
            Dice.of(
                DiceFace.attack(3),
                DiceFace.defense(),
                DiceFace.defense(),
                DiceFace.defense(),
                DiceFace.heal(4),
                DiceFace.attack(3),
            ),
        ),
    )

    val all: List<StarterClass> = listOf(warrior, mage, guardian)

    fun byId(id: String): StarterClass? = all.firstOrNull { it.id == id }

    fun default(): StarterClass = warrior
}
