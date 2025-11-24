package org.example.upsy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data class to hold information about each chatbot
data class Chatbot(val name: String, val logoUrl: String)

/*
@Composable
fun ChatbotSelection() {
    // List of chatbots to display
    val chatbots = listOf(
        Chatbot("Ask GPT", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/04/ChatGPT_logo.svg/1024px-ChatGPT_logo.svg.png"),
        Chatbot("Ask Gemini", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1a/Google_Gemini_logo.svg/1200px-Google_Gemini_logo.svg.png")
        // Add more chatbots here if you want
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(chatbots) { chatbot ->
            ChatbotIcon(
                chatbot = chatbot,
                onClick = {
                    // Handle click event for each chatbot here
                    println("${chatbot.name} clicked")
                }
            )
        }
    }
}

@Composable
fun ChatbotIcon(
    chatbot: Chatbot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Gradient for the circle border, similar to the example image
    val storyBorder = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFEDA75), // Yellow
            Color(0xFFFA7E1E), // Orange
            Color(0xFFD62976), // Pink
            Color(0xFF962FBF), // Purple
            Color(0xFF4F5BD5)  // Blue
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        AsyncImage(
            model = chatbot.logoUrl,
            contentDescription = "${chatbot.name} Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(
                    BorderStroke(2.dp, storyBorder),
                    CircleShape
                )
                .padding(6.dp) // Padding inside the border
                .clip(CircleShape) // Clip the image to a circle
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = chatbot.name,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}*/
