package com.koke1024.craftdice.ui.dungeon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koke1024.craftdice.data.repository.MetaProgressRepository
import com.koke1024.craftdice.domain.battle.model.BattleConfig
import com.koke1024.craftdice.domain.battle.model.BattleStatus
import com.koke1024.craftdice.domain.meta.MetaProgressionService
import com.koke1024.craftdice.domain.meta.RunStartConfig
import com.koke1024.craftdice.domain.meta.runStartConfig
import com.koke1024.craftdice.domain.roguelike.RunEngine
import com.koke1024.craftdice.domain.roguelike.event.EventCatalog
import com.koke1024.craftdice.domain.roguelike.model.CombatSummary
import com.koke1024.craftdice.domain.roguelike.model.FloorNode
import com.koke1024.craftdice.domain.roguelike.model.Reward
import com.koke1024.craftdice.domain.roguelike.model.RunOutcome
import com.koke1024.craftdice.domain.roguelike.model.RunStatus
import com.koke1024.craftdice.domain.roguelike.model.RoomType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Lightweight projection of a [FloorNode] for display.
 */
data class RoomView(
    val id: Int,
    val floorIndex: Int,
    val type: RoomType,
    val label: String,
    val cleared: Boolean,
    val isCurrent: Boolean,
    val isReachable: Boolean,
)

/**
 * UI state for the dungeon screen.
 *
 * Phase 4 keeps the surface minimal: a readable map, current-room actions,
 * and a finished summary. Full battle integration (driving the Phase 3 battle
 * screen with [com.koke1024.craftdice.domain.roguelike.model.BattleSetup] and
 * returning the [CombatSummary]) is intentionally deferred — combat rooms here
 * are auto-resolved so the run loop is explorable end to end.
 */
sealed interface DungeonUiState {

    data object Idle : DungeonUiState

    data class Running(
        val floors: List<List<RoomView>>,
        val currentRoom: RoomView,
        val availableMoves: List<RoomView>,
        val playerHp: Int,
        val playerMaxHp: Int,
        val inventoryLine: String,
        val status: RunStatus,
        val eventChoices: List<String>,
        val lastMessage: String?,
    ) : DungeonUiState

    data class Finished(
        val victory: Boolean,
        val metaCurrency: Int,
        val floorsCleared: Int,
        val inventoryLine: String,
    ) : DungeonUiState
}

class DungeonViewModel(
    private val runEngine: RunEngine,
    private val metaRepository: MetaProgressRepository,
    private val metaService: MetaProgressionService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DungeonUiState>(DungeonUiState.Idle)
    val uiState: StateFlow<DungeonUiState> = _uiState.asStateFlow()

    private var lastMessage: String? = null
    private var rewardsGranted: Boolean = false

    fun startRun(seed: Long = Random.nextLong()) {
        viewModelScope.launch {
            val config = loadRunStartConfig()
            runEngine.startRun(
                seed = seed,
                playerDice = config.dice,
                hpPerUnit = BattleConfig.DEFAULT_HP + config.bonusStartingHp,
                initialInventory = startingMaterialRewards(config.bonusStartingMaterial),
            )
            lastMessage = null
            rewardsGranted = false
            refresh()
        }
    }

    fun moveTo(roomId: Int) {
        runEngine.moveTo(roomId)
        lastMessage = null
        refresh()
    }

    /**
     * Auto-resolves the current combat room as a player victory so the run
     * loop is explorable. Wired to the real battle flow in a later phase.
     */
    fun fightCurrentCombat() {
        val room = runEngine.state.currentRoom
        val enemy = room.enemy ?: return
        val setup = runEngine.currentBattleSetup()
        val summary = CombatSummary(
            status = BattleStatus.PLAYER1_WON,
            survivingPlayerUnits = setup.playerUnits,
            roundsFought = 1,
            defeatedTemplate = enemy,
        )
        runEngine.applyCombatResult(summary)
        lastMessage = "${enemy.name}を撃破した！"
        refresh()
    }

    fun resolveEvent(choiceIndex: Int) {
        val resolution = runEngine.resolveEvent(choiceIndex)
        lastMessage = resolution.outcome.message
        refresh()
    }

    fun claimReward() {
        runEngine.claimReward()
        lastMessage = "報酬を獲得した。"
        refresh()
    }

    fun rest() {
        runEngine.rest()
        lastMessage = "体力が全回復した。"
        refresh()
    }

    fun finishRun() {
        runEngine.finish()
        refresh()
    }

    private fun refresh() {
        val state = runEngine.state
        if (state.status.isFinished) {
            val outcome = runEngine.finish()
            grantRunRewardIfNeeded(outcome)
            _uiState.value = DungeonUiState.Finished(
                victory = outcome is RunOutcome.Victory,
                metaCurrency = outcome.metaCurrency,
                floorsCleared = outcome.floorsCleared,
                inventoryLine = inventoryLine(state.inventory),
            )
            return
        }
        _uiState.value = DungeonUiState.Running(
            floors = state.map.floors.map { floor ->
                floor.map { nodeId -> roomView(state.map.node(nodeId), state) }
            },
            currentRoom = roomView(state.currentRoom, state),
            availableMoves = runEngine.availableMoves().map { roomView(it, state) },
            playerHp = state.alivePlayerUnits.sumOf { it.currentHp },
            playerMaxHp = state.playerUnits.sumOf { it.maxHp },
            inventoryLine = inventoryLine(state.inventory),
            status = state.status,
            eventChoices = eventChoicesFor(state.currentRoom),
            lastMessage = lastMessage,
        )
    }

    private fun grantRunRewardIfNeeded(outcome: RunOutcome) {
        if (rewardsGranted) return
        rewardsGranted = true
        val gained = outcome.metaCurrency
        if (gained <= 0) return
        viewModelScope.launch {
            val progress = withContext(Dispatchers.IO) { metaRepository.load() }
            val updated = metaService.grantShards(progress, gained)
            withContext(Dispatchers.IO) { metaRepository.save(updated) }
        }
    }

    private suspend fun loadRunStartConfig(): RunStartConfig {
        val progress = withContext(Dispatchers.IO) { metaRepository.load() }
        return metaService.runStartConfig(progress)
    }

    private fun startingMaterialRewards(bonus: Int): List<Reward> =
        if (bonus > 0) listOf(Reward.DiceMaterial(bonus)) else emptyList()

    private fun roomView(node: FloorNode, state: com.koke1024.craftdice.domain.roguelike.model.RunState): RoomView {
        val reachable = node.id in state.currentRoom.nextNodeIds || node.id == state.currentRoomId
        return RoomView(
            id = node.id,
            floorIndex = node.floorIndex,
            type = node.type,
            label = labelFor(node),
            cleared = node.cleared,
            isCurrent = node.id == state.currentRoomId,
            isReachable = reachable,
        )
    }

    private fun labelFor(node: FloorNode): String = when (node.type) {
        RoomType.START -> "入口"
        RoomType.COMBAT -> "戦闘 (${node.enemy?.name ?: "?"})"
        RoomType.ELITE_COMBAT -> "精鋭戦 (${node.enemy?.name ?: "?"})"
        RoomType.REWARD -> "報酬"
        RoomType.EVENT -> "イベント"
        RoomType.REST -> "休憩"
        RoomType.BOSS -> "BOSS (${node.enemy?.name ?: "?"})"
    }

    private fun eventChoicesFor(room: FloorNode): List<String> =
        room.eventId?.let { EventCatalog.byId(it).choices.map { c -> c.label } } ?: emptyList()

    private fun inventoryLine(inventory: List<com.koke1024.craftdice.domain.roguelike.model.Reward>): String {
        val faces = inventory.filterIsInstance<com.koke1024.craftdice.domain.roguelike.model.Reward.FaceFragment>().size
        val material = inventory.filterIsInstance<com.koke1024.craftdice.domain.roguelike.model.Reward.DiceMaterial>().sumOf { it.amount }
        val meta = inventory.filterIsInstance<com.koke1024.craftdice.domain.roguelike.model.Reward.MetaCurrency>().sumOf { it.amount }
        return "面:$faces 素材:$material 欠片:$meta"
    }
}
