package edu.uph.m24si1.boardgame

data class Room(
    val hostUid: String = "",
    val status: String = RoomStatus.WAITING,
    val createdAt: Long = 0L,
    val maxPlayers: Int = 4
)

data class RoomPlayer(
    val username: String = "",
    val joinedAt: Long = 0L,
    val isReady: Boolean = false
)

object RoomStatus {
    const val WAITING = "WAITING"
    const val PLAYING = "PLAYING"
    const val FINISHED = "FINISHED"
}