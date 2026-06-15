package com.koke1024.craftdice.ui.battle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.koke1024.craftdice.domain.physics.PhysicsConstraints
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleScreen(
    onNavigateBack: () -> Unit,
    viewModel: BattleViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Battle - Dice Physics") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DiceTray(
                state = uiState,
                modifier = Modifier.fillMaxWidth(),
                onSwipeUpdate = viewModel::updateSwipePreview,
                onSwipeEnd = viewModel::onSwipeEnd,
                onSwipeCancel = viewModel::clearSwipePreview,
            )

            Spacer(modifier = Modifier.height(16.dp))

            RollControls(
                state = uiState,
                onRollAll = { viewModel.throwAllDice() },
                onReset = viewModel::setupDefaultBattle,
            )

            if (uiState.rollResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                ResultsPanel(results = uiState.rollResults)
            }
        }
    }
}

@Composable
private fun DiceTray(
    state: BattleUiState,
    modifier: Modifier = Modifier,
    onSwipeUpdate: (Float, Float, Float, Float) -> Unit,
    onSwipeEnd: (SwipeResult) -> Unit,
    onSwipeCancel: () -> Unit,
) {
    val trayRatio = PhysicsConstraints.TRAY_WIDTH / PhysicsConstraints.TRAY_HEIGHT
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(trayRatio.toFloat()),
    ) {
        DiceCanvas(state = state, modifier = Modifier.fillMaxSize())

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        swipeDetectorModifier(
                            onSwipeUpdate = onSwipeUpdate,
                            onSwipeEnd = onSwipeEnd,
                            onSwipeCancel = onSwipeCancel,
                        ),
                    ),
        )
    }
}

@Composable
private fun RollControls(
    state: BattleUiState,
    onRollAll: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onRollAll,
            enabled = state.canThrow,
            modifier = Modifier.weight(1f),
        ) {
            Text(if (state.isRolling) "Rolling..." else "Roll All")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onReset,
            modifier = Modifier.weight(1f),
        ) {
            Text("Reset")
        }
    }
}

@Composable
private fun ResultsPanel(results: List<RollResultUi>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Results",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            results.forEach { result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(16.dp)
                            .width(16.dp),
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(Color(result.faceColor))
                        }
                    }
            Text(
                text = "Dice ${result.diceId}: ${result.faceLabel}",
                style = MaterialTheme.typography.bodyMedium,
            )
                }
            }
        }
    }
}
