package com.example.andopsi.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.intellij.lang.annotations.Language

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit, // <--- 1. ADD THIS CALLBACK
    onLoginSubmit: (String, String) -> Unit,
    onBackClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val languages = mapOf(
        "en" to "English",
        "ro" to "Romanian"
    )

    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log In") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFA6E0FF)
                )
            )
        },
        containerColor = Color(0xFFA6E0FF)
    ) { p ->
        Column(
            modifier = Modifier
                .padding(p)
                .fillMaxSize()
                .padding(16.dp)
                .background(Color(0xFFA6E0FF)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onLoginSubmit(email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && password.isNotBlank()
            ) {
                Text("Log In")
            }

            Spacer(Modifier.height(24.dp))

            // --- Language Selection Dropdown ---
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    // Display the full name (e.g., "English") based on the code (e.g., "en")
                    value = languages[selectedLanguage] ?: selectedLanguage,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    languages.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(text = name) },
                            onClick = {
                                // 2. CALL THE CALLBACK HERE
                                onLanguageSelected(code)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    // UPDATED Callback Signature
    onSignUpSubmit: (String, String, String, String, String?) -> Unit,
    onBackClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    // New State for Avatar
    var avatarUrl by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =   TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFA6E0FF))
            )
        }
        , containerColor = Color(0xFFA6E0FF)
    ) { p ->
        Column(
            modifier = Modifier
                .padding(p)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()).background(Color(0xFFA6E0FF)), // Make it scrollable for small screens
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mandatory Fields
            TextField(value = username, onValueChange = { username = it }, label = { Text("Username *") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            TextField(value = name, onValueChange = { name = it }, label = { Text("Display Name *") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            TextField(value = email, onValueChange = { email = it }, label = { Text("Email *") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password *") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Optional Field
            TextField(
                value = avatarUrl,
                onValueChange = { avatarUrl = it },
                label = { Text("Avatar URL (Optional)") },
                placeholder = { Text("https://example.com/me.png") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    // Pass all 5 parameters
                    onSignUpSubmit(username, email, password, name, avatarUrl)
                },
                modifier = Modifier.fillMaxWidth(),
                // Logic: Only mandatory fields are required to enable the button
                enabled = email.isNotBlank() && password.isNotBlank() && username.isNotBlank() && name.isNotBlank()
            ) {
                Text("Sign Up")
            }
        }
    }
}