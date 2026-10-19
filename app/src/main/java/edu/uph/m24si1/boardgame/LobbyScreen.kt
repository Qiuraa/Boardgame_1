package edu.uph.m24si1.boardgame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LobbyScreen(
    roomId: String,
    players: List<Pair<String, RoomPlayer>>,
    currentUid: String,
    hostUid: String,
    onReadyClick: () -> Unit,
    onStartGameClick: () -> Unit
) {
    val currentPlayer = players.find { it.first == currentUid }?.second

    val isHost = currentUid == hostUid

    val allPlayersReady =
        players.isNotEmpty() && players.all { it.second.isReady }

    val canStartGame =
        isHost &&
                players.size >= 2 &&
                allPlayersReady

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        Text("Game Lobby")

        Spacer(modifier = Modifier.height(16.dp))

        Text("Room ID")

        Text(roomId)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Players (${players.size}/4)")

        Spacer(modifier = Modifier.height(8.dp))

        players.forEach { (uid, player) ->

            PlayerLobbyCard(
                player = player,
                isCurrentPlayer = uid == currentUid,
                isHost = uid == hostUid
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onReadyClick,
            enabled = currentPlayer != null
        ) {
            Text(
                if (currentPlayer?.isReady == true) {
                    "Unready"
                } else {
                    "Ready"
                }
            )
        }

        if (isHost) {

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStartGameClick,
                enabled = canStartGame
            ) {
                Text("Start Game")
            }

            if (players.size < 2) {
                Text("Need at least 2 players")
            } else if (!allPlayersReady) {
                Text("All players must be ready")
            }
        }
    }
}

@Composable
fun PlayerLobbyCard(
    player: RoomPlayer,
    isCurrentPlayer: Boolean,
    isHost: Boolean

) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                buildString {
                    if (isHost) {
                        append("👑 ")
                    }

                    append(player.username)

                    if (isCurrentPlayer) {
                        append(" (You)")
                    }
                }
            )

            Spacer(Modifier.height(4.dp))

            if (isHost) {
                Text("Host")
            }

            Spacer(Modifier.height(4.dp))

            Text(
                if (player.isReady) {
                    "Ready"
                } else {
                    "Not Ready"
                }
            )
        }
    }
}