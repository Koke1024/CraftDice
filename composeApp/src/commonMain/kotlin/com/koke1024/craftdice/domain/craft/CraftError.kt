package com.koke1024.craftdice.domain.craft

/**
 * Reasons why a craft operation was rejected.
 */
enum class CraftError(val message: String) {
    AT_MAX_FACES("最大面数に達しているため、これ以上面を追加できません"),
    AT_MIN_FACES("最小面数に達しているため、これ以上面を削除できません"),
    WOULD_EMPTY_TYPE("指定スキルの面を全て削除すると面数が0になります"),
    INVALID_FACE_INDEX("指定された面の位置が範囲外です"),
    INVALID_FACE("面の内容が不正です"),
}
