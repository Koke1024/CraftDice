package com.koke1024.craftdice.domain.battle.model

/**
 * Trey rules (handoff-compatible, switchable via config).
 *
 * - [BUMP]: dice can be bumped to change the outcome (handoff core, default).
 * - [NO_BUMP]: outcomes are fixed, pure luck.
 * - [CENTER]: landing in the central zone grants a power bonus.
 */
enum class BattleRule(val displayName: String, val description: String) {
    BUMP("ぶつけ", "ダイスをぶつけて出目を変えられる（デフォルト）"),
    NO_BUMP("固定", "出目固定の純運ゲー"),
    CENTER("中央", "中央ゾーン着弾で威力ボーナス"),
}
