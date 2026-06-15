package com.koke1024.craftdice.domain.battle.resolution

import com.koke1024.craftdice.domain.battle.facedamage.FaceDamageSystem
import com.koke1024.craftdice.domain.battle.model.BattleConfig
import com.koke1024.craftdice.domain.battle.model.BattleRule
import com.koke1024.craftdice.domain.battle.model.BattleSide
import com.koke1024.craftdice.domain.battle.model.BattleState
import com.koke1024.craftdice.domain.battle.model.BattleStatus
import com.koke1024.craftdice.domain.battle.model.BattleUnit
import com.koke1024.craftdice.domain.battle.outcome.DiceOutcome
import com.koke1024.craftdice.domain.battle.synergy.SynergyDetector
import com.koke1024.craftdice.domain.battle.synergy.SynergyResult
import com.koke1024.craftdice.domain.battle.synergy.SynergyType
import com.koke1024.craftdice.domain.model.SkillType
import kotlin.math.ceil
import kotlin.math.max

/**
 * Core battle resolution engine (Phase 3).
 *
 * Faithfully reproduces the handoff prototype's [buildResolution] flow while
 * integrating Phase 1 dice models, Phase 2 roll outcomes, synergy bonuses,
 * and the face-destruction system:
 *
 * 1. Defends resolve first (set defending guards).
 * 2. Attacks / heals / crits / misses resolve in strict P1→P2 alternation.
 * 3. Synergy multipliers, center bonus, and defense halving are applied.
 * 4. Damage shatters dice faces; broken-face rolls self-damage the actor.
 * 5. Win/loss is evaluated at the end.
 *
 * Pure function: takes a [BattleState] and outcomes, returns a new state.
 */
class BattleResolver(
    private val synergyDetector: SynergyDetector = SynergyDetector(),
    private val faceDamageSystem: FaceDamageSystem = FaceDamageSystem(),
    private val targetSelector: TargetSelector = RandomTargetSelector(),
) {
    fun resolve(state: BattleState, outcomes: List<DiceOutcome>): ResolutionResult {
        val steps = mutableListOf<ResolutionStep>()
        var current = state

        val p1Outcomes = outcomes.filter { it.owner == BattleSide.PLAYER1 }
        val p2Outcomes = outcomes.filter { it.owner == BattleSide.PLAYER2 }
        val synergyP1 = synergyDetector.detect(p1Outcomes)
        val synergyP2 = synergyDetector.detect(p2Outcomes)

        current = applyPinzoroRecoil(current, BattleSide.PLAYER1, synergyP1, steps)
        current = applyPinzoroRecoil(current, BattleSide.PLAYER2, synergyP2, steps)

        current = resolveDefends(current, outcomes, steps)

        val ordered = interleave(p1Outcomes, p2Outcomes)
        for (outcome in ordered) {
            if (current.status.isFinished) break
            val synergy = if (outcome.owner == BattleSide.PLAYER1) synergyP1 else synergyP2
            current = resolveOutcome(current, outcome, synergy, steps)
        }

        current = applyStraightBonus(current, BattleSide.PLAYER1, synergyP1, steps)
        current = applyStraightBonus(current, BattleSide.PLAYER2, synergyP2, steps)

        current = clearDefending(current)
        current = evaluateWin(current)

        return ResolutionResult(steps.toList(), current)
    }

    private fun resolveDefends(
        state: BattleState,
        outcomes: List<DiceOutcome>,
        steps: MutableList<ResolutionStep>,
    ): BattleState {
        var current = state
        for (outcome in outcomes) {
            if (outcome.face.skillType != SkillType.DEF) continue
            val actor = current.unitById(outcome.unitId) ?: continue
            if (!actor.isAlive) continue

            if (outcome.isFaceBroken) {
                current = applyBrokenFace(current, actor, steps)
            } else {
                current = current.updateUnit(actor.defending())
                steps.add(
                    ResolutionStep(
                        kind = StepKind.DEFEND,
                        actorUnitId = actor.id,
                        owner = actor.owner,
                        message = "${actor.name} は みをまもった!",
                    ),
                )
            }
        }
        return current
    }

    private fun resolveOutcome(
        state: BattleState,
        outcome: DiceOutcome,
        synergy: SynergyResult,
        steps: MutableList<ResolutionStep>,
    ): BattleState {
        var current = state
        val actor = current.unitById(outcome.unitId) ?: return current
        if (!actor.isAlive) return current

        if (outcome.isFaceBroken) {
            return applyBrokenFace(current, actor, steps)
        }

        val bonus = centerBonus(outcome, current.rule)

        return when (outcome.face.skillType) {
            SkillType.MISS -> {
                steps.add(
                    ResolutionStep(
                        kind = StepKind.MISS,
                        actorUnitId = actor.id,
                        owner = actor.owner,
                        message = "${actor.name} は ころんでしまった…",
                    ),
                )
                current
            }
            SkillType.HEAL -> resolveHeal(current, actor, outcome, synergy, bonus, steps)
            SkillType.ATK, SkillType.CRIT -> resolveAttack(current, actor, outcome, synergy, bonus, steps)
            SkillType.DEF -> current
        }
    }

    private fun resolveHeal(
        state: BattleState,
        actor: BattleUnit,
        outcome: DiceOutcome,
        synergy: SynergyResult,
        bonus: Int,
        steps: MutableList<ResolutionStep>,
    ): BattleState {
        var current = state
        val rawAmount = outcome.face.value + bonus
        val amount = scaleValue(rawAmount, synergy)
        val target = targetSelector.selectHealTarget(current, actor) ?: actor
        if (!target.isAlive) return current

        val healed = target.withHeal(amount)
        current = current.updateUnit(healed)
        steps.add(
            ResolutionStep(
                kind = StepKind.HEAL,
                actorUnitId = actor.id,
                owner = actor.owner,
                targetUnitId = target.id,
                healAmount = amount,
                message = "${actor.name} の かいふく! HP+$amount",
            ),
        )
        return current
    }

    private fun resolveAttack(
        state: BattleState,
        actor: BattleUnit,
        outcome: DiceOutcome,
        synergy: SynergyResult,
        bonus: Int,
        steps: MutableList<ResolutionStep>,
    ): BattleState {
        var current = state
        val target = targetSelector.selectAttackTarget(current, actor.owner)
        if (target == null) {
            steps.add(
                ResolutionStep(
                    kind = attackKind(outcome),
                    actorUnitId = actor.id,
                    owner = actor.owner,
                    message = "${actor.name} の こうげき! …あいてが いない",
                ),
            )
            return current
        }

        val rawDamage = outcome.face.value + bonus
        val baseDamage = scaleValue(rawDamage, synergy)
        val guarded = target.isDefending
        val damage = if (guarded) {
            max(BattleConfig.MIN_DAMAGE_AFTER_DEFENSE, ceil(baseDamage.toDouble() / BattleConfig.DEFENSE_DIVISOR).toInt())
        } else {
            baseDamage
        }

        val beforeBroken = target.brokenFaceIndices.size
        val damaged = faceDamageSystem.applyDamage(target, damage)
        val facesBroken = damaged.brokenFaceIndices.size - beforeBroken
        current = current.updateUnit(damaged)

        val head = if (outcome.face.skillType == SkillType.CRIT) "${actor.name} の ひっさつ!!" else "${actor.name} の こうげき!"
        val tail = buildString {
            append(" ${target.name}に ${damage}ダメージ")
            if (guarded) append("(ぼうぎょ)")
            if (!damaged.isAlive) append(" たおれた!")
        }
        steps.add(
            ResolutionStep(
                kind = attackKind(outcome),
                actorUnitId = actor.id,
                owner = actor.owner,
                targetUnitId = target.id,
                damageDealt = damage,
                facesBroken = facesBroken,
                message = head + tail,
            ),
        )
        return current
    }

    private fun applyBrokenFace(
        state: BattleState,
        actor: BattleUnit,
        steps: MutableList<ResolutionStep>,
    ): BattleState {
        val recoil = BattleConfig.BROKEN_FACE_SELF_DAMAGE
        val damaged = faceDamageSystem.applyDamage(actor, recoil)
        val current = state.updateUnit(damaged)
        steps.add(
            ResolutionStep(
                kind = StepKind.BROKEN_FACE,
                actorUnitId = actor.id,
                owner = actor.owner,
                targetUnitId = actor.id,
                damageDealt = recoil,
                message = "${actor.name} の面は 欠けていた! 自傷 $recoil ダメージ",
            ),
        )
        return current
    }

    private fun applyPinzoroRecoil(
        state: BattleState,
        side: BattleSide,
        synergy: SynergyResult,
        steps: MutableList<ResolutionStep>,
    ): BattleState {
        if (synergy.type != SynergyType.PINZORO || synergy.recoilDamage <= 0) return state
        var current = state
        for (unit in current.aliveUnits(side)) {
            val damaged = faceDamageSystem.applyDamage(unit, synergy.recoilDamage)
            current = current.updateUnit(damaged)
            steps.add(
                ResolutionStep(
                    kind = StepKind.RECOIL,
                    actorUnitId = unit.id,
                    owner = side,
                    targetUnitId = unit.id,
                    damageDealt = synergy.recoilDamage,
                    message = "${unit.name} に ピンゾロの反動 ${synergy.recoilDamage} ダメージ!",
                ),
            )
        }
        return current
    }

    private fun applyStraightBonus(
        state: BattleState,
        side: BattleSide,
        synergy: SynergyResult,
        steps: MutableList<ResolutionStep>,
    ): BattleState {
        if (synergy.type != SynergyType.STRAIGHT || synergy.bonusDamage <= 0) return state
        var current = state
        val actor = current.aliveUnits(side).firstOrNull() ?: return current
        val target = targetSelector.selectAttackTarget(current, side)
        if (target == null) return current

        val beforeBroken = target.brokenFaceIndices.size
        val damaged = faceDamageSystem.applyDamage(target, synergy.bonusDamage)
        val facesBroken = damaged.brokenFaceIndices.size - beforeBroken
        current = current.updateUnit(damaged)
        steps.add(
            ResolutionStep(
                kind = StepKind.SYNERGY_BONUS,
                actorUnitId = actor.id,
                owner = side,
                targetUnitId = target.id,
                damageDealt = synergy.bonusDamage,
                facesBroken = facesBroken,
                message = "ストレート! ボーナスアタック ${synergy.bonusDamage} ダメージ!",
            ),
        )
        return current
    }

    private fun clearDefending(state: BattleState): BattleState {
        var current = state
        for (unit in current.allUnits) {
            if (unit.isDefending) current = current.updateUnit(unit.clearDefending())
        }
        return current
    }

    private fun evaluateWin(state: BattleState): BattleState {
        val p1Defeated = state.isSideDefeated(BattleSide.PLAYER1)
        val p2Defeated = state.isSideDefeated(BattleSide.PLAYER2)
        val status = when {
            p1Defeated && p2Defeated -> BattleStatus.DRAW
            p2Defeated -> BattleStatus.PLAYER1_WON
            p1Defeated -> BattleStatus.PLAYER2_WON
            else -> BattleStatus.ONGOING
        }
        return state.withStatus(status)
    }

    private fun interleave(p1: List<DiceOutcome>, p2: List<DiceOutcome>): List<DiceOutcome> {
        val ordered = mutableListOf<DiceOutcome>()
        val max = maxOf(p1.size, p2.size)
        for (i in 0 until max) {
            if (i < p1.size) ordered.add(p1[i])
            if (i < p2.size) ordered.add(p2[i])
        }
        return ordered
    }

    private fun scaleValue(raw: Int, synergy: SynergyResult): Int =
        (raw * synergy.valueMultiplier).toInt().coerceAtLeast(1)

    private fun centerBonus(outcome: DiceOutcome, rule: BattleRule): Int =
        if (rule == BattleRule.CENTER && outcome.centerBonus) BattleConfig.CENTER_BONUS else 0

    private fun attackKind(outcome: DiceOutcome): StepKind =
        if (outcome.face.skillType == SkillType.CRIT) StepKind.CRIT else StepKind.ATTACK
}
