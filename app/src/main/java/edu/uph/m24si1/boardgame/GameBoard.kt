
package edu.uph.m24si1.boardgame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private data class TilePosition(
    val x: Float,
    val y: Float
)

private val tilePositions = listOf(
    TilePosition(0.12f, 0.78f),
    TilePosition(0.10f, 0.62f),
    TilePosition(0.10f, 0.46f),
    TilePosition(0.12f, 0.30f),
    TilePosition(0.23f, 0.17f),
    TilePosition(0.38f, 0.10f),
    TilePosition(0.50f, 0.08f),
    TilePosition(0.62f, 0.10f),
    TilePosition(0.77f, 0.17f),
    TilePosition(0.88f, 0.30f),
    TilePosition(0.90f, 0.46f),
    TilePosition(0.90f, 0.62f),
    TilePosition(0.78f, 0.78f),
    TilePosition(0.62f, 0.86f),
    TilePosition(0.38f, 0.86f)
)

private val playerTokens = listOf("🔵", "🔴", "🟢", "🟡")

@Composable
fun GameBoard(
    playerPositions: Map<String, PlayerGameState>
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val boardSize = maxWidth

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(boardSize)
                .background(
                    color = Color(0xFFE8F0E0),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            tilePositions.forEachIndexed { position, tilePosition ->

                val playersHere = playerPositions
                    .entries
                    .filter { it.value.position == position }
                    .mapNotNull { entry ->
                        val playerIndex = playerPositions.keys
                            .sorted()
                            .indexOf(entry.key)

                        playerTokens.getOrNull(playerIndex)
                    }

                BoardTile(
                    position = position,
                    tokens = playersHere,
                    x = boardSize * tilePosition.x,
                    y = boardSize * tilePosition.y
                )
            }

            Text(
                text = "BOARD",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun BoardTile(
    position: Int,
    tokens: List<String>,
    x: Dp,
    y: Dp
) {
    Box(
        modifier = Modifier
            .offset(
                x = x - 28.dp,
                y = y - 28.dp
            )
            .size(56.dp)
            .background(
                color = if (position == 0) {
                    Color(0xFFE0B15A)
                } else {
                    Color(0xFFD8C5A3)
                },
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = position.toString())

            if (position == 0) {
                Text("START")
            }

            if (tokens.isNotEmpty()) {
                Row {
                    tokens.forEach { token ->
                        Text(text = token)
                    }
                }
            }
        }
    }
}
