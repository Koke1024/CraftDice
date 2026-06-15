package com.koke1024.craftdice.domain.battle.model

import com.koke1024.craftdice.domain.model.Dice

/**
 * A combatant unit in battle.
 *
 * Each unit binds a craftable [Dice] to runtime combat state: HP and the set
 * of shattered face indices. Taking damage both reduces HP and breaks faces
 * (the Phase 3 "face destruction" system); rolling a broken face causes the
 * action to fail and inflicts self-damage.
 *
 * Immutable: all state transitions return new instances.
 */
data class BattleUnit(
    val id: Int,
    val owner: BattleSide,
    val name: String,
    val dice: Dice,
    val maxHp: Int,
    val currentHp: Int,
    val brokenFaceIndices: Set<Int> = emptySet(),
    val isDefending: Boolean = false,
) {
    val isAlive: Boolean get() = currentHp > 0

    val intactFaceCount: Int get() = dice.faceCount - brokenFaceIndices.size

    val isShattered: Boolean get() = intactFaceCount <= 0

    val hpRatio: Double get() = if (maxHp <= 0) 0.0 else currentHp.toDouble() / maxHp

    fun isFaceBroken(index: Int): Boolean = index in brokenFaceIndices

    fun withDamage(damage: Int): BattleUnit {
        if (damage <= 0) return this
        val newHp = (currentHp - damage).coerceAtLeast(0)
        return copy(currentHp = newHp)
    }

    fun withHeal(amount: Int): BattleUnit {
        if (amount <= 0) return this
        val newHp = (currentHp + amount).coerceAtMost(maxHp)
        return copy(currentHp = newHp)
    }

    fun withBrokenFace(index: Int): BattleUnit {
        if (index !in 0 until dice.faceCount) return this
        if (index in brokenFaceIndices) return this
        return copy(brokenFaceIndices = brokenFaceIndices + index)
    }

    fun defending(): BattleUnit = copy(isDefending = true)

    fun clearDefending(): BattleUnit = copy(isDefending = false)

    companion object {
        fun fromDice(
            id: Int,
            owner: BattleSide,
            name: String,
            dice: Dice,
            hp: Int = BattleConfig.DEFAULT_HP,
        ): BattleUnit = BattleUnit(id, owner, name, dice, hp, hp)
    }
}
