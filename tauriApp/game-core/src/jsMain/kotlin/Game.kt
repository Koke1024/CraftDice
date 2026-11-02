@file:OptIn(ExperimentalJsExport::class)

import com.koke1024.craftdice.domain.battle.BattleEngine
import com.koke1024.craftdice.domain.battle.ai.EnemyAI
import com.koke1024.craftdice.domain.battle.ai.PlayerDiceInfo
import com.koke1024.craftdice.domain.battle.model.*
import com.koke1024.craftdice.domain.craft.*
import com.koke1024.craftdice.domain.meta.*
import com.koke1024.craftdice.domain.model.*
import com.koke1024.craftdice.domain.physics.*
import com.koke1024.craftdice.domain.roguelike.*
import com.koke1024.craftdice.domain.roguelike.event.*
import com.koke1024.craftdice.domain.roguelike.generation.*
import com.koke1024.craftdice.domain.roguelike.model.*
import com.koke1024.craftdice.domain.roguelike.reward.RewardRoller
import kotlinx.serialization.json.*

private fun obj(vararg pairs: Pair<String, JsonElement>) = JsonObject(mapOf(*pairs))
private fun str(value: String) = JsonPrimitive(value)
private fun num(value: Number) = JsonPrimitive(value)
private fun flag(value: Boolean) = JsonPrimitive(value)
private fun <T> Iterable<T>.json(convert: (T) -> JsonElement) = JsonArray(map(convert))
private fun faceJson(face: DiceFace) = obj(
    "type" to str(face.skillType.name), "label" to str(face.skillType.displayName),
    "value" to num(face.value), "rarity" to str(face.rarity.name),
)

/** String-only boundary keeps Kotlin collections and Longs out of the web API. */
@JsExport
class Game(saved: String) {
    private val service = MetaProgressionService()
    private var progress = MetaProgression.DEFAULT
    private val run = RunEngine(DungeonGenerator(DefaultEnemyCatalog.instance), RewardRoller(), EventResolver())
    private val battle = BattleEngine()
    private val physics = DicePhysicsEngine()
    private var setup: BattleSetup? = null
    private var running = false
    private var inBattle = false
    private var rolling = false
    private var settled = false
    private var rewarded = false
    private var message = ""
    private var log = emptyList<String>()
    private var loadout = StarterClassCatalog.default().dice

    init {
        if (saved.isNotBlank()) {
            val root = Json.parseToJsonElement(saved).jsonObject
            require(root.getValue("version").jsonPrimitive.int == 1) { "未対応のセーブ形式です" }
            val p = root.getValue("progress").jsonObject
            progress = MetaProgression(
                p.getValue("diceShards").jsonPrimitive.int,
                p.getValue("totalEarned").jsonPrimitive.int,
                p.getValue("upgrades").jsonArray.map { it.jsonPrimitive.content }.toSet(),
                p.getValue("classId").jsonPrimitive.content,
            )
            require(progress.purchasedUpgradeIds.all { UpgradeCatalog.byId(it) != null }) { "不明な強化データです" }
            require(service.isClassUnlocked(progress, progress.selectedClassId)) { "職業データが不正です" }
            loadout = service.runStartConfig(progress).dice
        }
    }

    fun save(): String = obj("version" to num(1), "progress" to obj(
        "diceShards" to num(progress.diceShards), "totalEarned" to num(progress.totalEarned),
        "upgrades" to progress.purchasedUpgradeIds.json(::str), "classId" to str(progress.selectedClassId),
    )).toString()

    fun command(action: String, argument: String): String {
        message = ""
        when (action) {
            "purchase" -> {
                check(!running)
                when (val result = service.purchase(progress, argument)) {
                    is PurchaseResult.Success -> progress = result.progress
                    else -> error("購入できません（所持済み、または欠片不足）")
                }
            }
            "class" -> {
                check(!running)
                val result = service.selectClass(progress, argument)
                check(result is ClassSelectResult.Success) { "職業が未解放です" }
                progress = result.progress
                loadout = result.selectedClass.dice
            }
            "craft" -> {
                check(!running)
                val args = argument.split(":")
                val index = args[1].toInt()
                val dice = loadout.first()
                val result = when (args[0]) {
                    "remove" -> DiceCraftService.removeFace(dice, index)
                    else -> {
                        val type = SkillType.valueOf(args[2])
                        val face = DiceFace(type, if (type.hasValue) args[3].toInt() else 0)
                        if (args[0] == "add") DiceCraftService.addFace(dice, face)
                        else DiceCraftService.replaceFace(dice, index, face)
                    }
                }
                when (result) {
                    is CraftResult.Success -> loadout = listOf(result.dice) + loadout.drop(1)
                    is CraftResult.Failure -> error(result.error.message)
                }
            }
            "start" -> {
                check(!running)
                val config = service.runStartConfig(progress)
                run.startRun(argument.toLong(), loadout,
                    hpPerUnit = BattleConfig.DEFAULT_HP + config.bonusStartingHp,
                    initialInventory = if (config.bonusStartingMaterial > 0)
                        listOf(Reward.DiceMaterial(config.bonusStartingMaterial)) else emptyList())
                running = true
                rewarded = false
                inBattle = false
                log = emptyList()
            }
            "move" -> {
                requireRun()
                check(run.state.currentRoom.type == RoomType.START || run.state.currentRoom.cleared) { "現在の部屋を完了してください" }
                check(argument.toInt() != run.state.currentRoomId)
                run.moveTo(argument.toInt())
            }
            "fight" -> {
                requireRoom()
                val next = run.currentBattleSetup()
                setup = next
                battle.setup(next.playerUnits, next.enemyUnits, next.rule)
                inBattle = true
                settled = false
                rolling = false
                log = emptyList()
                seedPhysics()
            }
            "throw" -> {
                check(inBattle && !rolling && !battle.isFinished)
                val values = argument.split(":").map(String::toDouble)
                require(values.size == 2 && values.all { it.isFinite() })
                val velocity = Vector2(values[0], values[1])
                val targets = physics.getSnapshots().filter { snap ->
                    battle.state.player1Units.any { it.id == snap.id }
                }.map { PlayerDiceInfo(it.id, it.position, it.face.value) }
                battle.state.player1Units.filter { it.isAlive }.forEach {
                    physics.throwDice(ThrowInput(it.id, velocity))
                }
                EnemyAI().planThrows(battle.state, battle.state.rule, targets).forEach {
                    physics.throwDice(ThrowInput(it.unitId, it.velocity))
                }
                rolling = true
            }
            "continue" -> {
                check(inBattle && battle.isFinished && !settled)
                run.applyCombatResult(CombatSummaryBuilder.build(battle.state, checkNotNull(setup)))
                settled = true
                inBattle = false
            }
            "event" -> { requireRoom(); message = run.resolveEvent(argument.toInt()).outcome.message }
            "reward" -> { requireRoom(); run.claimReward(); message = "報酬を獲得した。" }
            "rest" -> { requireRoom(); run.rest(); message = "HPと壊れた面が全回復した。" }
            "home" -> {
                check(running && run.state.isFinished && !inBattle)
                running = false
            }
            else -> error("不明な操作です")
        }
        grantReward()
        return snapshot()
    }

    private fun requireRun() { check(running && !inBattle && !run.state.isFinished) }
    private fun requireRoom() { requireRun(); check(!run.state.currentRoom.cleared) { "完了済みの部屋です" } }

    private fun grantReward() {
        if (running && run.state.isFinished && !rewarded) {
            progress = service.grantShards(progress, run.finish().metaCurrency)
            rewarded = true
        }
    }

    private fun seedPhysics() {
        physics.reset()
        listOf(battle.state.player1Units, battle.state.player2Units).forEachIndexed { side, units ->
            units.filter { it.isAlive }.forEachIndexed { i, unit ->
                physics.addDice(unit.id, unit.dice, Vector2(physics.trayWidth * (if (side == 0) .25 else .75),
                    physics.trayHeight / 2 + i * 50))
            }
        }
    }

    /** Called at 60 Hz by requestAnimationFrame, independent of display refresh rate. */
    fun tick(): String {
        if (rolling) {
            physics.step(PhysicsConstraints.FIXED_TIME_STEP)
            if (physics.isAllStopped()) {
                val resolution = battle.resolveRound(physics.getResults())
                log = resolution.steps.map { it.message }
                rolling = false
                if (!battle.isFinished) seedPhysics()
            }
        }
        return snapshot()
    }

    fun snapshot(): String {
        val fields = mutableMapOf<String, JsonElement>(
            "screen" to str(if (inBattle) "battle" else if (running) "dungeon" else "home"),
            "message" to str(message), "balance" to num(progress.diceShards),
            "totalEarned" to num(progress.totalEarned), "classId" to str(progress.selectedClassId),
            "loadout" to loadout.json { it.faces.json(::faceJson) },
            "classes" to StarterClassCatalog.all.json { obj("id" to str(it.id), "name" to str(it.displayName),
                "description" to str(it.description), "unlocked" to flag(service.isClassUnlocked(progress, it.id))) },
            "upgrades" to UpgradeCatalog.all.json { obj("id" to str(it.id), "name" to str(it.displayName),
                "description" to str(it.description), "cost" to num(it.cost), "owned" to flag(it.id in progress.purchasedUpgradeIds)) },
        )
        if (running) {
            val state = run.state
            fields["run"] = obj("status" to str(state.status.name), "seed" to str(state.seed.toString()),
                "cleared" to num(state.roomsCleared), "reward" to num(if (state.isFinished) run.finish().metaCurrency else 0),
                "hp" to num(state.alivePlayerUnits.sumOf { it.currentHp }), "maxHp" to num(state.playerUnits.sumOf { it.maxHp }),
                "material" to num(state.totalDiceMaterial()), "fragments" to num(state.collectedFaces().size),
                "current" to num(state.currentRoomId), "rooms" to state.map.floors.flatten().json { id ->
                    val room = state.map.node(id)
                    obj("id" to num(id), "floor" to num(room.floorIndex), "type" to str(room.type.name),
                        "name" to str(room.enemy?.name ?: room.type.name), "cleared" to flag(room.cleared),
                        "reachable" to flag(id in state.currentRoom.nextNodeIds &&
                            (state.currentRoom.type == RoomType.START || state.currentRoom.cleared)))
                }, "choices" to (state.currentRoom.eventId?.let { EventCatalog.byId(it).choices.map { c -> c.label } }
                    ?: emptyList()).json(::str))
        }
        if (inBattle) {
            fields["battle"] = obj("round" to num(battle.round), "status" to str(battle.state.status.name),
                "rolling" to flag(rolling), "width" to num(physics.trayWidth), "height" to num(physics.trayHeight),
                "units" to battle.state.allUnits.json { obj("id" to num(it.id), "name" to str(it.name),
                    "enemy" to flag(it.owner == BattleSide.PLAYER2), "hp" to num(it.currentHp), "maxHp" to num(it.maxHp),
                    "broken" to num(it.brokenFaceIndices.size)) },
                "dice" to physics.getSnapshots().json { snap -> obj("id" to num(snap.id), "x" to num(snap.position.x),
                    "y" to num(snap.position.y), "radius" to num(snap.radius), "face" to faceJson(snap.face),
                    "enemy" to flag(battle.state.player2Units.any { it.id == snap.id })) }, "log" to log.json(::str))
        }
        return JsonObject(fields).toString()
    }
}
