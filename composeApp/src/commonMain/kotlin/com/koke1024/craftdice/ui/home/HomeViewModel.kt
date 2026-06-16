package com.koke1024.craftdice.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koke1024.craftdice.data.repository.MetaProgressRepository
import com.koke1024.craftdice.domain.meta.ClassSelectResult
import com.koke1024.craftdice.domain.meta.MetaProgression
import com.koke1024.craftdice.domain.meta.MetaProgressionService
import com.koke1024.craftdice.domain.meta.PurchaseResult
import com.koke1024.craftdice.domain.meta.StarterClassCatalog
import com.koke1024.craftdice.domain.meta.UpgradeCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View model for the workshop (home) screen.
 *
 * Owns the current [MetaProgression] snapshot, projecting it into a display
 * [WorkshopUiState]. Purchases and class selections are applied through
 * [MetaProgressionService] (pure) and then persisted via the repository on a
 * background dispatcher, so the UI thread never touches disk.
 */
class HomeViewModel(
    private val repository: MetaProgressRepository,
    private val service: MetaProgressionService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkshopUiState())
    val uiState: StateFlow<WorkshopUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun purchase(upgradeId: String) {
        mutate { progress ->
            when (val result = service.purchase(progress, upgradeId)) {
                is PurchaseResult.Success -> {
                    repository.save(result.progress)
                    result.progress to "「${result.upgrade.displayName}」を獲得した！"
                }
                PurchaseResult.AlreadyOwned -> progress to "すでに所持している。"
                PurchaseResult.InsufficientFunds -> progress to "ダイスの欠片が足りない。"
                PurchaseResult.NotFound -> progress to null
            }
        }
    }

    fun selectClass(classId: String) {
        mutate { progress ->
            when (val result = service.selectClass(progress, classId)) {
                is ClassSelectResult.Success -> {
                    repository.save(result.progress)
                    result.progress to "職業を「${result.selectedClass.displayName}」に切り替えた。"
                }
                ClassSelectResult.Locked -> progress to "この職業はまだ解放されていない。"
                ClassSelectResult.NotFound -> progress to null
            }
        }
    }

    /** Dismisses the transient [WorkshopUiState.message]. */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun mutate(block: suspend (MetaProgression) -> Pair<MetaProgression, String?>) {
        viewModelScope.launch {
            val current = repository.load()
            val (next, message) = withContext(Dispatchers.Default) { block(current) }
            publish(next, message)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val progress = withContext(Dispatchers.IO) { repository.load() }
            publish(progress, null)
        }
    }

    private fun publish(progress: MetaProgression, message: String?) {
        _uiState.value = project(progress, message, _uiState.value.message)
    }

    private fun project(
        progress: MetaProgression,
        newMessage: String?,
        previousMessage: String?,
    ): WorkshopUiState = WorkshopUiState(
        balance = progress.diceShards,
        totalEarned = progress.totalEarned,
        upgrades = UpgradeCatalog.all.map { upgrade ->
            UpgradeRow(
                upgrade = upgrade,
                owned = upgrade.id in progress.purchasedUpgradeIds,
                affordable = progress.diceShards >= upgrade.cost,
            )
        },
        classes = StarterClassCatalog.all.map { cls ->
            ClassRow(
                starterClass = cls,
                unlocked = service.isClassUnlocked(progress, cls.id),
                selected = progress.selectedClassId == cls.id,
            )
        },
        message = newMessage ?: previousMessage,
    )
}
