package com.example.msgapp.ui.view

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.msgapp.model.Message
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

@Composable
fun ChatScreen(
    username: String,
    userId: String,
    messages: List<Message>,
    onSend: (String) -> Unit,
    currentRoom: String,
    lastNotifiedId: String?,
    onNotify: (Message) -> Unit,
    onLeaveRoom: (() -> Unit)? = null,
    modifier: Modifier
) {
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(messages.size) {
        val lastMsg = messages.lastOrNull()
        if (lastMsg != null && lastMsg.senderId != userId && lastMsg.id != lastNotifiedId) {
            onNotify(lastMsg)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Cabeçalho
        Surface(
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sala: $currentRoom",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                onLeaveRoom?.let {
                    TextButton(onClick = it) {
                        Text("Trocar sala", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        Divider()

        // Lista de mensagens
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                MessageBubble(msg = msg, isOwn = msg.senderId == userId)
            }
        }

        Divider()

        // Campo de entrada
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                placeholder = { Text("Digite sua mensagem...") },
                shape = RoundedCornerShape(30.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        onSend(input)
                        input = ""
                    }
                },
                shape = CircleShape,
                enabled = input.isNotBlank(),
                modifier = Modifier.defaultMinSize(minWidth = 48.dp)
            ) {
                Text("Enviar")
            }
        }
    }
}

@Composable
fun MessageBubble(msg: Message, isOwn: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isOwn) 48.dp else 8.dp,
                end = if (isOwn) 8.dp else 48.dp,
                top = 2.dp, bottom = 2.dp
            ),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isOwn) {
            // Avatar para recebidas
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB3E5FC)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = msg.senderName.firstOrNull()?.uppercase() ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF01579B)
                )
            }
            Spacer(Modifier.width(2.dp))
        }
        Surface(
            color = if (isOwn) Color(0xFF0068E0) else Color(0xFFF1F0F0),
            shape = RoundedCornerShape(
                topStart = if (isOwn) 20.dp else 4.dp,
                topEnd = if (isOwn) 4.dp else 20.dp,
                bottomEnd = 20.dp,
                bottomStart = 20.dp
            ),
            shadowElevation = 1.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOwn) Color.White else Color(0xFF222222),
                )
                Text(
                    text = android.text.format.DateFormat.format("HH:mm", msg.timestamp).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOwn) Color.White.copy(alpha = 0.7f) else Color(0xFF8A8A8A),
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                )
            }
        }
        if (isOwn) {
            Spacer(Modifier.width(2.dp))
        }
    }
}

// Notificação local
@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun notifyNewMessage(context: Context, message: Message) {
    val channelId = "chat_messages"
    val notificationManager = NotificationManagerCompat.from(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId, "Mensagens", NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Nova mensagem de ${message.senderName}")
        .setContentText(message.text)
        .setSmallIcon(R.drawable.ic_dialog_email)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
    notificationManager.notify(message.id.hashCode(), notification)
}