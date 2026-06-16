package com.koke1024.craftdice.domain.roguelike.model

/**
 * Progress of an active run.
 */
enum class RunStatus {
    ONGOING,
    VICTORY,
    DEFEAT,
    ;

    val isFinished: Boolean get() = this != ONGOING
}
