package com.koke1024.craftdice.domain.battle.facedamage

import com.koke1024.craftdice.domain.battle.model.BattleConfig
import com.koke1024.craftdice.domain.battle.model.BattleUnit

/**
 * Applies the Phase 3 "face destruction" system.
 *
 * When a unit takes damage, its dice faces shatter: HP is reduced and one or
 * more intact faces are marked broken. A broken face, if rolled later, causes
 * the action to fail and inflicts self-damage.
 *
 * Face selection is random over the remaining intact faces; inject a fixed
 * [random] provider for deterministic tests.
 */
class FaceDamageSystem(
    private val facesPerHit: Int = BattleConfig.FACE_DAMAGE_PER_HIT,
    private val random: () -> Double = { kotlin.random.Random.nextDouble() },
) {
    /**
     * Picks an intact face index to break, or null if every face is shattered.
     */
    fun selectFaceToBreak(unit: BattleUnit): Int? {
        val intact = (0 until unit.dice.faceCount).filter { it !in unit.brokenFaceIndices }
        if (intact.isEmpty()) return null
        val index = (random() * intact.size).toInt().coerceIn(0, intact.lastIndex)
        return intact[index]
    }

    /**
     * Reduces the unit's HP by [damage] and breaks [facesPerHit] intact faces.
     * A damage of 0 leaves the unit unchanged.
     */
    fun applyDamage(unit: BattleUnit, damage: Int): BattleUnit {
        if (damage <= 0) return unit
        var result = unit.withDamage(damage)
        var broken = 0
        while (broken < facesPerHit) {
            val face = selectFaceToBreak(result) ?: break
            result = result.withBrokenFace(face)
            broken++
        }
        return result
    }
}
