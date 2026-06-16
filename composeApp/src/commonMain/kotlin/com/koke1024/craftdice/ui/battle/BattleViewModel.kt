package com.koke1024.craftdice.ui.battle

import com.koke1024.craftdice.domain.battle.BattleEngine
import com.koke1024.craftdice.domain.battle.ai.EnemyAI
import com.koke1024.craftdice.domain.battle.ai.PlayerDiceInfo
import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleUnit
import com.koke1024.craftdice.domain.battle.model.BattleConfig
import com.koke1024.craftdice.domain.battle.model.BattleRule
import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.physics.DicePhysicsEngine
import com.koke1024.craftdice.domain.physics.PhysicsConstraints
import com.koke1024.craftdice.domain.physics.PhysicsEngine
import com.koke1024.craftdice.domain.physics.ThrowInput
import com.koke1024.craftdice.domain.physics.Vector2
import com.koke1024.craftdice.domain.roguelike.CombatSummaryBuilder
import com.koke1024.craftdice.domain.roguelike.model.BattleSetup
import com.koke1024.craftdice.ui.session.BattleSessionHolder
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
    private val sessionHolder: BattleSessionHolder,
    private val physicsEngine: PhysicsEngine = DicePhysicsEngine(),
    private val battleEngine: BattleEngine = BattleEngine(),
    private val enemyAI: EnemyAI = EnemyAI(),
) : ViewModel() {

    private val rule: BattleRule = BattleRule.BUMP

    private val _uiState = MutableStateFlow(BattleUiState())
    val uiState: StateFlow<BattleUiState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null
    private val diceOwner: MutableMap<Int, BattleSide> = mutableMapOf()

    /**
     * The [BattleSetup] this battle was launched with, or null for a
     * standalone launch. Only dungeon-launched battles hand a
     * [com.koke1024.craftdice.domain.roguelike.model.CombatSummary] back via
     * the [sessionHolder].
     */
    private var currentSetup: BattleSetup? = null
    private var resultPublished: Boolean = false

    init {
        val staged = sessionHolder.consumeSetup()
        if (staged != null) {
            seedFromSetup(staged)
        } else {
            setupDefaultBattle()
        }
    }

    /**
     * Initialises the engine from a [BattleSetup] handed over by the dungeon.
     * Replaces the legacy auto-resolve: the player and enemy units (with their
     * persistent HP/broken faces) are seeded straight into the [BattleEngine].
     */
    private fun seedFromSetup(setup: BattleSetup) {
        currentSetup = setup
        resultPublished = false
        initBattle(
            playerUnits = setup.playerUnits,
            enemyUnits = setup.enemyUnits,
            rule = setup.rule,
            launchedFromDungeon = true,
        )
    }

    fun setupDefaultBattle() {
        currentSetup = null
        resultPublished = false
        val playerUnits = listOf(
            BattleUnit.fromDice(0, BattleSide.PLAYER1, "プレイヤーA", attackerDice(), BattleConfig.DEFAULT_HP),
            BattleUnit.fromDice(1, BattleSide.PLAYER1, "プレイヤーB", balancedDice(), BattleConfig.DEFAULT_HP),
        )
        val enemyUnits = listOf(
            BattleUnit.fromDice(2, BattleSide.PLAYER2, "エネミーA", balancedDice(), BattleConfig.DEFAULT_HP),
            BattleUnit.fromDice(3, BattleSide.PLAYER2, "エネミーB", tankDice(), BattleConfig.DEFAULT_HP),
        )
        initBattle(
            playerUnits = playerUnits,
            enemyUnits = enemyUnits,
            rule = rule,
            launchedFromDungeon = false,
        )
    }

    /**
     * Shared seeding path: resets physics, configures the [BattleEngine] for
     * both sides under [rule], registers every unit on the tray, and emits the
     * first [BattleUiState].
     */
    private fun initBattle(
        playerUnits: List<BattleUnit>,
        enemyUnits: List<BattleUnit>,
        rule: BattleRule,
        launchedFromDungeon: Boolean,
    ) {
        simulationJob?.cancel()
        physicsEngine.reset()
        diceOwner.clear()

        battleEngine.setup(playerUnits, enemyUnits, rule)
        registerAll(playerUnits + enemyUnits)

        _uiState.value = BattleUiState(
            canThrow = true,
            playerUnits = playerUnits.map { it.toUnitUi() },
            enemyUnits = enemyUnits.map { it.toUnitUi() },
            round = battleEngine.round,
            status = battleEngine.state.status.toUi(),
            launchedFromDungeon = launchedFromDungeon,
        )
    }

    private fun registerAll(units: List<BattleUnit>) {
        val centerX = PhysicsConstraints.TRAY_WIDTH / 2.0
        val centerY = PhysicsConstraints.TRAY_HEIGHT / 2.0
        val playerUnits = units.filter { it.owner == BattleSide.PLAYER1 }
        val enemyUnits = units.filter { it.owner == BattleSide.PLAYER2 }
        placeSide(playerUnits, centerX * 0.5, centerY)
        placeSide(enemyUnits, centerX * 1.5, centerY)
        units.forEach { diceOwner[it.id] = it.owner }
    }

    private fun placeSide(units: List<BattleUnit>, baseX: Double, centerY: Double) {
        val spacing = 50.0
        val startY = centerY - (units.size - 1) * spacing / 2.0
        units.forEachIndexed { index, unit ->
            physicsEngine.addDice(unit.id, unit.dice, Vector2(baseX, startY + index * spacing))
        }
    }

    fun throwAllDice(velocityMultiplier: Double = 1.0) {
        if (_uiState.value.isRolling || battleEngine.isFinished) return

        val snapshots = physicsEngine.getSnapshots()
        if (snapshots.isEmpty()) return

        val centerX = PhysicsConstraints.TRAY_WIDTH / 2.0
        val centerY = PhysicsConstraints.TRAY_HEIGHT / 2.0

        for (snap in snapshots) {
            val owner = diceOwner[snap.id] ?: BattleSide.PLAYER1
            if (owner == BattleSide.PLAYER2) continue
            val dir = Vector2(snap.position.x - centerX, snap.position.y - centerY).normalize()
            val speed = (200.0 + kotlin.random.Random.nextDouble(300.0)) * velocityMultiplier
            physicsEngine.throwDice(ThrowInput(snap.id, dir * speed))
        }

        throwEnemyDice()

        _uiState.value = _uiState.value.copy(isRolling = true, canThrow = false, rollResults = emptyList())
        startSimulation()
    }

    private fun throwEnemyDice() {
        val playerTargets = physicsEngine.getSnapshots()
            .filter { diceOwner[it.id] == BattleSide.PLAYER1 }
            .map { PlayerDiceInfo(it.id, it.position, faceValueFor(it.face)) }
        val plans = enemyAI.planThrows(battleEngine.state, rule, playerTargets)
        for (plan in plans) {
            physicsEngine.throwDice(ThrowInput(plan.unitId, plan.velocity))
        }
    }

    private fun faceValueFor(face: com.koke1024.craftdice.domain.model.DiceFace): Int = when (face.skillType) {
        com.koke1024.craftdice.domain.model.SkillType.CRIT -> face.value + 2
        com.koke1024.craftdice.domain.model.SkillType.ATK, com.koke1024.craftdice.domain.model.SkillType.HEAL -> face.value
        com.koke1024.craftdice.domain.model.SkillType.DEF -> 3
        com.koke1024.craftdice.domain.model.SkillType.MISS -> 0
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
        _uiState.value = _uiState.value.copy(swipePreview = SwipePreviewUi(startX, startY, endX, endY, power))
    }

    fun clearSwipePreview() {
        _uiState.value = _uiState.value.copy(swipePreview = null)
    }

    fun onSwipeEnd(result: SwipeResult) {
        clearSwipePreview()
        if (_uiState.value.isRolling || battleEngine.isFinished) return
        if (result.distance < 20f) return

        val (vx, vy) = result.toVelocity()
        throwAllDiceWithVelocity(vx.toDouble(), vy.toDouble())
    }

    private fun throwAllDiceWithVelocity(vx: Double, vy: Double) {
        if (_uiState.value.isRolling || battleEngine.isFinished) return

        val snapshots = physicsEngine.getSnapshots()
        if (snapshots.isEmpty()) return

        val jitter = 50.0
        for (snap in snapshots) {
            val owner = diceOwner[snap.id] ?: BattleSide.PLAYER1
            if (owner == BattleSide.PLAYER2) continue
            val velocity = Vector2(
                vx + kotlin.random.Random.nextDouble(-jitter, jitter),
                vy + kotlin.random.Random.nextDouble(-jitter, jitter),
            )
            physicsEngine.throwDice(ThrowInput(snap.id, velocity))
        }

        throwEnemyDice()

        _uiState.value = _uiState.value.copy(isRolling = true, canThrow = false, rollResults = emptyList())
        startSimulation()
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
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
        val resolution = battleEngine.resolveRound(results)

        val resultUi = results.results.map { entry ->
            RollResultUi(
                diceId = entry.diceId,
                faceLabel = entry.face.toLabel(),
                faceColor = entry.face.toColor(),
            )
        }
        val state = battleEngine.state
        val log = resolution.steps.map { it.message }

        _uiState.value = _uiState.value.copy(
            isRolling = false,
            rollResults = resultUi,
            canThrow = !battleEngine.isFinished,
            playerUnits = state.player1Units.map { it.toUnitUi() },
            enemyUnits = state.player2Units.map { it.toUnitUi() },
            round = battleEngine.round,
            status = state.status.toUi(),
            log = log,
        )
        reseedPhysicsFromState()
        publishResultIfNeeded()
    }

    /**
     * When the battle reaches a terminal state, builds the
     * [com.koke1024.craftdice.domain.roguelike.model.CombatSummary] from the
     * resolved engine state and the originating [BattleSetup] and publishes it
     * to the [sessionHolder] so the dungeon can sync it on resume.
     *
     * Only runs for dungeon-launched battles (a standalone launch has no
     * [currentSetup]) and only once, so navigating back after a finished
     * battle does not re-publish a stale result.
     */
    private fun publishResultIfNeeded() {
        if (!battleEngine.isFinished) return
        if (resultPublished) return
        val setup = currentSetup ?: return
        val summary = CombatSummaryBuilder.build(battleEngine.state, setup)
        sessionHolder.publishResult(summary)
        resultPublished = true
    }

    private fun reseedPhysicsFromState() {
        physicsEngine.reset()
        for (unit in battleEngine.state.allUnits) {
            if (!unit.isAlive) continue
            val owner = unit.owner
            val centerX = PhysicsConstraints.TRAY_WIDTH / 2.0
            val centerY = PhysicsConstraints.TRAY_HEIGHT / 2.0
            val baseX = if (owner == BattleSide.PLAYER1) centerX * 0.5 else centerX * 1.5
            physicsEngine.addDice(unit.id, unit.dice, Vector2(baseX, centerY))
        }
        updateSnapshots()
    }

    private fun updateSnapshots() {
        val snapshots = physicsEngine.getSnapshots().map { snap ->
            val owner = diceOwner[snap.id] ?: BattleSide.PLAYER1
            DiceSnapshotUi(
                id = snap.id,
                x = snap.position.x.toFloat(),
                y = snap.position.y.toFloat(),
                radius = snap.radius.toFloat(),
                faceLabel = snap.face.toLabel(),
                faceColor = snap.face.toColor(),
                isStopped = snap.isStopped,
                ownerLabel = owner.displayName,
            )
        }
        _uiState.value = _uiState.value.copy(diceSnapshots = snapshots)
    }

    private fun attackerDice(): Dice = Dice(
        listOf(
            DiceFace.attack(3),
            DiceFace.attack(3),
            DiceFace.attack(5),
            DiceFace.miss(),
            DiceFace.critical(8),
            DiceFace.attack(2),
        ),
    )

    private fun balancedDice(): Dice = Dice(
        listOf(
            DiceFace.attack(2),
            DiceFace.heal(4),
            DiceFace.attack(3),
            DiceFace.defense(),
            DiceFace.attack(4),
            DiceFace.miss(),
        ),
    )

    private fun tankDice(): Dice = Dice(
        listOf(
            DiceFace.defense(),
            DiceFace.defense(),
            DiceFace.attack(4),
            DiceFace.attack(4),
            DiceFace.miss(),
            DiceFace.attack(6),
        ),
    )

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
    }
}
