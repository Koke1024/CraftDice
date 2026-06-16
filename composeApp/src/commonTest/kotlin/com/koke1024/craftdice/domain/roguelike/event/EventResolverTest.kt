package com.koke1024.craftdice.domain.roguelike.event

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventResolverTest {

    private val resolver = EventResolver()

    private fun fixedRandom(value: Double): Random = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = value
    }

    private val goodOutcome = EventOutcome(
        weight = 1.0,
        effects = listOf(EventEffect.GainMetaCurrency(amount = 5)),
        message = "win",
    )
    private val badOutcome = EventOutcome(
        weight = 1.0,
        effects = listOf(EventEffect.Damage(amount = 10)),
        message = "lose",
    )

    @Test
    fun resolve_singleOutcomeAlwaysReturned() {
        val choice = EventChoice("act", listOf(goodOutcome))

        repeat(10) {
            assertEquals(goodOutcome, resolver.resolve(choice, fixedRandom(0.99)))
        }
    }

    @Test
    fun resolve_isReproducibleForSameRandom() {
        val choice = EventChoice("act", listOf(goodOutcome, badOutcome))

        val a = resolver.resolve(choice, Random(5))
        val b = resolver.resolve(choice, Random(5))

        assertEquals(a, b)
    }

    @Test
    fun resolve_lowRollPicksFirstOutcome() {
        val choice = EventChoice("act", listOf(goodOutcome, badOutcome))

        assertEquals(goodOutcome, resolver.resolve(choice, fixedRandom(0.0)))
    }

    @Test
    fun resolve_highRollPicksSecondOutcome() {
        val choice = EventChoice("act", listOf(goodOutcome, badOutcome))

        assertEquals(badOutcome, resolver.resolve(choice, fixedRandom(0.99)))
    }

    @Test
    fun resolve_zeroWeightOutcomeNeverPicked() {
        val weightedGood = goodOutcome.copy(weight = 0.0)
        val choice = EventChoice("act", listOf(weightedGood, badOutcome))

        repeat(20) {
            assertEquals(badOutcome, resolver.resolve(choice, Random(it)))
        }
    }

    @Test
    fun resolve_weightedDistributionFavoursHeavierOutcome() {
        val heavy = goodOutcome.copy(weight = 99.0)
        val light = badOutcome.copy(weight = 1.0)
        val choice = EventChoice("act", listOf(heavy, light))

        val wins = (1..200).count { resolver.resolve(choice, Random(it)) == heavy }

        assertTrue(wins > 170, "Heavier outcome should dominate, got $wins/200")
    }

    @Test
    fun resolve_eventCatalogExposesKnownEvents() {
        val ids = EventCatalog.all.map { it.id }

        assertTrue("devil_gambler" in ids)
        assertTrue("fate_roulette" in ids)
        assertTrue("mysterious_chest" in ids)
        assertEquals("devil_gambler", EventCatalog.byId("devil_gambler").id)
    }

    @Test
    fun resolve_devilGamblerHasGambleAndLeaveChoices() {
        val event = EventCatalog.byId("devil_gambler")

        assertEquals(2, event.choices.size)
        assertTrue(event.choices.first().outcomes.size >= 2)
        val leave = event.choices.last()
        assertEquals(1, leave.outcomes.size)
        assertTrue(leave.outcomes.first().effects.isEmpty())
    }
}
