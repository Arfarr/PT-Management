package com.ptmanager.ui.detail

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ptmanager.data.model.Tracker
import com.ptmanager.ui.appViewModel
import com.ptmanager.util.Formatters
import com.ptmanager.util.StateColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentDetailScreen(
    serverId: String,
    torrentHash: String,
    onBack: () -> Unit,
) {
    val vm: TorrentDetailViewModel = appViewModel { TorrentDetailViewModel(it, serverId, torrentHash) }
    val state by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("种子详情", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            val info = state.info
            if (info != null) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val isPaused = info.state.startsWith("paused")
                        Button(
                            onClick = {
                                vm.runAction(
                                    { r -> if (isPaused) r.start(listOf(torrentHash)) else r.stop(listOf(torrentHash)) },
                                    onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
                                )
                            },
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isPaused) "恢复" else "暂停")
                        }
                        OutlinedButton(
                            onClick = {
                                vm.runAction(
                                    { r -> r.recheck(listOf(torrentHash)) },
                                    onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
                                )
                            },
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Rule, null)
                            Spacer(Modifier.width(4.dp))
                            Text("校验")
                        }
                        Button(
                            onClick = { showDeleteDialog = true },
                            enabled = !state.busy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Delete, null)
                            Spacer(Modifier.width(4.dp))
                            Text("删除")
                        }
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.loading && state.info == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.info == null -> {
                Column(
                    Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("加载失败: ${state.error}")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.refresh() }) { Text("重试") }
                }
            }
            else -> {
                val info = state.info
                val props = state.properties
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (info != null) {
                        item { DetailHeader(info) }
                        item { StatsSection(info, props) }
                    }
                    item { TrackerSection(state.trackers) }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (showDeleteDialog) {
        var deleteFiles by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除种子") },
            text = {
                Column {
                    Text("确定删除该种子吗？")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = deleteFiles, onCheckedChange = { deleteFiles = it })
                        Text("同时删除本地文件", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm.runAction(
                        action = { r -> r.delete(listOf(torrentHash), deleteFiles) },
                        onDone = { onBack() },
                        onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
                    )
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun DetailHeader(t: com.ptmanager.data.model.TorrentInfo) {
    val stateColor = StateColor.colorFor(t.state)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(t.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(stateColor).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        t.displayState(),
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(stateColor),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text("${Formatters.percent(t.progress)}  ETA ${Formatters.eta(t.eta)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { t.progress.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(stateColor),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row {
                SpeedStat("下载", Formatters.speed(t.dlspeed), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                SpeedStat("上传", Formatters.speed(t.upspeed), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                SpeedStat(
                    "分享率",
                    Formatters.ratio(t.ratio),
                    MaterialTheme.colorScheme.secondary,
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("↑ ${Formatters.bytes(t.uploaded)}", style = MaterialTheme.typography.labelMedium)
                Text("↓ ${Formatters.bytes(t.downloaded)}", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text("做种 ${t.numSeeds} / 下载 ${t.numLeechs}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SpeedStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("$label ${value}", style = MaterialTheme.typography.titleSmall, color = color)
    }
}

@Composable
private fun StatsSection(info: com.ptmanager.data.model.TorrentInfo, props: com.ptmanager.data.model.TorrentProperties?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("基本信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            StatRow("大小", Formatters.bytes(info.size))
            StatRow("已完成", "${Formatters.bytes(info.downloaded)} / ${Formatters.bytes(info.size)}")
            StatRow("剩余", Formatters.bytes(info.amountLeft))
            StatRow("添加时间", Formatters.date(info.addedOn))
            StatRow("分类", info.category.ifBlank { "-" })
            StatRow("标签", info.tags.ifBlank { "-" })
            StatRow("Tracker", if (info.tracker.isEmpty()) info.trackersCount.toString() + " 个" else info.tracker)
            props?.let { p ->
                StatRow("保存路径", p.savePath)
                StatRow("创建者", p.createdBy.ifBlank { "-" })
                StatRow("完成时间", Formatters.date(p.completionDate))
                StatRow("做种时间", Formatters.eta(p.seedingTime))
                StatRow("块大小", Formatters.bytes(p.pieceSize))
                StatRow("连接数", "${p.nbConnections} / ${if (p.nbConnectionsLimit < 0) "不限" else p.nbConnectionsLimit}")
                StatRow("平均下载", Formatters.speed(p.dlSpeedAvg))
                StatRow("平均上传", Formatters.speed(p.upSpeedAvg))
                StatRow("做种时长限制", if (p.seedingTimeLimit > 0) Formatters.eta(p.seedingTimeLimit) else "不限")
                StatRow("私有种子", if (p.isPrivate) "是" else "否")
                StatRow("浪费量", Formatters.bytes(p.totalWasted))
                if (p.comment.isNotBlank()) StatRow("备注", p.comment)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.width(90.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TrackerSection(trackers: List<Tracker>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Tracker (${trackers.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (trackers.isEmpty()) {
                Text("没有 Tracker", color = MaterialTheme.colorScheme.outline)
            } else {
                trackers.forEachIndexed { i, tr ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Column {
                        Text(tr.url, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TrackerStatusBadge(tr)
                            Spacer(Modifier.weight(1f))
                            Text("做种 ${tr.numSeeds} / 下载 ${tr.numLeeches}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        if (tr.msg.isNotBlank()) {
                            Text(tr.msg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerStatusBadge(tr: Tracker) {
    val ok = tr.status == 2 || tr.status == 3
    Surface(color = (if (ok) StateColor.downloading else StateColor.error).toColor().copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
        Text(
            tr.statusText,
            Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            color = (if (ok) StateColor.downloading else StateColor.error).toColor(),
        )
    }
}

private fun Int.toColor(): Color = Color(this)