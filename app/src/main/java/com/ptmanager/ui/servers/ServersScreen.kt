package com.ptmanager.ui.servers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ptmanager.data.model.ServerConfig
import com.ptmanager.ui.appViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(onServerOpened: (String) -> Unit) {
    val vm: ServersViewModel = appViewModel { ServersViewModel(it) }
    val state by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()

    var editTarget by remember { mutableStateOf<ServerConfig?>(null) }
    var showEditNew by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ServerConfig?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("PT管理宝") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditNew = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加服务器")
            }
        },
    ) { padding ->
        if (state.servers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Link, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))
                    Text("还没有添加下载服务器", style = MaterialTheme.typography.bodyLarge)
                    Text("点击右下角 + 添加 qBittorrent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.servers, key = { it.id }) { server ->
                    ServerCard(
                        server = server,
                        active = server.id == state.activeId,
                        onClick = { vm.open(server.id); onServerOpened(server.id) },
                        onEdit = { editTarget = server },
                        onDelete = { deleteTarget = server },
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showEditNew || editTarget != null) {
        val target = editTarget
        EditServerDialog(
            initial = target,
            onTest = { vm.testConnection(it) },
            onDismiss = { showEditNew = false; editTarget = null },
            onSave = { config ->
                vm.save(config, isNew = target == null)
                showEditNew = false
                editTarget = null
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除服务器") },
            text = { Text("确定删除「${target.name}」吗？其凭据将被移除。") },
            confirmButton = {
                TextButton(onClick = { vm.remove(target.id); deleteTarget = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun ServerCard(
    server: ServerConfig,
    active: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (active) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    server.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (server.username.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(server.username, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "编辑") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun EditServerDialog(
    initial: ServerConfig?,
    onTest: suspend (ServerConfig) -> Result<String>,
    onDismiss: () -> Unit,
    onSave: (ServerConfig) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var url by remember { mutableStateOf(initial?.baseUrl ?: "") }
    var username by remember { mutableStateOf(initial?.username ?: "") }
    var password by remember { mutableStateOf(initial?.password ?: "") }
    var testing by remember { mutableStateOf(false) }
    var testState by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val current = ServerConfig(
        id = initial?.id ?: remember { ServersViewModel.newId() },
        name = name,
        baseUrl = url,
        username = username,
        password = password,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加服务器" else "编辑服务器") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("地址（如 http://192.168.1.10:8080）") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    label = { Text("用户名") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("密码") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                val tint = when {
                    testState == null -> MaterialTheme.colorScheme.outline
                    testState!!.startsWith("✓") -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                }
                Text(
                    testState ?: "建议先测试连接再保存",
                    style = MaterialTheme.typography.bodySmall,
                    color = tint,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    enabled = !testing && url.isNotBlank(),
                    onClick = {
                        testing = true
                        testState = null
                        scope.launch {
                            val r = onTest(current)
                            testing = false
                            testState = if (r.isSuccess) "✓ ${r.getOrNull()}" else "✗ ${r.exceptionOrNull()?.message ?: "连接失败"}"
                        }
                    },
                ) { Text(if (testing) "测试中…" else "测试连接") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(
                    onClick = { onSave(current) },
                    enabled = !testing && name.isNotBlank() && url.isNotBlank(),
                ) { Text("保存") }
            }
        },
    )
}