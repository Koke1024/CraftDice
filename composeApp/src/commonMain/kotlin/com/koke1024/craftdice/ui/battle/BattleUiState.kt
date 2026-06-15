package com.koke1024.craftdice.ui.battle

import com.koke1024.craftdice.domain.battle.model.BattleStatus
import com.koke1024.craftdice.domain.battle.model.BattleUnit
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.model.SkillType

data class BattleUiState(
    val diceSnapshots: List<DiceSnapshotUi> = emptyList(),
    val isRolling: Boolean = false,
    val rollResults: List<RollResultUi> = emptyList(),
    val canThrow: Boolean = true,
    val swipePreview: SwipePreviewUi? = null,
    val playerUnits: List<UnitUi> = emptyList(),
    val enemyUnits: List<UnitUi> = emptyList(),
    val round: Int = 1,
    val status: BattleStatusUi = BattleStatusUi.ONGOING,
    val log: List<String> = emptyList(),
)

data class DiceSnapshotUi(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float,
    val faceLabel: String,
    val faceColor: Long,
    val isStopped: Boolean,
    val ownerLabel: String,
)

data class RollResultUi(
    val diceId: Int,
    val faceLabel: String,
    val faceColor: Long,
)

data class SwipePreviewUi(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val power: Float,
)

data class UnitUi(
    val id: Int,
    val name: String,
    val maxHp: Int,
    val currentHp: Int,
    val brokenFaceCount: Int,
    val totalFaces: Int,
)

enum class BattleStatusUi { ONGOING, PLAYER1_WON, PLAYER2_WON, DRAW }

internal fun BattleStatus.toUi(): BattleStatusUi = when (this) {
    BattleStatus.ONGOING -> BattleStatusUi.ONGOING
    BattleStatus.PLAYER1_WON -> BattleStatusUi.PLAYER1_WON
    BattleStatus.PLAYER2_WON -> BattleStatusUi.PLAYER2_WON
    BattleStatus.DRAW -> BattleStatusUi.DRAW
}

internal fun BattleUnit.toUnitUi(): UnitUi = UnitUi(
    id = id,
    name = name,
    maxHp = maxHp,
    currentHp = currentHp,
    brokenFaceCount = brokenFaceIndices.size,
    totalFaces = dice.faceCount,
)

internal fun DiceFace.toLabel(): String =
    when (skillType) {
        SkillType.ATK -> "攻$value"
        SkillType.DEF -> "防"
        SkillType.HEAL -> "回$value"
        SkillType.CRIT -> "必$value"
        SkillType.MISS -> "ﾐｽ"
    }

internal fun DiceFace.toColor(): Long =
    when (skillType) {
        SkillType.ATK -> 0xFFEF5350
        SkillType.DEF -> 0xFF42A5F5
        SkillType.HEAL -> 0xFF66BB6A
        SkillType.CRIT -> 0xFFFFCA28
        SkillType.MISS -> 0xFF9E9E9E
    }
