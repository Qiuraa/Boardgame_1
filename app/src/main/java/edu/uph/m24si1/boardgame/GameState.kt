package edu.uph.m24si1.boardgame

data class GameState(
    val turnNumber: Int = 1,
    val currentTurnUid: String = "",
    val diceValue: Int = 0,
    val players: Map<String, PlayerGameState> = emptyMap()
)