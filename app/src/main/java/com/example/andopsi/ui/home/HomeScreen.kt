package org.example.upsy.ui.home

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.andopsi.R
import com.example.andopsi.model.Video
import com.example.andopsi.ui.home.HomeViewModel
import org.example.upsy.ui.components.ChatbotSelection
import org.example.upsy.ui.components.UrlInputField
import retrofit2.Response
// Don't forget to add these imports at the top of your file:
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Divider
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.andopsi.ui.profile.EditProfileScreen
import com.example.andopsi.ui.profile.LoginScreen
import com.example.andopsi.ui.profile.ProfileScreen
import com.example.andopsi.ui.profile.SignUpScreen

// --- DATA MODELS ---
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean
)
// Define a simple enum for internal navigation
enum class ProfileView { MAIN, LOGIN, SIGNUP, EDIT } // <--- Added EDIT
// --- MAIN CONTROLLER SCREEN (Parent) ---
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {


    val videoState by homeViewModel.video.collectAsState()
    val videoDebugState by homeViewModel.videoDebugScreen.collectAsState()

    // COLLECT CURRENT USER
    val currentUser by homeViewModel.currentUser.collectAsState()

    // STATE
    var showPortraitMode by remember { mutableStateOf(false) }
    var currentVideoId by remember { mutableStateOf<String?>(null) }
    var currentVideoTitle by remember { mutableStateOf("") }
    var showProfileScreen by remember { mutableStateOf(false) }

    // NEW: Track which auth screen is visible
    var activeProfileView by remember { mutableStateOf(ProfileView.MAIN) }

    val context = LocalContext.current

    // --- 2. UPDATE BACK HANDLER ---
    BackHandler(enabled = showPortraitMode || currentVideoId != null || showProfileScreen) {
        when {
            // If in Login/Signup, go back to Main Profile
            showProfileScreen && activeProfileView != ProfileView.MAIN -> activeProfileView = ProfileView.MAIN
            // If in Main Profile, close it
            showProfileScreen -> showProfileScreen = false
            currentVideoId != null -> currentVideoId = null
            else -> showPortraitMode = false
        }
    }

    // --- 3. NAVIGATION LOGIC ---
    if (showProfileScreen) {
        // --- PROFILE NAVIGATION SWITCHER ---
        when (activeProfileView) {
            ProfileView.LOGIN -> {
                LoginScreen(
                    onBackClick = { activeProfileView = ProfileView.MAIN },
                    onLoginSubmit = { email, pass ->
                        homeViewModel.login(email, pass) { success, error ->
                            if (success) {
                                activeProfileView = ProfileView.MAIN // Return to profile, which will now show "Authenticated"
                            } else {
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            // ... inside HomeScreen composable ...
            ProfileView.SIGNUP -> {
                SignUpScreen(
                    onBackClick = { activeProfileView = ProfileView.MAIN },
                    // UPDATED Lambda to accept 5 args
                    onSignUpSubmit = { username, email, pass, name, avatarUrl ->
                        homeViewModel.signUp(username, email, pass, name, avatarUrl) { success, error ->
                            if (success) {
                                activeProfileView = ProfileView.MAIN
                            } else {
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            ProfileView.EDIT -> {
                // Safety check: Ensure user is actually logged in before showing Edit screen
                if (currentUser != null) {
                    EditProfileScreen(
                        user = currentUser!!,
                        onBackClick = { activeProfileView = ProfileView.MAIN },
                        onUpdateClick = { newName, newAvatar ->
                            homeViewModel.updateUserProfile(newName, newAvatar) { success, error ->
                                if (success) {
                                    Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                                    activeProfileView = ProfileView.MAIN // Go back to profile
                                } else {
                                    Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                } else {
                    // If somehow they got here without being logged in, reset
                    activeProfileView = ProfileView.MAIN
                }
            }

            ProfileView.MAIN -> {
                ProfileScreen(
                    user = currentUser,
                    onBackClick = { showProfileScreen = false },
                    onLoginClick = { activeProfileView = ProfileView.LOGIN },
                    onSignUpClick = { activeProfileView = ProfileView.SIGNUP },

                    // CONNECT THE EDIT BUTTON HERE
                    onEditProfileClick = { activeProfileView = ProfileView.EDIT },

                    onLogOutClick = {
                        homeViewModel.logout()
                        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    }
                )
            }

        }
    }
    else if (currentVideoId != null) {
        AdaChatScreen(
             videoTitle = currentVideoTitle, // Uncomment if your AdaChatScreen uses title
            videoId = currentVideoId!!,
            onBackClick = { currentVideoId = null }
        )
    }
    else if (showPortraitMode) {
        HomePortraitScreen(
            user = currentUser,
            onTalkClick = { url ->
                homeViewModel.fetchVideoDetailsFromUrl(url)
                // Note: The immediate check of videoState here might be flaky because
                // fetching is async. Consider observing changes separately if this fails.
                videoState?.onSuccess { response ->
                    response.body()?.items?.firstOrNull()?.let { videoItem ->
                        currentVideoId = videoItem.id
                        currentVideoTitle = videoItem.snippet.title
                    }
                }
            },
            onMenuClick = { showPortraitMode = false },

            // --- 4. CONNECT THE PROFILE CLICK ---
            onProfileClick = { showProfileScreen = true
                activeProfileView = ProfileView.MAIN
            }
        )
    }
    else {
        ExistingDebugScreen(
            videoDebugState = videoDebugState,
            onNavigateToPortrait = { showPortraitMode = true },
            onFetchVideo = { url -> homeViewModel.fetchVideoDetailsForDebugScreen(url) }
        )
    }
}

// --- DEBUG SCREEN (Child - Now Stateless) ---
@Composable
fun ExistingDebugScreen(
    videoDebugState: Result<Response<Video>>?, // Receive State
    onNavigateToPortrait: () -> Unit,
    onFetchVideo: (String) -> Unit,                  // Send Event
   // onVideoLoaded: (String) -> Unit                  // Send Event
) {
    var textUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onNavigateToPortrait,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(top = 16.dp)
        ) {
            Text("✨ Enter AdaChat Mode")
        }

        Spacer(modifier = Modifier.height(24.dp))

        UrlInputField(
            value = textUrl,
            onValueChange = { textUrl = it }
        )

        Button(
            onClick = {
                if (textUrl.isNotBlank()) {
                    onFetchVideo(textUrl) // Call the hoisted event
                }
            }
        ) {
            Text("Fetch Info")
        }

        // Display video result
        videoDebugState?.let { result ->
            Spacer(modifier = Modifier.height(16.dp))

            result.onSuccess { response ->
                response.body()?.let { video ->
                    if (video.items.isNotEmpty()) {
                        val videoItem = video.items[0]

                        // Notify Parent that video is loaded (side-effect safely triggered)
                        /*LaunchedEffect(videoItem.id) {
                            onVideoLoaded(videoItem.id)
                        }*/

                        Text(text = "Title: ${videoItem.snippet.title}")
                        Text(text = "Description: ${videoItem.snippet.description}", maxLines = 2)

                        YoutubeScreen(
                            videoId = videoItem.id,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    } else {
                        Text(text = "No video details found.")
                    }
                }
            }.onFailure { exception ->
                Text(
                    text = "Error fetching video details: ${exception.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        ChatbotSelection()
    }
}

// Add this to your HomePortraitScreen composable

// Add this to your HomePortraitScreen composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePortraitScreen(
    user: com.example.andopsi.model.User?,
    onTalkClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFA6E0FF)
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(24.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ada_portrait),
                        contentDescription = "Ada",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "AdaChat",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Your AI Video Assistant",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Divider()

                // Menu Items
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Main Page") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onMenuClick() // Navigate to ExistingDebugScreen
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                /*NavigationDrawerItem(
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Chat History") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        // TODO: Navigate to chat history
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("Saved Videos") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        // TODO: Navigate to saved videos
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
*/
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        // TODO: Navigate to settings
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("About") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        // TODO: Show about dialog
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("Help & Feedback") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        // TODO: Show help
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Color(0xFFA6E0FF),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "AdaChat",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.Black
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onProfileClick) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.Black
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xD0E6FF)
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Image(
                    painter = painterResource(id = R.drawable.ada_portrait),
                    contentDescription = "Ada Portrait",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(0.5f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // if user is logged in, greet by name
                // else generic greeting
                if (user != null) {
                    Text(
                        text = "Hello, ${user.displayName}! What can I help you with?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                else
                {
                    Text(
                        text = "Hello! What can I help you with?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                TextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    placeholder = { Text("Enter Video URL") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Black,
                        unfocusedIndicatorColor = Color.Gray,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onTalkClick(urlText) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Talk to Ada",
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}




// --- NEW ADA CHAT SCREEN (Playback + Chat) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaChatScreen(
    videoTitle: String,
    videoId: String,
    onBackClick: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    // Mock Chat History
    val messages = remember { mutableStateListOf(
        ChatMessage(text = "This video is presenting correct informations.", isUser = false),
    ) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(videoTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFA6E0FF))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFA6E0FF))
        ) {
            // 1. VIDEO PLAYER (Fixed Top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                YoutubeScreen(
                    videoId = videoId,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. CHAT LIST (Flexible Middle)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = true // Chat style: starts from bottom
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }

                items(messages.reversed()) { message ->
                    MessageBubble(message)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // 3. INPUT AREA (Fixed Bottom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Ada something...", color = Color.Gray) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            // Add User Message
                            messages.add(ChatMessage(text = inputText, isUser = true))
                            inputText = ""
                            // Simulate Ada Reply (For demo)
                            messages.add(ChatMessage(text = "That's an interesting perspective on the video.", isUser = false))
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Ada Avatar placeholder
            Box(

                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            ) {
                Image(
                    // Ensure R.drawable.ada_portrait exists!
                    painter = painterResource(id = R.drawable.ada_portrait),
                    contentDescription = "Ada Portrait",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                    // .background(Color.LightGray, RoundedCornerShape(16.dp))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color.White else Color.Black
            )
        }
    }
}

fun extractVideoId(url: String): String {
    return when {
        "youtu.be/" in url -> url.substringAfterLast("/").substringBefore("?")
        "youtube.com/shorts/" in url -> url.substringAfterLast("/").substringBefore("?")
        "v=" in url -> url.substringAfter("v=").substringBefore("&").substringBefore("?")
        else -> ""
    }
}