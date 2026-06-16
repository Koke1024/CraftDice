package com.koke1024.craftdice.domain.roguelike

import com.koke1024.craftdice.domain.battle.model.BattleStatus
import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.roguelike.event.EventCatalog
import com.koke1024.craftdice.domain.roguelike.event.EventEffect
import com.koke1024.craftdice.domain.roguelike.event.EventOutcome
import com.koke1024.craftdice.domain.roguelike.event.EventResolver
import com.koke1024.craftdice.domain.roguelike.generation.DungeonGenerationConfig
import com.koke1024.craftdice.domain.roguelike.generation.DungeonGenerator
import com.koke1024.craftdice.domain.roguelike.meta.MetaCurrencyCalculator
import com.koke1024.craftdice.domain.roguelike.model.BattleSetup
import com.koke1024.craftdice.domain.roguelike.model.CombatSummary
import com.koke1024.craftdice.domain.roguelike.model.FloorNode
import com.koke1024.craftdice.domain.roguelike.model.RunOutcome
import com.koke1024.craftdice.domain.roguelike.model.RunState
import com.koke1024.craftdice.domain.roguelike.model.RunStatus
import com.koke1024.craftdice.domain.roguelike.model.Reward
import com.koke1024.craftdice.domain.roguelike.model.RewardSource
import com.koke1024.craftdice.domain.roguelike.model.RoomType
import com.koke1024.craftdice.domain.roguelike.reward.DropTables
import com.koke1024.craftdice.domain.roguelike.reward.RewardRoller
import kotlin.random.Random

/**
 * Result of resolving an event choice, exposing both the narrated [outcome]
 * and the resulting [state] so the UI can render the consequence.
 */
data class EventResolution(
    val outcome: EventOutcome,
    val state: RunState,
)

/**
 * Orchestrates a single roguelike run as a pure state machine.
 *
 * Owns the [RunState] and advances it through the four room kinds. Combat is
 * delegated: the caller reads [currentBattleSetup], drives the Phase 3
 * [com.koke1024.craftdice.domain.battle.BattleEngine], and feeds the
 * [CombatSummary] back via [applyCombatResult]. This keeps the run engine free
 * of physics/tray concerns and fully unit-testable.
 *
 * All randomness flows from the run seed through one [Random], so a run is
 * reproducible end to end.
 */
class RunEngine(
    private val dungeonGenerator: DungeonGenerator,
    private val rewardRoller: RewardRoller,
    private val eventResolver: EventResolver,
    private val randomFactory: (Long) -> Random = { Random(it) },
) {
    lateinit var state: RunState
        private set

    private var random: Random = Random.Default

    fun startRun(
        seed: Long,
        playerDice: List<Dice>,
        config: DungeonGenerationConfig = DungeonGenerationConfig(),
    ): RunState {
        random = randomFactory(seed)
        val map = dungeonGenerator.generate(seed, config)
        state = RunState.start(seed, playerDice, map)
        return state
    }

    fun availableMoves(): List<FloorNode> = state.map.nextChoices(state.currentRoomId)

    fun moveTo(roomId: Int): RunState {
        check(!state.isFinished) { "Cannot move after the run has ended" }
        state = state.moveTo(roomId)
        return state
    }

    /**
     * Builds the inputs the battle layer needs for the current combat room.
     */
    fun currentBattleSetup(): BattleSetup {
        val room = state.currentRoom
        val enemy = room.enemy ?: error("Room ${room.id} (${room.type}) has no enemy")
        return BattleSetup(
            playerUnits = state.playerUnits,
            enemyUnits = listOf(enemy.toUnit(ENEMY_ID_BASE)),
        )
    }

    /**
     * Consumes a resolved [CombatSummary]: syncs survivors, rolls loot on a
     * win (tier-scaled), clears the room, and ends the run on a loss or a
     * boss kill.
     */
    fun applyCombatResult(summary: CombatSummary): RunState {
        check(!state.isFinished) { "Run already finished" }
        var next = state.syncAfterCombat(summary)
        if (summary.playerWon) {
            val tier = summary.defeatedTemplate?.rewardTier ?: 0
            val rewards = rewardRoller.roll(
                DropTables.forEnemyTier(tier),
                random,
                sourceForRoom(state.currentRoom),
            )
            next = next.addRewards(rewards).clearCurrentRoom()
        } else {
            next = next.defeat()
        }
        state = next
        return next
    }

    /**
     * Resolves an event choice: rolls the outcome, applies its effects, and
     * clears the room (or ends the run if the effects wiped the party).
     */
    fun resolveEvent(choiceIndex: Int): EventResolution {
        check(!state.isFinished) { "Run already finished" }
        val room = state.currentRoom
        val eventId = room.eventId ?: error("Room ${room.id} is not an event room")
        val event = EventCatalog.byId(eventId)
        val choice = event.choices[choiceIndex]
        val outcome = eventResolver.resolve(choice, random)
        val afterEffects = applyEventEffects(state, outcome.effects)
        val cleared = if (afterEffects.isPlayerDefeated) {
            afterEffects.defeat()
        } else {
            afterEffects.clearCurrentRoom()
        }
        state = cleared
        return EventResolution(outcome, cleared)
    }

    /** Claims the loot of a REWARD room and clears it. */
    fun claimReward(): RunState {
        check(!state.isFinished) { "Run already finished" }
        val room = state.currentRoom
        check(room.type == RoomType.REWARD) { "Room ${room.id} is not a reward room" }
        val rewards = rewardRoller.roll(DropTables.regular, random, RewardSource.COMBAT)
        state = state.addRewards(rewards).clearCurrentRoom()
        return state
    }

    /** Fully restores the party at a REST room and clears it. */
    fun rest(): RunState {
        check(!state.isFinished) { "Run already finished" }
        val room = state.currentRoom
        check(room.type == RoomType.REST) { "Room ${room.id} is not a rest room" }
        state = state.healAllToFull().clearCurrentRoom()
        return state
    }

    /**
     * Finalises the run, computing the meta currency brought home. Requires
     * the run to be in a terminal state ([RunStatus.VICTORY] or [DEFEAT]).
     */
    fun finish(): RunOutcome {
        val status = state.status
        check(status.isFinished) { "Cannot finish a run that is still ONGOING" }
        val currency = MetaCurrencyCalculator.forOutcome(state, status)
        return when (status) {
            RunStatus.VICTORY -> RunOutcome.Victory(currency, state.roomsCleared, state.inventory)
            RunStatus.DEFEAT -> RunOutcome.Defeat(currency, state.roomsCleared, state.inventory)
            RunStatus.ONGOING -> error("unreachable")
        }
    }

    private fun applyEventEffects(current: RunState, effects: List<EventEffect>): RunState {
        var s = current
        effects.forEach { effect ->
            s = when (effect) {
                is EventEffect.Heal -> s.copy(
                    playerUnits = s.playerUnits.map { it.withHeal(effect.amount) },
                )
                is EventEffect.Damage -> s.copy(
                    playerUnits = s.playerUnits.map { it.withDamage(effect.amount) },
                )
                EventEffect.FullHeal -> s.healAllToFull()
                is EventEffect.GainReward -> s.addRewards(listOf(effect.reward))
                is EventEffect.LoseDiceMaterial -> s.removeDiceMaterial(effect.amount)
                is EventEffect.GainMetaCurrency -> s.addRewards(
                    listOf(Reward.MetaCurrency(effect.amount, RewardSource.EVENT)),
                )
            }
        }
        return s
    }

    private fun sourceForRoom(room: FloorNode): RewardSource = when (room.type) {
        RoomType.BOSS -> RewardSource.BOSS
        RoomType.ELITE_COMBAT -> RewardSource.ELITE
        else -> RewardSource.COMBAT
    }

    private companion object {
        const val ENEMY_ID_BASE = 1000
    }
}
