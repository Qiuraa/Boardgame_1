
package edu.uph.m24si1.boardgame

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class PlayerRepository {

    companion object {
        private const val DATABASE_URL =
            "https://boardgame-c26a2-default-rtdb.asia-southeast1.firebasedatabase.app"
    }

    private val playersReference = FirebaseDatabase
        .getInstance(DATABASE_URL)
        .getReference("players")

    fun getPlayer(
        uid: String,
        onSuccess: (Player?) -> Unit,
        onError: (Exception) -> Unit
    ) {

        playersReference.child(uid).get()
            .addOnSuccessListener { snapshot ->

                val player = snapshot.getValue(Player::class.java)
                onSuccess(player)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun createPlayer(
        uid: String,
        displayName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        val playerData = mapOf(
            "displayName" to displayName,
            "createdAt" to ServerValue.TIMESTAMP
        )

        playersReference.child(uid)
            .setValue(playerData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}
