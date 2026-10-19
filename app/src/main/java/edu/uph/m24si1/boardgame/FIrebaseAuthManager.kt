package edu.uph.m24si1.boardgame

import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthManager {

    private val auth = FirebaseAuth.getInstance()

    fun signInIfNeeded(
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            onSuccess(currentUser.uid)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid

                if (uid != null) {
                    onSuccess(uid)
                } else {
                    onError(Exception("UID tidak ditemukan"))
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}