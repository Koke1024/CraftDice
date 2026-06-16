package com.koke1024.craftdice.domain.roguelike.reward

import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.roguelike.model.Reward
import com.koke1024.craftdice.domain.roguelike.model.RewardSource
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RewardRollerTest {

    private fun deterministicRandom(values: List<Int>): Random = object : Random() {
        private val queue = ArrayDeque(values)
        override fun nextInt(until: Int): Int {
            val v = queue.removeFirstOrNull() ?: (until - 1)
            return v.coerceIn(0, until - 1)
        }
        override fun nextBits(bitCount: Int): Int = nextInt(Int.MAX_VALUE)
    }

    private val material = Reward.DiceMaterial(amount = 1)
    private val fragment = Reward.FaceFragment(DiceFace.attack(3))

    @Test
    fun roll_returnsRollsEntriesPlusGuaranteed() {
        val table = DropTable(
            entries = listOf(
                DropEntry(material, weight = 1),
            ),
            rolls = 2,
            guaranteed = listOf(Reward.MetaCurrency(amount = 5)),
        )

        val result = RewardRoller().roll(table, deterministicRandom(listOf(0, 0)))

        assertEquals(3, result.size)
        assertEquals(5, result.last().let { it as Reward.MetaCurrency }.amount)
    }

    @Test
    fun roll_emptyTableReturnsOnlyGuaranteed() {
        val table = DropTable(entries = emptyList(), rolls = 3, guaranteed = listOf(material))

        val result = RewardRoller().roll(table, deterministicRandom(listOf()))

        assertEquals(listOf(material), result)
    }

    @Test
    fun roll_singleEntryAlwaysPicked() {
        val table = DropTable(entries = listOf(DropEntry(fragment, weight = 10)), rolls = 4)

        val result = RewardRoller().roll(table, deterministicRandom(listOf(0, 0, 0, 0)))

        assertEquals(4, result.size)
        result.forEach { assertEquals(fragment, it) }
    }

    @Test
    fun roll_zeroWeightEntryNeverPicked() {
        val table = DropTable(
            entries = listOf(
                DropEntry(material, weight = 0),
                DropEntry(fragment, weight = 10),
            ),
            rolls = 20,
        )

        val result = RewardRoller().roll(table, deterministicRandom(List(20) { 9 }))

        result.forEach { assertEquals(fragment, it) }
    }

    @Test
    fun roll_weightedDistributionFavoursHeavierEntries() {
        val table = DropTable(
            entries = listOf(
                DropEntry(material, weight = 1),
                DropEntry(fragment, weight = 99),
            ),
            rolls = 200,
        )

        val result = RewardRoller().roll(table, Random(42))

        val fragments = result.count { it is Reward.FaceFragment }
        assertTrue(
            fragments > 150,
            "Heavier entry should dominate, got $fragments/200 fragments",
        )
    }

    @Test
    fun roll_isReproducibleForSameRandomSequence() {
        val table = DropTable(
            entries = listOf(
                DropEntry(material, weight = 5),
                DropEntry(fragment, weight = 5),
            ),
            rolls = 5,
        )
        val roller = RewardRoller()

        val a = roller.roll(table, Random(7))
        val b = roller.roll(table, Random(7))

        assertEquals(a, b)
    }

    @Test
    fun roll_assignsSourceToRolledRewards() {
        val table = DropTable(
            entries = listOf(DropEntry(material, weight = 1)),
            rolls = 1,
        )

        val result = RewardRoller().roll(table, deterministicRandom(listOf(0)), RewardSource.ELITE)

        assertEquals(RewardSource.ELITE, (result.first() as Reward.DiceMaterial).source)
    }
}
