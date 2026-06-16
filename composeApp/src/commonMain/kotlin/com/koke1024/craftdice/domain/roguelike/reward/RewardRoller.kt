package com.koke1024.craftdice.domain.roguelike.reward

import com.koke1024.craftdice.domain.roguelike.model.Reward
import com.koke1024.craftdice.domain.roguelike.model.RewardSource
import kotlin.random.Random

/**
 * Rolls a [DropTable] into a concrete list of [Reward]s.
 *
 * Stateless: the caller supplies the [Random] so the run engine can keep a
 * single seeded [Random] driving every roll in a run, which makes the entire
 * reward sequence reproducible from the seed.
 */
class RewardRoller {

    fun roll(
        table: DropTable,
        random: Random,
        source: RewardSource = RewardSource.COMBAT,
    ): List<Reward> {
        if (table.isEmpty) return emptyList()
        val rolled = (0 until table.rolls).mapNotNull { pick(table, random, source) }
        return rolled + table.guaranteed.map { it.withSource(source) }
    }

    private fun pick(
        table: DropTable,
        random: Random,
        source: RewardSource,
    ): Reward? {
        val entries = table.entries
        if (entries.isEmpty()) return null
        val totalWeight = entries.sumOf { it.weight }
        if (totalWeight <= 0) return null
        var roll = random.nextInt(totalWeight)
        for (entry in entries) {
            roll -= entry.weight
            if (roll < 0) return entry.reward.withSource(source)
        }
        return entries.last().reward.withSource(source)
    }

    private fun Reward.withSource(source: RewardSource): Reward = when (this) {
        is Reward.FaceFragment -> copy(source = source)
        is Reward.DiceMaterial -> copy(source = source)
        is Reward.MetaCurrency -> copy(source = source)
    }
}
