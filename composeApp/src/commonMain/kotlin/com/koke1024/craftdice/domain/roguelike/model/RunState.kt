package com.koke1024.craftdice.domain.roguelike.model

import com.koke1024.craftdice.domain.battle.model.BattleConfig
import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleUnit
import com.koke1024.craftdice.domain.model.Dice

/**
 * Immutable snapshot of an in-progress run.
 *
 * The player's combatants are modelled as a list of [BattleUnit]s so that HP
 * and broken faces persist across battles — a defining roguelike tension. The
 * run engine drives state transitions; this class only exposes pure copy
 * helpers so every transition is auditable and testable.
 */
data class RunState(
    val seed: Long,
    val playerUnits: List<BattleUnit>,
    val inventory: List<Reward>,
    val map: DungeonMap,
    val currentRoomId: Int,
    val status: RunStatus,
    val enemiesDefeated: Int,
    val roomsCleared: Int,
) {
    init {
        require(playerUnits.all { it.owner == BattleSide.PLAYER1 }) {
            "All player units must belong to PLAYER1"
        }
    }

    val isFinished: Boolean get() = status.isFinished

    val alivePlayerUnits: List<BattleUnit> get() = playerUnits.filter { it.isAlive }

    val isPlayerDefeated: Boolean get() = alivePlayerUnits.isEmpty()

    val currentRoom: FloorNode get() = map.node(currentRoomId)

    fun totalMetaCurrency(): Int =
        inventory.filterIsInstance<Reward.MetaCurrency>().sumOf { it.amount }

    fun totalDiceMaterial(): Int =
        inventory.filterIsInstance<Reward.DiceMaterial>().sumOf { it.amount }

    fun collectedFaces(): List<com.koke1024.craftdice.domain.model.DiceFace> =
        inventory.filterIsInstance<Reward.FaceFragment>().map { it.face }

    fun moveTo(roomId: Int): RunState {
        require(roomId in currentRoom.nextNodeIds || roomId == currentRoomId) {
            "Room $roomId is not reachable from current room $currentRoomId"
        }
        return copy(currentRoomId = roomId)
    }

    /**
     * Replaces the player roster with the post-combat survivors, keeping HP
     * and broken faces persistent. An empty survivor list leaves the roster as
     * is; the run engine marks the run as [RunStatus.DEFEAT] via [defeat].
     */
    fun syncAfterCombat(summary: CombatSummary): RunState = copy(
        playerUnits = if (summary.survivingPlayerUnits.isEmpty()) {
            playerUnits
        } else {
            summary.survivingPlayerUnits
        },
        enemiesDefeated = enemiesDefeated + (if (summary.playerWon) 1 else 0),
    )

    fun addRewards(rewards: List<Reward>): RunState = copy(inventory = inventory + rewards)

    /**
     * Removes up to [amount] dice material from the inventory, draining the
     * earliest [Reward.DiceMaterial] entries first and dropping any that hit
     * zero. Used by risk events that cost materials.
     */
    fun removeDiceMaterial(amount: Int): RunState {
        if (amount <= 0) return this
        var remaining = amount
        val newInventory = inventory.mapNotNull { reward ->
            if (reward is Reward.DiceMaterial && remaining > 0) {
                val take = minOf(reward.amount, remaining)
                remaining -= take
                val leftover = reward.amount - take
                if (leftover <= 0) null else reward.copy(amount = leftover)
            } else {
                reward
            }
        }
        return copy(inventory = newInventory)
    }

    fun clearCurrentRoom(): RunState {
        val room = currentRoom
        val clearedMap = map.withNode(room.clear())
        val finished = room.type == RoomType.BOSS
        return copy(
            map = clearedMap,
            roomsCleared = roomsCleared + 1,
            status = if (finished) RunStatus.VICTORY else status,
        )
    }

    fun healAllToFull(): RunState = copy(
        playerUnits = playerUnits.map { unit ->
            unit.copy(
                currentHp = unit.maxHp,
                brokenFaceIndices = emptySet(),
            )
        },
    )

    fun defeat(): RunState = copy(status = RunStatus.DEFEAT)

    companion object {
        fun start(
            seed: Long,
            playerDice: List<Dice>,
            map: DungeonMap,
            hpPerUnit: Int = BattleConfig.DEFAULT_HP,
        ): RunState {
            require(playerDice.isNotEmpty()) { "A run requires at least one player dice" }
            val units = playerDice.mapIndexed { index, dice ->
                BattleUnit.fromDice(
                    id = index + 1,
                    owner = BattleSide.PLAYER1,
                    name = "Player ${index + 1}",
                    dice = dice,
                    hp = hpPerUnit,
                )
            }
            return RunState(
                seed = seed,
                playerUnits = units,
                inventory = emptyList(),
                map = map,
                currentRoomId = map.startNodeId,
                status = RunStatus.ONGOING,
                enemiesDefeated = 0,
                roomsCleared = 0,
            )
        }
    }
}
