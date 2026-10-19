
package edu.uph.m24si1.boardgame

import com.google.firebase.database.FirebaseDatabase

class FirebaseDatabaseManager {

    companion object {
        private const val DATABASE_URL =
            "https://boardgame-c26a2-default-rtdb.asia-southeast1.firebasedatabase.app"
    }

    private val database = FirebaseDatabase
        .getInstance(DATABASE_URL)
        .reference

    fun writeAndReadTest(
        uid: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val testReference = database
            .child("players")
            .child(uid)
            .child("connectionTest")

        testReference.setValue("SUCCESS")
            .addOnSuccessListener {

                testReference.get()
                    .addOnSuccessListener { snapshot ->

                        val result = snapshot
                            .getValue(String::class.java)

                        if (result != null) {
                            onSuccess(result)
                        } else {
                            onError(
                                Exception("Data tidak ditemukan")
                            )
                        }
                    }
                    .addOnFailureListener { exception ->
                        onError(exception)
                    }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}
