
package edu.uph.m24si1.boardgame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UsernameScreen(
    onContinue: (String) -> Unit,
    isSaving: Boolean = false,
    errorMessage: String = ""
) {

    var username by remember {
        mutableStateOf("")
    }

    val trimmedUsername = username.trim()

    val isValid = trimmedUsername.length in 3..16 &&
            trimmedUsername.matches(
                Regex("^[a-zA-Z0-9_]+$")
            )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Welcome to Board Game!"
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Choose your display name"
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = {
                Text("Display Name")
            },
            placeholder = {
                Text("Enter your name")
            },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "3–16 characters. Letters, numbers, and underscore only."
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = errorMessage
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onContinue(trimmedUsername)
            },
            enabled = isValid && !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isSaving) {
                    "Saving..."
                } else {
                    "Continue"
                }
            )
        }
    }
}
