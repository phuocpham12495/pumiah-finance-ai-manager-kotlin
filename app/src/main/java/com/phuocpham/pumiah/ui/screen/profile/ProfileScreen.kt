package com.phuocpham.pumiah.ui.screen.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.viewmodel.AuthViewModel
import com.phuocpham.pumiah.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val profileState by profileViewModel.profile.collectAsState()
    val updateState by profileViewModel.updateState.collectAsState()
    val avatarUploadState by profileViewModel.avatarUploadState.collectAsState()
    var editDob by remember { mutableStateOf("") }
    var showUpdateForm by remember { mutableStateOf(false) }
    var snackMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when (mimeType) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes != null) profileViewModel.uploadAvatar(bytes, ext)
    }

    LaunchedEffect(updateState) {
        when (updateState) {
            is UiState.Success -> { showUpdateForm = false; profileViewModel.resetUpdateState() }
            is UiState.Error -> { snackMessage = (updateState as UiState.Error).message; profileViewModel.resetUpdateState() }
            else -> {}
        }
    }

    LaunchedEffect(avatarUploadState) {
        when (avatarUploadState) {
            is UiState.Success -> { snackMessage = "Đã cập nhật ảnh đại diện"; profileViewModel.resetAvatarUploadState() }
            is UiState.Error -> { snackMessage = (avatarUploadState as UiState.Error).message; profileViewModel.resetAvatarUploadState() }
            else -> {}
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hồ sơ", fontWeight = FontWeight.Bold) }) },
        snackbarHost = {
            snackMessage?.let { msg ->
                LaunchedEffect(msg) { kotlinx.coroutines.delay(3000); snackMessage = null }
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(msg) }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with camera overlay
            Box(contentAlignment = Alignment.BottomEnd) {
                val avatarUrl = (profileState as? UiState.Success)?.data?.avatarUrl
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(avatarUrl).crossfade(true).build(),
                        contentDescription = "Ảnh đại diện",
                        modifier = Modifier.size(96.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(96.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                SmallFloatingActionButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.size(32.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    if (avatarUploadState is UiState.Loading) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.CameraAlt, "Thay ảnh", modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when (val state = profileState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Success -> {
                    Text(state.data.email, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    if (!state.data.dateOfBirth.isNullOrBlank()) {
                        Text("Ngày sinh: ${state.data.dateOfBirth}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!showUpdateForm) {
                        OutlinedButton(onClick = { editDob = state.data.dateOfBirth ?: ""; showUpdateForm = true }) {
                            Text("Cập nhật hồ sơ")
                        }
                    }
                }
                is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                else -> {}
            }

            if (showUpdateForm) {
                Spacer(Modifier.height(16.dp))

                var showDatePicker by remember { mutableStateOf(false) }
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = editDob.takeIf { it.isNotBlank() }?.let {
                        runCatching { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
                    }
                )
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    editDob = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Huỷ") } }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                OutlinedTextField(
                    value = editDob,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ngày sinh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.clickable { showDatePicker = true }) }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { profileViewModel.updateProfile(editDob.ifBlank { null }) },
                        enabled = updateState !is UiState.Loading
                    ) {
                        if (updateState is UiState.Loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Lưu")
                    }
                    TextButton(onClick = { showUpdateForm = false }) { Text("Huỷ") }
                }
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            OutlinedButton(onClick = onNavigateToCategories, modifier = Modifier.fillMaxWidth()) {
                Text("Quản lý danh mục")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onNavigateToBudgets, modifier = Modifier.fillMaxWidth()) {
                Text("Ngân sách")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onNavigateToGoals, modifier = Modifier.fillMaxWidth()) {
                Text("Mục tiêu tiết kiệm")
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { authViewModel.signOut(onSignOut) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Đăng xuất") }
        }
    }
}
