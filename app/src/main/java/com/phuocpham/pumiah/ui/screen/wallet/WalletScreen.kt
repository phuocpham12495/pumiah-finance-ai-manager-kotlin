package com.phuocpham.pumiah.ui.screen.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.data.model.Wallet
import com.phuocpham.pumiah.data.model.WalletParticipant
import com.phuocpham.pumiah.ui.theme.Green
import com.phuocpham.pumiah.ui.theme.Purple
import com.phuocpham.pumiah.viewmodel.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(viewModel: WalletViewModel = hiltViewModel()) {
    val walletsState by viewModel.wallets.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var pendingDeleteWallet by remember { mutableStateOf<Wallet?>(null) }
    var pendingLeaveWallet by remember { mutableStateOf<Wallet?>(null) }
    var pendingRemoveUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionState) {
        if (actionState is UiState.Success) {
            showCreateDialog = false
            selectedWallet = null
            viewModel.resetActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ví của tôi", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadWallets() }) {
                        Icon(Icons.Default.Refresh, "Làm mới")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, "Tạo ví mới")
            }
        }
    ) { padding ->
        when (val state = walletsState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccountBalanceWallet, null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            Spacer(Modifier.height(12.dp))
                            Text("Chưa có ví nào",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("Nhấn + để tạo ví mới",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        items(state.data, key = { it.id }) { wallet ->
                            WalletCard(
                                wallet = wallet,
                                onManage = { selectedWallet = wallet },
                                onDelete = { pendingDeleteWallet = wallet },
                                onLeave = { pendingLeaveWallet = wallet }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
            is UiState.Error -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            else -> {}
        }

    }

    if (showCreateDialog) {
        CreateWalletDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, type -> viewModel.createWallet(name, type) },
            isLoading = actionState is UiState.Loading
        )
    }

    selectedWallet?.let { wallet ->
        ManageWalletDialog(
            wallet = wallet,
            onDismiss = { selectedWallet = null; viewModel.resetActionState() },
            onInvite = { email, role -> viewModel.inviteParticipant(wallet.id, email, role) },
            onRemove = { userId -> pendingRemoveUserId = userId },
            onUpdateRole = { userId, role -> viewModel.updateRole(wallet.id, userId, role) },
            actionState = actionState
        )
    }

    // Confirm delete wallet
    pendingDeleteWallet?.let { wallet ->
        AlertDialog(
            onDismissRequest = { pendingDeleteWallet = null },
            title = { Text("Xác nhận xóa ví") },
            text = { Text("Bạn có chắc muốn xóa ví \"${wallet.name}\"?\nTất cả dữ liệu liên quan sẽ bị xóa.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteWallet(wallet.id); pendingDeleteWallet = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteWallet = null }) { Text("Huỷ") } }
        )
    }

    // Confirm leave wallet
    pendingLeaveWallet?.let { wallet ->
        AlertDialog(
            onDismissRequest = { pendingLeaveWallet = null },
            title = { Text("Rời ví") },
            text = { Text("Bạn có chắc muốn rời khỏi ví \"${wallet.name}\"?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.leaveWallet(wallet.id); pendingLeaveWallet = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Rời") }
            },
            dismissButton = { TextButton(onClick = { pendingLeaveWallet = null }) { Text("Huỷ") } }
        )
    }

    // Confirm remove participant
    pendingRemoveUserId?.let { userId ->
        AlertDialog(
            onDismissRequest = { pendingRemoveUserId = null },
            title = { Text("Xác nhận xóa thành viên") },
            text = { Text("Bạn có chắc muốn xóa thành viên này khỏi ví?") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedWallet?.let { viewModel.removeParticipant(it.id, userId) }
                        pendingRemoveUserId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { pendingRemoveUserId = null }) { Text("Huỷ") } }
        )
    }
}

@Composable
fun WalletCard(
    wallet: Wallet,
    onManage: () -> Unit,
    onDelete: () -> Unit,
    onLeave: () -> Unit
) {
    val isOwner = wallet.myRole == "owner"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AccountBalanceWallet, null,
                        tint = if (isOwner) Purple else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp))
                    Column {
                        Text(wallet.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (isOwner) "Chủ sở hữu" else wallet.myRole.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            color = if (isOwner) Purple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Row {
                    IconButton(onClick = onManage) {
                        Icon(Icons.Default.Group, "Quản lý thành viên")
                    }
                    if (isOwner) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = onLeave) {
                            Icon(Icons.Default.ExitToApp, "Rời ví", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (wallet.participants.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("${wallet.participants.size} thành viên", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))
                wallet.participants.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(p.email.ifBlank { p.userId.take(8) + "..." }, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        RoleBadge(p.role)
                    }
                }
            }
        }
    }
}

@Composable
fun RoleBadge(role: String) {
    val (text, color) = when (role) {
        "owner"    -> Pair("Chủ", Purple)
        "co-owner" -> Pair("Đồng sở hữu", Green)
        else       -> Pair("Xem", MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CreateWalletDialog(onDismiss: () -> Unit, onCreate: (name: String, type: String) -> Unit, isLoading: Boolean) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("personal") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo ví mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên ví") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Loại ví", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "personal",
                        onClick = { type = "personal" },
                        label = { Text("Ví riêng") }
                    )
                    FilterChip(
                        selected = type == "shared",
                        onClick = { type = "shared" },
                        label = { Text("Ví chung") }
                    )
                }
                if (type == "shared") {
                    Text(
                        "Ví chung cho phép mời thành viên cùng quản lý",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, type) },
                enabled = !isLoading && name.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Tạo")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageWalletDialog(
    wallet: Wallet,
    onDismiss: () -> Unit,
    onInvite: (email: String, role: String) -> Unit,
    onRemove: (userId: String) -> Unit,
    onUpdateRole: (userId: String, role: String) -> Unit,
    actionState: UiState<Unit>
) {
    var inviteEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quản lý: ${wallet.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Participant list
                Text("Thành viên (${wallet.participants.size})", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                wallet.participants.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.email.ifBlank { p.userId.take(12) + "..." }, fontSize = 13.sp)
                        }
                        RoleBadge(p.role)
                        if (p.role != "owner" && wallet.myRole == "owner") {
                            IconButton(
                                onClick = { onRemove(p.userId) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (wallet.myRole == "owner" && wallet.type == "shared") {
                    HorizontalDivider()
                    Text("Mời thành viên", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Vai trò: Đồng sở hữu", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    if (actionState is UiState.Error) {
                        Text((actionState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onInvite(inviteEmail, "co-owner") },
                        enabled = inviteEmail.isNotBlank() && actionState !is UiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (actionState is UiState.Loading)
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Mời")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}
