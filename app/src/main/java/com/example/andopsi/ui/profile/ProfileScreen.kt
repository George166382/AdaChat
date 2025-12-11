package com.example.andopsi.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.andopsi.model.User // Ensure you import your User model

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?, // If null -> Not Authenticated. If set -> Authenticated.
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onLogOutClick: () -> Unit
) {
    Scaffold(

        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFA6E0FF)
                )
            )

        },
        // background color for the entire screen
        // Color(0xFFA6E0FF)
        containerColor = Color(0xFFA6E0FF)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .background(Color(0xFFA6E0FF)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (user != null) {
                // --- SCENARIO 2: AUTHENTICATED ---
                AuthenticatedContent(
                    user = user,
                    onEditClick = onEditProfileClick,
                    onLogoutClick = onLogOutClick
                )
            } else {
                // --- SCENARIO 1: NOT AUTHENTICATED ---
                UnauthenticatedContent(
                    onLoginClick = onLoginClick,
                    onSignUpClick = onSignUpClick
                )
            }
        }
    }
}

@Composable
fun AuthenticatedContent(
    user: User,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    // 1. Profile Picture
    if (user.avatarUrl != null) {
        // Use AsyncImage here if you have coil/glide, for now generic placeholder
        // AsyncImage(model = user.photoUrl, ...)

        AsyncImage(
            model = user.avatarUrl,
            contentDescription = "Profile Pic",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
    } else {
        Image(
            painter = rememberVectorPainter(Icons.Default.Person),
            contentDescription = "Profile Pic",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .padding(20.dp)
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 2. Name
    Text(
        text = user.displayName ?: "User",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = user.email ?: "",
        fontSize = 16.sp,
        color = Color.Gray
    )

    Spacer(modifier = Modifier.height(48.dp))

    // 3. Actions
    Button(
        onClick = onEditClick,
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text("Edit Profile")
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
        onClick = onLogoutClick,
        modifier = Modifier.fillMaxWidth(0.8f),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) {
        Text("Log Out")
    }
}

@Composable
fun UnauthenticatedContent(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = null,
        tint = Color.Gray,
        modifier = Modifier.size(80.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Not Connected",
        style = MaterialTheme.typography.headlineMedium,
        color = Color.Gray
    )


    Spacer(modifier = Modifier.height(48.dp))

    // Login
    Button(
        onClick = onLoginClick,
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text("Log In")
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Sign Up
    OutlinedButton(
        onClick = onSignUpClick,
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text("Sign Up")
    }
}