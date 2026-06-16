package com.koke1024.craftdice.domain.roguelike.event

import com.koke1024.craftdice.domain.model.DiceFace
import com.koke1024.craftdice.domain.model.Rarity
import com.koke1024.craftdice.domain.roguelike.model.Reward
import com.koke1024.craftdice.domain.roguelike.model.RewardSource

/**
 * Catalogue of the built-in risk-and-return events.
 *
 * These are referenced by [com.koke1024.craftdice.domain.roguelike.model.FloorNode.eventId]
 * when a dungeon node is generated. Phase 4 ships three signature encounters;
 * the format is extensible for later phases.
 */
object EventCatalog {

    val devilGambler: DungeonEvent = DungeonEvent(
        id = "devil_gambler",
        name = "悪魔の賭博師",
        description = "赤眼の賭博師がダイスを転がし、ニヤリと笑う。「命の一部を賭けないか？」",
        choices = listOf(
            EventChoice(
                label = "賭ける",
                outcomes = listOf(
                    EventOutcome(
                        weight = 0.5,
                        effects = listOf(
                            EventEffect.GainReward(
                                Reward.FaceFragment(
                                    DiceFace.critical(7, Rarity.EPIC),
                                    source = RewardSource.EVENT,
                                ),
                            ),
                            EventEffect.GainReward(Reward.DiceMaterial(2, source = RewardSource.EVENT)),
                        ),
                        message = "悪魔のダイスが砕けた！強力な面と素材を奪い取った。",
                    ),
                    EventOutcome(
                        weight = 0.5,
                        effects = listOf(EventEffect.Damage(amount = 12)),
                        message = "悪魔のダイスが勝った。深手を負ってしまった。",
                    ),
                ),
            ),
            EventChoice(
                label = "立ち去る",
                outcomes = listOf(
                    EventOutcome(
                        weight = 1.0,
                        effects = emptyList(),
                        message = "賭けを拒み、静かにその場を去った。",
                    ),
                ),
            ),
        ),
    )

    val fateRoulette: DungeonEvent = DungeonEvent(
        id = "fate_roulette",
        name = "運命のルーレット",
        description = "光と影が交互に点滅する巨大なルーレットが回っている。",
        choices = listOf(
            EventChoice(
                label = "回す",
                outcomes = listOf(
                    EventOutcome(
                        weight = 1.0,
                        effects = listOf(EventEffect.FullHeal),
                        message = "幸運の光！傷が完全に癒えた。",
                    ),
                    EventOutcome(
                        weight = 1.0,
                        effects = listOf(EventEffect.Damage(amount = 8)),
                        message = "不幸の影！毒の針が突き刺さった。",
                    ),
                    EventOutcome(
                        weight = 1.0,
                        effects = listOf(
                            EventEffect.GainMetaCurrency(amount = 3),
                        ),
                        message = "ダイスの欠片が姿を現した！",
                    ),
                    EventOutcome(
                        weight = 1.0,
                        effects = listOf(EventEffect.LoseDiceMaterial(amount = 2)),
                        message = "持っていた素材が闇に消えた…。",
                    ),
                    EventOutcome(
                        weight = 1.0,
                        effects = listOf(
                            EventEffect.GainReward(
                                Reward.FaceFragment(
                                    DiceFace.attack(4, Rarity.RARE),
                                    source = RewardSource.EVENT,
                                ),
                            ),
                        ),
                        message = "珍しい面が転がり出てきた！",
                    ),
                ),
            ),
            EventChoice(
                label = "見過ごす",
                outcomes = listOf(
                    EventOutcome(
                        weight = 1.0,
                        effects = emptyList(),
                        message = "ルーレットを一瞥し、先へ進んだ。",
                    ),
                ),
            ),
        ),
    )

    val mysteriousChest: DungeonEvent = DungeonEvent(
        id = "mysterious_chest",
        name = "怪しげな宝箱",
        description = "埃を被った古い宝箱が、微かに光っている。",
        choices = listOf(
            EventChoice(
                label = "開ける",
                outcomes = listOf(
                    EventOutcome(
                        weight = 0.7,
                        effects = listOf(
                            EventEffect.GainReward(Reward.DiceMaterial(2, source = RewardSource.EVENT)),
                        ),
                        message = "中には上質なダイス素材が入っていた！",
                    ),
                    EventOutcome(
                        weight = 0.3,
                        effects = listOf(
                            EventEffect.Damage(amount = 6),
                            EventEffect.GainReward(
                                Reward.FaceFragment(
                                    DiceFace.attack(5, Rarity.RARE),
                                    source = RewardSource.EVENT,
                                ),
                            ),
                        ),
                        message = "罠が発動した！しかし、強力な面も手に入れた。",
                    ),
                ),
            ),
            EventChoice(
                label = "開けない",
                outcomes = listOf(
                    EventOutcome(
                        weight = 1.0,
                        effects = emptyList(),
                        message = "危険を察知し、宝箱を残して去った。",
                    ),
                ),
            ),
        ),
    )

    val all: List<DungeonEvent> = listOf(devilGambler, fateRoulette, mysteriousChest)

    fun byId(id: String): DungeonEvent = all.first { it.id == id }
}
