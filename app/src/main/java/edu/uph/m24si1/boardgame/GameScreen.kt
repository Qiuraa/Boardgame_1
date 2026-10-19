package edu.uph.m24si1.boardgame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme

@Composable
fun GameScreen(
    roomId: String,
    uid: String,
    gameState: GameState,
    isRolling: Boolean,
    errorMessage: String,
    onRollDice: () -> Unit
) {
    val isMyTurn = gameState.currentTurnUid == uid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally


    ) {

        if (errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }
        Text("🎮 Game")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Room: $roomId")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Turn: ${gameState.turnNumber}")
        Text("Dice: ${gameState.diceValue}")

        Spacer(modifier = Modifier.height(16.dp))

        GameBoard(
            playerPositions = gameState.players
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRollDice,
            enabled = gameState.currentTurnUid == uid && !isRolling
        ) {
            Text(
                when {
                    isRolling -> "Rolling..."
                    gameState.currentTurnUid == uid -> "Roll Dice"
                    else -> "Waiting for your turn"
                }
            )
        }
    }
}
