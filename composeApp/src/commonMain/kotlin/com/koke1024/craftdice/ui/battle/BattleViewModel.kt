package com.koke1024.craftdice.ui.battle

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.model.SkillType
import com.koke1024.craftdice.domain.physics.DicePhysicsEngine
import com.koke1024.craftdice.domain.physics.DiceSetup
import com.koke1024.craftdice.domain.physics.PhysicsConstraints
import com.koke1024.craftdice.domain.physics.PhysicsEngine
import com.koke1024.craftdice.domain.physics.ThrowInput
import com.koke1024.craftdice.domain.physics.Vector2
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BattleViewModel(
    private val physicsEngine: PhysicsEngine = DicePhysicsEngine(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(BattleUiState())
    val uiState: StateFlow<BattleUiState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null
    private var nextDiceId = 0

    init {
        setupDefaultBattle()
    }

    fun setupDefaultBattle() {
        simulationJob?.cancel()
        physicsEngine.reset()
        nextDiceId = 0

        val playerDice = createSampleDice()
        val centerX = PhysicsConstraints.TRAY_WIDTH / 2.0
        val centerY = PhysicsConstraints.TRAY_HEIGHT / 2.0

        physicsEngine.addDice(nextDiceId, playerDice, Vector2(centerX - 40.0, centerY))
        nextDiceId++
        physicsEngine.addDice(nextDiceId, playerDice, Vector2(centerX + 40.0, centerY))
        nextDiceId++

        updateSnapshots()
        _uiState.value = _uiState.value.copy(isRolling = false, rollResults = emptyList(), canThrow = true)
    }

    fun throwAllDice(velocityMultiplier: Double = 1.0) {
        if (_uiState.value.isRolling) return

        val bodies = physicsEngine.getSnapshots()
        if (bodies.isEmpty()) return

        val centerX = PhysicsConstraints.TRAY_WIDTH / 2.0
        val centerY = PhysicsConstraints.TRAY_HEIGHT / 2.0

        for (snap in bodies) {
            val dir = Vector2(snap.position.x - centerX, snap.position.y - centerY).normalize()
            val speed = (200.0 + kotlin.random.Random.nextDouble(300.0)) * velocityMultiplier
            val velocity = dir * speed
            physicsEngine.throwDice(ThrowInput(snap.id, velocity))
        }

        _uiState.value = _uiState.value.copy(isRolling = true, canThrow = false, rollResults = emptyList())
        startSimulation()
    }

    fun throwDice(
        id: Int,
        velocityX: Double,
        velocityY: Double,
    ) {
        if (_uiState.value.isRolling) return
        physicsEngine.throwDice(ThrowInput(id, Vector2(velocityX, velocityY)))
        _uiState.value = _uiState.value.copy(isRolling = true, canThrow = false, rollResults = emptyList())
        startSimulation()
    }

    fun updateSwipePreview(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
    ) {
        val dx = endX - startX
        val dy = endY - startY
        val power = (kotlin.math.hypot(dx, dy) / 200.0).coerceIn(0.0, 1.0).toFloat()
        _uiState.value =
            _uiState.value.copy(
                swipePreview = SwipePreviewUi(startX, startY, endX, endY, power),
            )
    }

    fun clearSwipePreview() {
        _uiState.value = _uiState.value.copy(swipePreview = null)
    }

    fun onSwipeEnd(result: SwipeResult) {
        clearSwipePreview()
        if (_uiState.value.isRolling) return
        if (result.distance < 20f) return

        val (vx, vy) = result.toVelocity()
        throwAllDiceWithVelocity(vx.toDouble(), vy.toDouble())
    }

    private fun throwAllDiceWithVelocity(vx: Double, vy: Double) {
        if (_uiState.value.isRolling) return

        val bodies = physicsEngine.getSnapshots()
        if (bodies.isEmpty()) return

        for (snap in bodies) {
            val jitter = 50.0
            val velocity =
                Vector2(
                    vx + kotlin.random.Random.nextDouble(-jitter, jitter),
                    vy + kotlin.random.Random.nextDouble(-jitter, jitter),
                )
            physicsEngine.throwDice(ThrowInput(snap.id, velocity))
        }

        _uiState.value = _uiState.value.copy(isRolling = true, canThrow = false, rollResults = emptyList())
        startSimulation()
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob =
            viewModelScope.launch {
                val timeStep = PhysicsConstraints.FIXED_TIME_STEP
                while (isActive) {
                    physicsEngine.step(timeStep)
                    updateSnapshots()
                    if (physicsEngine.isAllStopped()) {
                        finishRoll()
                        break
                    }
                    delay((timeStep * 1000).toLong())
                }
            }
    }

    private fun finishRoll() {
        val results = physicsEngine.getResults()
        val resultUi =
            results.results.map { entry ->
                RollResultUi(
                    diceId = entry.diceId,
                    faceLabel = entry.face.toLabel(),
                    faceColor = entry.face.toColor(),
                )
            }
        _uiState.value =
            _uiState.value.copy(
                isRolling = false,
                rollResults = resultUi,
                canThrow = true,
            )
    }

    private fun updateSnapshots() {
        val snapshots =
            physicsEngine.getSnapshots().map { snap ->
                DiceSnapshotUi(
                    id = snap.id,
                    x = snap.position.x.toFloat(),
                    y = snap.position.y.toFloat(),
                    radius = snap.radius.toFloat(),
                    faceLabel = snap.face.toLabel(),
                    faceColor = snap.face.toColor(),
                    isStopped = snap.isStopped,
                )
            }
        _uiState.value = _uiState.value.copy(diceSnapshots = snapshots)
    }

    private fun createSampleDice(): Dice =
        Dice(
            listOf(
                DiceFace.attack(10),
                DiceFace.defense(),
                DiceFace.heal(5),
                DiceFace.critical(20),
                DiceFace.miss(),
                DiceFace.attack(5),
            ),
        )

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
    }
}
