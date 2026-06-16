package com.koke1024.craftdice.ui.dungeon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.koke1024.craftdice.domain.roguelike.model.RoomType
import com.koke1024.craftdice.ui.session.BattleSessionHolder
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Dungeon map and run-loop surface.
 *
 * Shows the generated floors, the current room, and the action appropriate to
 * that room type. Combat rooms navigate to the real battle screen: the view
 * model stages a [com.koke1024.craftdice.domain.roguelike.model.BattleSetup]
 * via [BattleSessionHolder] and emits [DungeonNavigation.NavigateToBattle],
 * and the resolved [com.koke1024.craftdice.domain.roguelike.model.CombatSummary]
 * is drained from the holder on resume and applied.
 */
@Composable
fun DungeonScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBattle: () -> Unit,
    viewModel: DungeonViewModel = koinViewModel(),
    battleSessionHolder: BattleSessionHolder = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Translate the view model's navigation requests into NavController calls.
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                DungeonNavigation.NavigateToBattle -> onNavigateToBattle()
            }
        }
    }

    // When we come back from the battle screen, drain any published result and
    // apply it. LifecycleEventEffect fires on every ON_RESUME, including the
    // first composition, where there is nothing to drain.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME, lifecycleOwner = LocalLifecycleOwner.current) {
        val summary = battleSessionHolder.consumeResult()
        if (summary != null) {
            viewModel.onCombatResult(summary)
        }
    }

    Scaffold { padding ->
        when (val ui = state) {
            DungeonUiState.Idle -> IdleView(viewModel, onNavigateBack, padding)
            is DungeonUiState.Running -> RunningView(ui, viewModel, onNavigateBack, padding)
            is DungeonUiState.Finished -> FinishedView(ui, viewModel, onNavigateBack, padding)
        }
    }
}

@Composable
private fun IdleView(
    viewModel: DungeonViewModel,
    onNavigateBack: () -> Unit,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("クラフトダイス", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "ダンジョンを生成して1ランを開始します。",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { viewModel.startRun() }) { Text("ダンジョンに入る") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onNavigateBack) { Text("戻る") }
    }
}

@Composable
private fun RunningView(
    ui: DungeonUiState.Running,
    viewModel: DungeonViewModel,
    onNavigateBack: () -> Unit,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("ダンジョン進行", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "HP ${ui.playerHp}/${ui.playerMaxHp}   ${ui.inventoryLine}",
            style = MaterialTheme.typography.bodyMedium,
        )
        ui.lastMessage?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF555555))
        }
        Spacer(Modifier.height(12.dp))

        Text("マップ", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        ui.floors.forEach { floor ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                floor.forEach { room ->
                    RoomChip(room)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("現在の部屋: ${ui.currentRoom.label}", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        CurrentRoomActions(ui, viewModel)

        Spacer(Modifier.height(16.dp))
        Text("次へ進む", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ui.availableMoves.forEach { move ->
                Button(
                    onClick = { viewModel.moveTo(move.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("→ ${move.label}") }
            }
            if (ui.availableMoves.isEmpty()) {
                Text(
                    "進める部屋がありません。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onNavigateBack) { Text("拠点へ戻る") }
    }
}

@Composable
private fun CurrentRoomActions(ui: DungeonUiState.Running, viewModel: DungeonViewModel) {
    val room = ui.currentRoom
    when {
        room.cleared -> Text("（クリア済み）", style = MaterialTheme.typography.bodySmall)
        room.type == RoomType.COMBAT ||
            room.type == RoomType.ELITE_COMBAT ||
            room.type == RoomType.BOSS -> {
            Button(onClick = { viewModel.fightCurrentCombat() }) { Text("戦う") }
        }
        room.type == RoomType.EVENT -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ui.eventChoices.forEachIndexed { index, label ->
                    Button(
                        onClick = { viewModel.resolveEvent(index) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(label) }
                }
            }
        }
        room.type == RoomType.REWARD -> {
            Button(onClick = { viewModel.claimReward() }) { Text("報酬を受け取る") }
        }
        room.type == RoomType.REST -> {
            Button(onClick = { viewModel.rest() }) { Text("休憩して全回復") }
        }
        room.type == RoomType.START -> {
            Text("冒険の始まりです。上の「次へ進む」から部屋を選んでください。", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RoomChip(room: RoomView) {
    val background = when {
        room.isCurrent -> Color(0xFFFFC107)
        room.cleared -> Color(0xFFBDBDBD)
        room.isReachable -> Color(0xFF42A5F5)
        else -> Color(0xFFE0E0E0)
    }
    val foreground = if (room.isCurrent || room.cleared) Color.Black else Color.White
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .width(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            room.label,
            color = foreground,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.alpha(if (room.cleared && !room.isCurrent) 0.6f else 1f),
        )
    }
}

@Composable
private fun FinishedView(
    ui: DungeonUiState.Finished,
    viewModel: DungeonViewModel,
    onNavigateBack: () -> Unit,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (ui.victory) "ダンジョン制圧！" else "討ち果てた…",
            style = MaterialTheme.typography.headlineMedium,
            color = if (ui.victory) Color(0xFF2E7D32) else Color(0xFFC62828),
        )
        Spacer(Modifier.height(12.dp))
        Text("持ち帰ったダイスの欠片: ${ui.metaCurrency}")
        Text("攻略した部屋数: ${ui.floorsCleared}")
        Text("所持品: ${ui.inventoryLine}")
        Spacer(Modifier.height(24.dp))
        Button(onClick = { viewModel.startRun() }) { Text("もう一度") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onNavigateBack) { Text("拠点へ戻る") }
    }
}
