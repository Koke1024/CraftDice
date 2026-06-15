package com.koke1024.craftdice.ui.battle

import com.koke1024.craftdice.domain.model.DiceFace

data class BattleUiState(
    val diceSnapshots: List<DiceSnapshotUi> = emptyList(),
    val isRolling: Boolean = false,
    val rollResults: List<RollResultUi> = emptyList(),
    val canThrow: Boolean = true,
    val swipePreview: SwipePreviewUi? = null,
)

data class DiceSnapshotUi(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float,
    val faceLabel: String,
    val faceColor: Long,
    val isStopped: Boolean,
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

internal fun DiceFace.toLabel(): String =
    when (skillType) {
        com.koke1024.craftdice.domain.model.SkillType.ATK -> "攻$value"
        com.koke1024.craftdice.domain.model.SkillType.DEF -> "防"
        com.koke1024.craftdice.domain.model.SkillType.HEAL -> "回$value"
        com.koke1024.craftdice.domain.model.SkillType.CRIT -> "必$value"
        com.koke1024.craftdice.domain.model.SkillType.MISS -> "ﾐｽ"
    }

internal fun DiceFace.toColor(): Long =
    when (skillType) {
        com.koke1024.craftdice.domain.model.SkillType.ATK -> 0xFFEF5350
        com.koke1024.craftdice.domain.model.SkillType.DEF -> 0xFF42A5F5
        com.koke1024.craftdice.domain.model.SkillType.HEAL -> 0xFF66BB6A
        com.koke1024.craftdice.domain.model.SkillType.CRIT -> 0xFFFFCA28
        com.koke1024.craftdice.domain.model.SkillType.MISS -> 0xFF9E9E9E
    }
