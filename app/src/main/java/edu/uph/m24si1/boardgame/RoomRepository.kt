
package edu.uph.m24si1.boardgame

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import java.util.Locale

class RoomRepository {

    companion object {
        private const val DATABASE_URL =
            "https://boardgame-c26a2-default-rtdb.asia-southeast1.firebasedatabase.app"
    }

    private val database =
        FirebaseDatabase.getInstance(DATABASE_URL).reference

    private val roomMeta = database.child("roomMeta")
    private val roomPlayers = database.child("roomPlayers")
    private val roomUsernames = database.child("roomUsernames")
    private val roomSlots = database.child("roomSlots")
    private val gameStateRef = database.child("gameState")

    private fun validateUsername(username: String): String {
        val trimmed = username.trim()

        require(
            trimmed.length in 3..16 &&
                    trimmed.matches(Regex("^[a-zA-Z0-9_]+$"))
        ) {
            "Username must be 3-16 characters and contain only letters, numbers, or underscore."
        }

        return trimmed
    }

    // CREATE ROOM
    fun createRoom(
        hostUid: String,
        username: String,
        maxPlayers: Int = 4,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val validUsername = validateUsername(username)

            require(hostUid.isNotBlank()) {
                "Invalid player UID."
            }

            require(maxPlayers in 2..4) {
                "Maximum players must be between 2 and 4."
            }

            val roomId = roomMeta.push().key
                ?: throw Exception("Failed to generate Room ID")

            val usernameKey = validUsername.lowercase(Locale.ROOT)

            val updates = mapOf(
                "roomMeta/$roomId" to mapOf(
                    "hostUid" to hostUid,
                    "status" to RoomStatus.WAITING,
                    "createdAt" to ServerValue.TIMESTAMP,
                    "maxPlayers" to maxPlayers
                ),

                "roomPlayers/$roomId/$hostUid" to mapOf(
                    "username" to validUsername,
                    "joinedAt" to ServerValue.TIMESTAMP,
                    "isReady" to false
                ),

                "roomUsernames/$roomId/$usernameKey" to hostUid,

                "roomSlots/$roomId/1" to hostUid
            )

            database.updateChildren(updates)
                .addOnSuccessListener {
                    onSuccess(roomId)
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }

        } catch (exception: Exception) {
            onError(exception)
        }
    }

    // JOIN ROOM
    fun joinRoom(
        roomId: String,
        uid: String,
        username: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val validUsername = try {
            validateUsername(username)
        } catch (exception: Exception) {
            onError(exception)
            return
        }

        if (roomId.isBlank() || uid.isBlank()) {
            onError(Exception("Room ID or UID is invalid."))
            return
        }

        val normalizedRoomId = roomId.trim()
        val usernameKey = validUsername.lowercase(Locale.ROOT)

        roomMeta.child(normalizedRoomId).get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    onError(Exception("Room not found."))
                    return@addOnSuccessListener
                }

                val status = snapshot.child("status")
                    .getValue(String::class.java)

                if (status != RoomStatus.WAITING) {
                    onError(Exception("Room is not accepting players."))
                    return@addOnSuccessListener
                }

                val maxPlayers = snapshot.child("maxPlayers")
                    .getValue(Int::class.java) ?: 4

                roomPlayers.child(normalizedRoomId).child(uid).get()
                    .addOnSuccessListener { playerSnapshot ->

                        if (playerSnapshot.exists()) {
                            onError(Exception("You have already joined this room."))
                            return@addOnSuccessListener
                        }

                        reserveSlot(
                            roomId = normalizedRoomId,
                            uid = uid,
                            slotNumber = 1,
                            maxPlayers = maxPlayers,
                            onSuccess = { slotNumber ->

                                reserveUsername(
                                    roomId = normalizedRoomId,
                                    usernameKey = usernameKey,
                                    uid = uid,
                                    onSuccess = {

                                        val playerData = mapOf(
                                            "username" to validUsername,
                                            "joinedAt" to ServerValue.TIMESTAMP,
                                            "isReady" to false
                                        )

                                        roomPlayers
                                            .child(normalizedRoomId)
                                            .child(uid)
                                            .setValue(playerData)
                                            .addOnSuccessListener {
                                                onSuccess()
                                            }
                                            .addOnFailureListener { exception ->

                                                releaseUsername(
                                                    normalizedRoomId,
                                                    usernameKey
                                                )

                                                releaseSlot(
                                                    normalizedRoomId,
                                                    slotNumber
                                                )

                                                onError(exception)
                                            }
                                    },
                                    onError = { exception ->

                                        releaseSlot(
                                            normalizedRoomId,
                                            slotNumber
                                        )

                                        onError(exception)
                                    }
                                )
                            },
                            onError = onError
                        )
                    }
                    .addOnFailureListener { exception ->
                        onError(exception)
                    }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun listenToRoomPlayers(
        roomId: String,
        onUpdate: (List<Pair<String, RoomPlayer>>) -> Unit,
        onError: (Exception) -> Unit
    ): com.google.firebase.database.ValueEventListener {

        val listener = object : com.google.firebase.database.ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val players = mutableListOf<Pair<String, RoomPlayer>>()

                for (playerSnapshot in snapshot.children) {

                    val uid = playerSnapshot.key ?: continue

                    val username = playerSnapshot
                        .child("username")
                        .getValue(String::class.java)
                        ?: ""

                    val joinedAt = playerSnapshot
                        .child("joinedAt")
                        .getValue(Long::class.java)
                        ?: 0L

                    val isReady = playerSnapshot
                        .child("isReady")
                        .getValue(Boolean::class.java)
                        ?: false

                    val player = RoomPlayer(
                        username = username,
                        joinedAt = joinedAt,
                        isReady = isReady
                    )

                    players.add(uid to player)
                }

                onUpdate(players)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(Exception(error.message))
            }
        }

        roomPlayers
            .child(roomId)
            .addValueEventListener(listener)

        return listener
    }

    fun removeRoomPlayersListener(
        roomId: String,
        listener: com.google.firebase.database.ValueEventListener
    ) {
        roomPlayers
            .child(roomId)
            .removeEventListener(listener)
    }

    fun setPlayerReady(
        roomId: String,
        uid: String,
        isReady: Boolean,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        roomPlayers
            .child(roomId)
            .child(uid)
            .child("isReady")
            .setValue(isReady)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(
                    Exception(
                        "Failed to update Ready: ${exception.message}",
                        exception
                    )
                )
            }
    }

    fun getRoomHostUid(
        roomId: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        roomMeta
            .child(roomId)
            .child("hostUid")
            .get()
            .addOnSuccessListener { snapshot ->

                val hostUid = snapshot.getValue(String::class.java)

                if (hostUid.isNullOrBlank()) {
                    onError(Exception("Host UID not found."))
                } else {
                    onSuccess(hostUid)
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun setRoomStatus(
        roomId: String,
        status: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        roomMeta
            .child(roomId)
            .child("status")
            .setValue(status)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun listenToRoomStatus(
        roomId: String,
        onUpdate: (String) -> Unit,
        onError: (Exception) -> Unit
    ): com.google.firebase.database.ValueEventListener {

        val listener = object : com.google.firebase.database.ValueEventListener {

            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {

                val status = snapshot.getValue(String::class.java)

                if (status != null) {
                    onUpdate(status)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                onError(Exception(error.message))
            }
        }

        roomMeta
            .child(roomId)
            .child("status")
            .addValueEventListener(listener)

        return listener
    }

    fun removeRoomStatusListener(
        roomId: String,
        listener: com.google.firebase.database.ValueEventListener
    ) {
        roomMeta
            .child(roomId)
            .child("status")
            .removeEventListener(listener)
    }

    fun createInitialGameState(
        roomId: String,
        firstPlayerUid: String,
        playerUids: List<String>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val playerStates = playerUids.associateWith {
            PlayerGameState(
                position = 0
            )
        }

        val gameState = GameState(
            turnNumber = 1,
            currentTurnUid = firstPlayerUid,
            diceValue = 0,
            players = playerStates
        )

        gameStateRef
            .child(roomId)
            .setValue(gameState)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
    fun listenToGameState(
        roomId: String,
        onUpdate: (GameState) -> Unit,
        onError: (Exception) -> Unit
    ): com.google.firebase.database.ValueEventListener {

        val listener = object : com.google.firebase.database.ValueEventListener {

            override fun onDataChange(
                snapshot: com.google.firebase.database.DataSnapshot
            ) {
                val gameState = snapshot.getValue(GameState::class.java)

                if (gameState != null) {
                    onUpdate(gameState)
                }
            }

            override fun onCancelled(
                error: com.google.firebase.database.DatabaseError
            ) {
                onError(Exception(error.message))
            }
        }

        gameStateRef
            .child(roomId)
            .addValueEventListener(listener)

        return listener
    }

    fun removeGameStateListener(
        roomId: String,
        listener: com.google.firebase.database.ValueEventListener
    ) {
        gameStateRef
            .child(roomId)
            .removeEventListener(listener)
    }


    fun rollDice(
        roomId: String,
        uid: String,
        playerUids: List<String>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (playerUids.size < 2 || uid !in playerUids) {
            onError(Exception("Invalid player list."))
            return
        }

        val diceRoll = (1..6).random()

        android.util.Log.d(
            "GAME_DEBUG",
            "roomId=$roomId, uid=$uid, playerUids=$playerUids"
        )

        gameStateRef
            .child(roomId)
            .runTransaction(object : Transaction.Handler {

                override fun doTransaction(
                    currentData: MutableData
                ): Transaction.Result {

                    val currentTurnUid = currentData
                        .child("currentTurnUid")
                        .getValue(String::class.java)

                    // Hanya pemain yang mendapat giliran boleh roll.
                    if (currentTurnUid != uid) {
                        return Transaction.abort()
                    }

                    val positionData = currentData
                        .child("players")
                        .child(uid)
                        .child("position")

                    val oldPosition =
                        positionData.getValue(Int::class.java)
                            ?: return Transaction.abort()

                    val turnNumber = currentData
                        .child("turnNumber")
                        .getValue(Int::class.java)
                        ?: return Transaction.abort()

                    android.util.Log.d(
                        "GAME_DEBUG",
                        "currentTurnUid=$currentTurnUid, " +
                                "uid=$uid, " +
                                "turnNumber=$turnNumber, " +
                                "diceRoll=$diceRoll, " +
                                "playerUids=$playerUids"
                    )

                    val currentIndex = playerUids.indexOf(uid)
                    val nextIndex = (currentIndex + 1) % playerUids.size
                    val nextPlayerUid = playerUids[nextIndex]

                    val newPosition = (oldPosition + diceRoll) % 15

                    currentData.child("diceValue").value = diceRoll
                    positionData.value = newPosition
                    currentData.child("turnNumber").value = turnNumber + 1
                    currentData.child("currentTurnUid").value = nextPlayerUid

                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    when {
                        error != null -> {
                            onError(Exception(error.message))
                        }

                        committed -> {
                            onSuccess()
                        }

                        else -> {
                            onError(
                                Exception(
                                    "Roll rejected. It may not be your turn, or game state is invalid."
                                )
                            )
                        }
                    }
                }
            })
    }


    fun testGameStateAccess(
        roomId: String,
        onResult: (String) -> Unit
    ) {
        val ref = gameStateRef.child(roomId)

        ref.get()
            .addOnSuccessListener {
                android.util.Log.d(
                    "GAME_DEBUG",
                    "READ gameState berhasil"
                )
                onResult("READ_OK")
            }
            .addOnFailureListener { error ->
                android.util.Log.e(
                    "GAME_DEBUG",
                    "READ gameState gagal",
                    error
                )
                onResult("READ_FAILED: ${error.message}")
            }
    }


    fun testGameStateWrite(
        roomId: String,
        onResult: (String) -> Unit
    ) {
        val ref = gameStateRef.child(roomId).child("debugWriteTest")

        ref.setValue("test")
            .addOnSuccessListener {
                onResult("WRITE_OK")
                ref.removeValue()
            }
            .addOnFailureListener { error ->
                android.util.Log.e(
                    "GAME_DEBUG",
                    "WRITE gameState gagal",
                    error
                )
                onResult("WRITE_FAILED: ${error.message}")
            }
    }



    // RESERVE USERNAME
    private fun reserveUsername(
        roomId: String,
        usernameKey: String,
        uid: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        roomUsernames.child(roomId).child(usernameKey)
            .runTransaction(object : Transaction.Handler {

                override fun doTransaction(
                    currentData: MutableData
                ): Transaction.Result {

                    if (currentData.value != null) {
                        return Transaction.abort()
                    }

                    currentData.value = uid
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    when {
                        error != null ->
                            onError(Exception(error.message))

                        !committed ->
                            onError(Exception(
                                "Username is already taken in this room."
                            ))

                        else ->
                            onSuccess()
                    }
                }
            }, false)
    }


    fun getRoomPlayerUids(
        roomId: String,
        onSuccess: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        roomSlots
            .child(roomId)
            .get()
            .addOnSuccessListener { snapshot ->

                val playerUids = (1..4).mapNotNull { slotNumber ->
                    snapshot
                        .child(slotNumber.toString())
                        .getValue(String::class.java)
                        ?.takeIf { it.isNotBlank() }
                }

                if (playerUids.size < 2) {
                    onError(
                        Exception("At least 2 players are required.")
                    )
                } else {
                    onSuccess(playerUids)
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }


    // RESERVE PLAYER SLOT
    private fun reserveSlot(
        roomId: String,
        uid: String,
        slotNumber: Int,
        maxPlayers: Int,
        onSuccess: (Int) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (slotNumber > maxPlayers) {
            onError(Exception("Room is full."))
            return
        }

        roomSlots.child(roomId)
            .child(slotNumber.toString())
            .runTransaction(object : Transaction.Handler {

                override fun doTransaction(
                    currentData: MutableData
                ): Transaction.Result {

                    if (currentData.value != null) {
                        return Transaction.abort()
                    }

                    currentData.value = uid
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    when {
                        error != null ->
                            onError(Exception(error.message))

                        committed ->
                            onSuccess(slotNumber)

                        else ->
                            reserveSlot(
                                roomId = roomId,
                                uid = uid,
                                slotNumber = slotNumber + 1,
                                maxPlayers = maxPlayers,
                                onSuccess = onSuccess,
                                onError = onError
                            )
                    }
                }
            }, false)
    }

    // RELEASE USERNAME
    private fun releaseUsername(
        roomId: String,
        usernameKey: String
    ) {
        roomUsernames.child(roomId)
            .child(usernameKey)
            .removeValue()
    }

    // RELEASE SLOT
    private fun releaseSlot(
        roomId: String,
        slotNumber: Int
    ) {
        roomSlots.child(roomId)
            .child(slotNumber.toString())
            .removeValue()
    }
}
