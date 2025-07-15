package com.example.msgapp


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.msgapp.ui.view.ChatScreen
import com.example.msgapp.ui.view.RoomSelector
import com.example.msgapp.ui.view.notifyNewMessage
import com.example.msgapp.viewmodel.MsgViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            MsgAppTheme {
                MsgAppRoot()
            }
        }
    }
}

@Composable
fun MsgAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = lightColors(
            primary = Color(0xFF1976D2),
            secondary = Color(0xFF42A5F5)
        ),
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}

@Composable
fun MsgAppRoot(vm: MsgViewModel = viewModel()) {
    val context = LocalContext.current

    // Firebase login anônimo
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val user by produceState(initialValue = firebaseAuth.currentUser) {
        if (value == null) {
            firebaseAuth.signInAnonymously()
                .addOnCompleteListener { task -> value = firebaseAuth.currentUser }
        }
    }

    val userId = user?.uid ?: "pedro"
    var userName by remember { mutableStateOf("Usuário-${userId.takeLast(4)}") }
    var currentRoom by remember { mutableStateOf("geral") }
    var lastNotifiedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentRoom) {
        vm.switchRoom(currentRoom)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MsgApp", color = Color.White) },
                backgroundColor = MaterialTheme.colors.primary
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = "Sala atual: ${currentRoom.uppercase()}",
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .align(Alignment.CenterHorizontally)
                )

                RoomSelector(
                    onRoomSelected = { if (it.isNotBlank()) currentRoom = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                ChatScreen(
                    username = userName,
                    userId = userId,
                    messages = vm.messages.collectAsState().value,
                    onSend = { text -> vm.sendMessage(userId, userName, text) },
                    currentRoom = currentRoom,
                    lastNotifiedId = lastNotifiedId,
                    onNotify = { msg ->
                        notifyNewMessage(context, msg)
                        lastNotifiedId = msg.id
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}