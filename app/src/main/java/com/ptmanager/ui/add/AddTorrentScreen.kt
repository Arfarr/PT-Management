package com.ptmanager.ui.add

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ptmanager.ui.appViewModel
import com.ptmanager.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTorrentScreen(
    serverId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val vm: AddTorrentViewModel = appViewModel { AddTorrentViewModel(it, context.applicationContext, serverId) }
    val state by vm.ui.collectAsState()

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: Exception) {
                // 无法持久授权时仅限当前会话内读取
            }
        }
        vm.addFiles(uris)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("添加种子") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    TextButton(
                        onClick = { onBack() },
                        enabled = !state.adding,
                    ) { Text("取消") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Text("URL / 磁力链接", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = state.urlsText,
                onValueChange = vm::setUrls,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("每行一个，支持 magnet:? 与 http(s) 链接") },
                minLines = 3,
                maxLines = 6,
            )
            Row {
                OutlinedButton(
                    onClick = { vm.readClipboard() },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.ContentPaste, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("从剪贴板读取")
                }
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("本地 .torrent 文件", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            state.files.forEachIndexed { index, file ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (file.size >= 0) Formatters.bytes(file.size) else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = { vm.removeFile(index) }) {
                            Icon(Icons.Default.Close, "移除")
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { fileLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("选择 torrent 文件") }

            HorizontalDivider()

            Text("选项", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = state.category,
                onValueChange = vm::setCategory,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("分类（可选）") },
                singleLine = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.paused, onCheckedChange = vm::setPaused)
                Text("添加后保持暂停（不立即开始）")
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    vm.add(
                        onDone = { onBack() },
                        onError = { },
                    )
                },
                enabled = !state.adding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.adding) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("正在添加…")
                } else {
                    Text("添加并开始下载")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}