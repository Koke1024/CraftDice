package com.koke1024.craftdice.ui.dungeon

/**
 * One-shot navigation requests emitted by [DungeonViewModel] for the screen to
 * act on.
 *
 * The view model stays navigation-agnostic: it only signals intent ("go to the
 * battle screen") through a SharedFlow, and the Composable translates that into
 * a NavController call. Sealed so future destinations (e.g. a dedicated event
 * screen) can be added exhaustively.
 */
sealed interface DungeonNavigation {

    /** Open the battle screen to resolve the current combat room. */
    data object NavigateToBattle : DungeonNavigation
}
