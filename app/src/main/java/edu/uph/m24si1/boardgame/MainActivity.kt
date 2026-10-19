
package edu.uph.m24si1.boardgame

import android.os.Bundle
import androidx.compose.material3.OutlinedTextField
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.uph.m24si1.boardgame.ui.theme.BoardgameTheme
import androidx.compose.runtime.DisposableEffect

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BoardgameTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    PlayerProfileFlow(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerProfileFlow(modifier: Modifier = Modifier) {

    var screen by remember {
        mutableStateOf("loading")
    }

    var uid by remember {
        mutableStateOf("")
    }

    var displayName by remember {
        mutableStateOf("")
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }


    val authManager = remember {
        FirebaseAuthManager()
    }

    val playerRepository = remember {
        PlayerRepository()
    }

    fun loadPlayerProfile() {

        screen = "loading"
        errorMessage = ""

        authManager.signInIfNeeded(

            onSuccess = { userUid ->

                uid = userUid

                playerRepository.getPlayer(
                    uid = userUid,

                    onSuccess = { player ->

                        if (
                            player != null &&
                            player.displayName.isNotBlank()
                        ) {
                            displayName = player.displayName
                            screen = "home"
                        } else {
                            screen = "username"
                        }
                    },

                    onError = { exception ->
                        errorMessage =
                            exception.localizedMessage
                                ?: "Failed to load profile"

                        screen = "error"
                    }
                )
            },

            onError = { exception ->

                errorMessage =
                    exception.localizedMessage
                        ?: "Authentication failed"

                screen = "error"
            }
        )
    }

    LaunchedEffect(Unit) {
        loadPlayerProfile()
    }

    when (screen) {

        "loading" -> {

            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()

                Spacer(modifier = Modifier.height(16.dp))

                Text("Connecting to Firebase...")
            }
        }

        "username" -> {

            UsernameScreen(
                isSaving = isSaving,
                errorMessage = errorMessage,

                onContinue = { name ->

                    isSaving = true
                    errorMessage = ""

                    playerRepository.createPlayer(
                        uid = uid,
                        displayName = name,

                        onSuccess = {
                            displayName = name
                            isSaving = false
                            screen = "home"
                        },

                        onError = { exception ->
                            isSaving = false

                            errorMessage =
                                exception.localizedMessage
                                    ?: "Failed to save profile"
                        }
                    )
                }
            )
        }

        "home" -> MainMenuScreen(
            uid = uid,
            displayName = displayName
        )

        "error" -> {

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text("Something went wrong")

                Spacer(modifier = Modifier.height(12.dp))

                Text(errorMessage)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        loadPlayerProfile()
                    }
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun RoomLobbyFlow(
    roomId: String,
    uid: String,
    onLeave: () -> Unit
) {
    var players by remember {
        mutableStateOf<List<Pair<String, RoomPlayer>>>(emptyList())
    }

    var hostUid by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }
    var isRolling by remember {
        mutableStateOf(false)
    }

    val roomRepository = remember {
        RoomRepository()
    }

    LaunchedEffect(roomId) {
        roomRepository.getRoomHostUid(
            roomId = roomId,

            onSuccess = { uid ->
                hostUid = uid
            },

            onError = { exception ->
                errorMessage =
                    exception.localizedMessage
                        ?: "Failed to load room host"
            }
        )
    }

    DisposableEffect(roomId) {

        val listener = roomRepository.listenToRoomPlayers(
            roomId = roomId,

            onUpdate = { updatedPlayers ->
                players = updatedPlayers
            },

            onError = { exception ->
                errorMessage =
                    exception.localizedMessage
                        ?: "Failed to load players"
            }
        )

        onDispose {
            roomRepository.removeRoomPlayersListener(
                roomId = roomId,
                listener = listener
            )
        }
    }

    var roomStatus by remember {
        mutableStateOf(RoomStatus.WAITING)
    }

    var gameState by remember {
        mutableStateOf<GameState?>(null)
    }

    DisposableEffect(roomId) {

        val listener = roomRepository.listenToGameState(
            roomId = roomId,
            onUpdate = { updatedGameState ->
                gameState = updatedGameState
            },
            onError = { exception ->
                errorMessage =
                    exception.localizedMessage
                        ?: "Failed to load game state"
            }
        )

        onDispose {
            roomRepository.removeGameStateListener(
                roomId = roomId,
                listener = listener
            )
        }
    }

    DisposableEffect(roomId) {

        val listener = roomRepository.listenToRoomStatus(
            roomId = roomId,
            onUpdate = { status ->
                roomStatus = status
            },
            onError = { exception ->
                errorMessage =
                    exception.localizedMessage
                        ?: "Failed to load room status"
            }
        )

        onDispose {
            roomRepository.removeRoomStatusListener(
                roomId = roomId,
                listener = listener
            )
        }
    }

    if (errorMessage.isNotEmpty()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(errorMessage)
        }

    } else {

        if (roomStatus == RoomStatus.PLAYING) {

            val currentGameState = gameState

            if (currentGameState != null) {


                GameScreen(
                    roomId = roomId,
                    uid = uid,
                    gameState = currentGameState,
                    isRolling = isRolling,
                    errorMessage = errorMessage,
                    onRollDice = {
                        if (!isRolling) {
                            isRolling = true
                            roomRepository.testGameStateAccess(roomId) { result ->
                                android.util.Log.d("GAME_DEBUG", result)
                            }
                            roomRepository.getRoomPlayerUids(
                                roomId = roomId,
                                onSuccess = { playerUids ->
                                    roomRepository.rollDice(
                                        roomId = roomId,
                                        uid = uid,
                                        playerUids = playerUids,
                                        onSuccess = {
                                            isRolling = false
                                            errorMessage = ""
                                        },

                                        onError = { exception ->
                                            android.util.Log.e(
                                                "GAME_DEBUG",
                                                "Gagal mengambil urutan pemain",
                                                exception
                                            )

                                            isRolling = false
                                            errorMessage = exception.localizedMessage
                                                ?: "Gagal mengambil urutan pemain"
                                        }

//                                        onError = { exception ->
//                                            isRolling = false
//                                            errorMessage = exception.localizedMessage
//                                                ?: "Gagal melempar dadu"
//                                        }
                                    )
                                },


                                onError = { exception ->
                                    android.util.Log.e(
                                        "GAME_DEBUG",
                                        "Transaksi rollDice gagal",
                                        exception
                                    )

                                    isRolling = false
                                    errorMessage = exception.localizedMessage
                                        ?: "Gagal melempar dadu"
                                }


//                                onError = { exception ->
//                                    isRolling = false
//                                    errorMessage = exception.localizedMessage
//                                        ?: "Gagal mengambil urutan pemain"
//                                }
                            )
                        }
                    }
                )


            } else {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Loading game...")
                }
            }

            return
        }

        LobbyScreen(
            roomId = roomId,
            players = players,
            currentUid = uid,
            hostUid = hostUid,
            onReadyClick = {
                val currentPlayer =
                    players.find { it.first == uid }?.second

                roomRepository.setPlayerReady(
                    roomId = roomId,
                    uid = uid,
                    isReady = currentPlayer?.isReady != true,
                    onSuccess = {},
                    onError = { exception ->
                        errorMessage =
                            exception.localizedMessage
                                ?: "Failed to update ready status"
                    }
                )
            },
            onStartGameClick = {
                val firstPlayerUid = players.firstOrNull()?.first

                if (firstPlayerUid == null) {
                    errorMessage = "No players found."
                } else {
                    roomRepository.createInitialGameState(
                        roomId = roomId,
                        firstPlayerUid = firstPlayerUid,
                        playerUids = players.map { it.first },

                        onSuccess = {
                            roomRepository.setRoomStatus(
                                roomId = roomId,
                                status = RoomStatus.PLAYING,
                                onSuccess = {
                                    // Status listener akan memindahkan
                                    // semua player ke Game Screen.
                                },
                                onError = { exception ->
                                    errorMessage =
                                        exception.localizedMessage
                                            ?: "Failed to start game"
                                }
                            )
                        },

                        onError = { exception ->
                            errorMessage =
                                exception.localizedMessage
                                    ?: "Failed to create game state"
                        }
                    )
                }
            }
        )
    }
}


@Composable
fun MainMenuScreen(
    uid: String,
    displayName: String
) {
    var roomUsername by remember(displayName) {
        mutableStateOf(displayName)
    }

    var joinRoomId by remember {
        mutableStateOf("")
    }

    var joinUsername by remember(displayName) {
        mutableStateOf(displayName)
    }

    var isCreating by remember {
        mutableStateOf(false)
    }

    var isJoining by remember {
        mutableStateOf(false)
    }

    var createdRoomId by remember {
        mutableStateOf("")
    }

    var joinedRoomId by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    val roomRepository = remember {
        RoomRepository()
    }

    // =========================
    // LOBBY
    // =========================

    if (createdRoomId.isNotEmpty()) {
        RoomLobbyFlow(
            roomId = createdRoomId,
            uid = uid,
            onLeave = {
                createdRoomId = ""
            }
        )

        return
    }

    if (joinedRoomId.isNotEmpty()) {
        RoomLobbyFlow(
            roomId = joinedRoomId,
            uid = uid,
            onLeave = {
                joinedRoomId = ""
            }
        )

        return
    }

    // =========================
    // MAIN MENU
    // =========================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text("Board Game")

        Spacer(Modifier.height(16.dp))

        Text("Welcome, $displayName!")

        Spacer(Modifier.height(24.dp))

        // =========================
        // CREATE ROOM
        // =========================

        Text("Create a Room")

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = roomUsername,
            onValueChange = {
                roomUsername = it
            },
            label = {
                Text("Room Username")
            },
            singleLine = true,
            enabled = !isCreating && !isJoining
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                isCreating = true
                message = ""

                roomRepository.createRoom(
                    hostUid = uid,
                    username = roomUsername,
                    maxPlayers = 4,

                    onSuccess = { generatedRoomId ->
                        createdRoomId = generatedRoomId
                        isCreating = false
                    },

                    onError = { exception ->
                        message =
                            exception.localizedMessage
                                ?: "Failed to create room"

                        isCreating = false
                    }
                )
            },
            enabled = !isCreating && !isJoining
        ) {
            Text(
                if (isCreating) {
                    "Creating Room..."
                } else {
                    "Create Room"
                }
            )
        }

        Spacer(Modifier.height(32.dp))

        // =========================
        // JOIN ROOM
        // =========================

        Text("Join a Room")

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = joinRoomId,
            onValueChange = {
                joinRoomId = it
            },
            label = {
                Text("Room ID")
            },
            singleLine = true,
            enabled = !isCreating && !isJoining
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = joinUsername,
            onValueChange = {
                joinUsername = it
            },
            label = {
                Text("Room Username")
            },
            singleLine = true,
            enabled = !isCreating && !isJoining
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                isJoining = true
                message = ""

                roomRepository.joinRoom(
                    roomId = joinRoomId,
                    uid = uid,
                    username = joinUsername,

                    onSuccess = {
                        joinedRoomId = joinRoomId.trim()
                        isJoining = false
                    },

                    onError = { exception ->
                        message =
                            exception.localizedMessage
                                ?: "Failed to join room"

                        isJoining = false
                    }
                )
            },
            enabled = joinRoomId.isNotBlank() &&
                    !isCreating &&
                    !isJoining
        ) {
            Text(
                if (isJoining) {
                    "Joining Room..."
                } else {
                    "Join Room"
                }
            )
        }

        if (message.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))

            Text(message)
        }
    }
}

