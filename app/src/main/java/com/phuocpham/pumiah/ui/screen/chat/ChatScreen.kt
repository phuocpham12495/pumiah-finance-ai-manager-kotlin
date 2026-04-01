package com.phuocpham.pumiah.ui.screen.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phuocpham.pumiah.data.model.ChatMessage
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.ui.theme.LightPurple
import com.phuocpham.pumiah.ui.theme.Purple
import com.phuocpham.pumiah.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val sendState by viewModel.sendState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showClearConfirm by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) inputText = text
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PFAM AI", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showClearConfirm = true }) { Icon(Icons.Default.Delete, "Xóa lịch sử") }
                }
            )
        },
        bottomBar = {
            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói câu hỏi của bạn...")
                        }
                        speechLauncher.launch(intent)
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50))
                        .size(48.dp)
                ) {
                    Icon(Icons.Default.Mic, "Nhập giọng nói", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                OutlinedTextField(
                    value = inputText, onValueChange = { inputText = it },
                    placeholder = { Text("Hỏi PFAM AI...") },
                    modifier = Modifier.weight(1f), maxLines = 3,
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(
                    onClick = { if (inputText.isNotBlank()) { viewModel.sendMessage(inputText); inputText = "" } },
                    enabled = inputText.isNotBlank() && sendState !is UiState.Loading,
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50))
                        .size(48.dp)
                ) {
                    if (sendState is UiState.Loading)
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Icon(Icons.Default.Send, null, tint = Color.White)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            items(messages, key = { it.id }) { msg -> ChatBubble(msg) }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Xóa lịch sử chat") },
            text = { Text("Bạn có chắc muốn xóa toàn bộ lịch sử trò chuyện?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearHistory(); showClearConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Huỷ") } }
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(modifier = Modifier.size(32.dp).background(Purple, RoundedCornerShape(50))
                .align(Alignment.Bottom), contentAlignment = Alignment.Center) {
                Text("AI", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isUser) Purple else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(message.content, color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        }
    }
}
