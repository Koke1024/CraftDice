package com.koke1024.craftdice.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.koke1024.craftdice.domain.meta.UpgradeCatalog
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDungeon: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("拠点工房") })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { BalanceHeader(uiState) }
            item { ClassSelector(uiState, viewModel::selectClass) }
            item {
                SectionTitle("永続アップグレード")
            }
            items(uiState.upgrades, key = { it.upgrade.id }) { row ->
                UpgradeCard(row, viewModel::purchase)
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onNavigateToDungeon,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("ダンジョンへ出発") }
            }
        }

        uiState.message?.let { msg ->
            androidx.compose.material3.Snackbar(
                modifier = Modifier.padding(16.dp),
            ) { Text(msg) }
        }
    }
}

@Composable
private fun BalanceHeader(uiState: WorkshopUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ダイスの欠片",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "${uiState.balance} 個",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "累計獲得 ${uiState.totalEarned}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ClassSelector(
    uiState: WorkshopUiState,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("職業")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            uiState.classes.forEach { row ->
                FilterChip(
                    selected = row.selected,
                    onClick = { onSelect(row.starterClass.id) },
                    enabled = row.unlocked,
                    label = { Text(row.starterClass.displayName) },
                )
            }
        }
    }
}

@Composable
private fun UpgradeCard(
    row: UpgradeRow,
    onPurchase: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = row.upgrade.displayName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = kindLabel(row),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = row.upgrade.description,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "コスト ${row.upgrade.cost} 欠片",
                    style = MaterialTheme.typography.bodyMedium,
                )
                when {
                    row.owned -> Text(
                        text = "獲得済み",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    row.affordable -> Button(
                        onClick = { onPurchase(row.upgrade.id) },
                    ) { Text("購入") }
                    else -> OutlinedButton(
                        onClick = {},
                        enabled = false,
                    ) { Text("不足") }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 4.dp),
    )
}

private fun kindLabel(row: UpgradeRow): String = when (UpgradeCatalog.kindOf(row.upgrade)) {
    UpgradeCatalog.UpgradeKind.MATERIAL -> "素材強化"
    UpgradeCatalog.UpgradeKind.DICE -> "ダイス強化"
    UpgradeCatalog.UpgradeKind.CLASS -> "職業解放"
}
