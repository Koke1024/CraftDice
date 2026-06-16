package com.koke1024.craftdice.domain.roguelike.generation

import com.koke1024.craftdice.domain.model.Dice
import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.roguelike.model.EnemyTemplate

/**
 * Source pool of enemy blueprints, grouped by encounter weight.
 *
 * The generator picks from [regular] for most combat nodes, [elite] for
 * tougher optional fights, and [bosses] for the final room. Decoupling the
 * catalog from the generator lets tests inject tiny fixed pools while the
 * production default lives in [DefaultEnemyCatalog].
 */
data class EnemyCatalog(
    val regular: List<EnemyTemplate>,
    val elite: List<EnemyTemplate>,
    val bosses: List<EnemyTemplate>,
) {
    init {
        require(regular.isNotEmpty()) { "regular enemy pool must not be empty" }
        require(elite.isNotEmpty()) { "elite enemy pool must not be empty" }
        require(bosses.isNotEmpty()) { "boss pool must not be empty" }
    }
}

/**
 * Tunable shape of a generated dungeon.
 *
 * @param totalFloors number of floors including the START and BOSS layers
 *   (must be >= 3).
 * @param nodesPerFloor node count range sampled per intermediate floor.
 * @param roomTypeWeights weighted pick for intermediate node types; weights
 *   for START/BOSS are ignored.
 * @param eliteMinFloor earliest floor index (1-based intermediate) where an
 *   elite encounter may appear.
 * @param eventIds pool of event identifiers assigned to EVENT rooms.
 */
data class DungeonGenerationConfig(
    val totalFloors: Int = DEFAULT_FLOORS,
    val nodesPerFloor: IntRange = DEFAULT_NODES_PER_FLOOR,
    val roomTypeWeights: Map<RoomTypeWeight, Int> = DEFAULT_TYPE_WEIGHTS,
    val eliteMinFloor: Int = 2,
    val eventIds: List<String> = listOf(
        "devil_gambler",
        "fate_roulette",
        "mysterious_chest",
    ),
) {
    init {
        require(totalFloors >= MIN_TOTAL_FLOORS) {
            "totalFloors must be >= $MIN_TOTAL_FLOORS, got $totalFloors"
        }
        require(nodesPerFloor.first >= 1) {
            "nodesPerFloor lower bound must be >= 1"
        }
        require(eventIds.isNotEmpty()) { "eventIds must not be empty" }
    }

    companion object {
        const val MIN_TOTAL_FLOORS = 3
        const val DEFAULT_FLOORS = 7
        val DEFAULT_NODES_PER_FLOOR = 2..3
        val DEFAULT_TYPE_WEIGHTS: Map<RoomTypeWeight, Int> = mapOf(
            RoomTypeWeight.COMBAT to 50,
            RoomTypeWeight.ELITE_COMBAT to 12,
            RoomTypeWeight.REWARD to 16,
            RoomTypeWeight.EVENT to 12,
            RoomTypeWeight.REST to 10,
        )
    }
}

/**
 * Weight key for intermediate-room type selection, scoped to the weights that
 * make sense mid-dungeon (START/BOSS are placed explicitly).
 */
enum class RoomTypeWeight {
    COMBAT,
    ELITE_COMBAT,
    REWARD,
    EVENT,
    REST,
}

/**
 * Production enemy pool with a balanced spread of tiers.
 *
 * Values are intentionally moderate — Phase 4 only needs a run to be
 * completable; precise balance belongs to a later tuning pass. Exposed as a
 * property rather than an object subclass because [EnemyCatalog] is a data
 * class (final).
 */
object DefaultEnemyCatalog {
    val instance: EnemyCatalog = EnemyCatalog(
        regular = listOf(
        EnemyTemplate("slime", "スライム", Dice.of(
            DiceFace.attack(2), DiceFace.attack(2), DiceFace.defense(), DiceFace.miss(),
        ), hp = 12, rewardTier = 0),
        EnemyTemplate("goblin", "ゴブリン", Dice.of(
            DiceFace.attack(3), DiceFace.attack(2), DiceFace.heal(2), DiceFace.miss(),
        ), hp = 14, rewardTier = 0),
        EnemyTemplate("bat", "コウモリ", Dice.of(
            DiceFace.attack(2), DiceFace.attack(3), DiceFace.miss(), DiceFace.miss(),
        ), hp = 10, rewardTier = 1),
    ),
    elite = listOf(
        EnemyTemplate("skeleton", "スケルトン", Dice.of(
            DiceFace.attack(3), DiceFace.critical(6), DiceFace.defense(), DiceFace.miss(),
        ), hp = 18, rewardTier = 1),
        EnemyTemplate("orc", "オーク", Dice.of(
            DiceFace.attack(4), DiceFace.attack(3), DiceFace.defense(), DiceFace.defense(),
        ), hp = 22, rewardTier = 2),
    ),
    bosses = listOf(
        EnemyTemplate("dragon", "ドラゴン", Dice.of(
            DiceFace.attack(5), DiceFace.critical(7), DiceFace.critical(7),
            DiceFace.defense(), DiceFace.heal(4), DiceFace.miss(),
        ), hp = 36, rewardTier = 3),
        EnemyTemplate("demon", "デーモン", Dice.of(
            DiceFace.attack(4), DiceFace.critical(8), DiceFace.critical(6),
            DiceFace.defense(), DiceFace.heal(3),
        ), hp = 32, rewardTier = 3),
    ),
    )
}
