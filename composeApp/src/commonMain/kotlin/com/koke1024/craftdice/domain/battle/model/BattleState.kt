package com.koke1024.craftdice.domain.battle.model

/**
 * Immutable snapshot of an entire battle.
 *
 * Holds both teams keyed by side plus round counter, status, and the active
 * trey rule. The resolver and engine produce new [BattleState] instances
 * rather than mutating this one.
 */
data class BattleState(
    val unitsBySide: Map<BattleSide, List<BattleUnit>>,
    val round: Int = 1,
    val status: BattleStatus = BattleStatus.ONGOING,
    val rule: BattleRule = BattleRule.BUMP,
) {
    val player1Units: List<BattleUnit>
        get() = unitsBySide[BattleSide.PLAYER1] ?: emptyList()

    val player2Units: List<BattleUnit>
        get() = unitsBySide[BattleSide.PLAYER2] ?: emptyList()

    val allUnits: List<BattleUnit>
        get() = player1Units + player2Units

    fun units(side: BattleSide): List<BattleUnit> = unitsBySide[side] ?: emptyList()

    fun aliveUnits(side: BattleSide): List<BattleUnit> = units(side).filter { it.isAlive }

    fun isSideDefeated(side: BattleSide): Boolean = aliveUnits(side).isEmpty()

    fun unitById(id: Int): BattleUnit? = allUnits.find { it.id == id }

    fun updateUnit(updated: BattleUnit): BattleState = copy(
        unitsBySide = unitsBySide.mapValues { (_, units) ->
            units.map { if (it.id == updated.id) updated else it }
        },
    )

    fun updateUnits(updated: List<BattleUnit>): BattleState {
        val byId = updated.associateBy { it.id }
        return copy(
            unitsBySide = unitsBySide.mapValues { (_, units) ->
                units.map { byId[it.id] ?: it }
            },
        )
    }

    fun withStatus(status: BattleStatus): BattleState = copy(status = status)

    fun withRule(rule: BattleRule): BattleState = copy(rule = rule)

    fun nextRound(): BattleState = copy(round = round + 1)
}
