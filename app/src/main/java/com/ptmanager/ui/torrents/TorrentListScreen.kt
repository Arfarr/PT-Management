package com.ptmanager.ui.torrents

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ptmanager.data.model.TorrentInfo
import com.ptmanager.ui.appViewModel
import com.ptmanager.util.Formatters
import com.ptmanager.util.StateColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentListScreen(
    serverId: String,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onAddTorrent: () -> Unit,
) {
    val vm: TorrentListViewModel = appViewModel { TorrentListViewModel(it, serverId) }
    val state by vm.ui.collectAsState()
    val torrents = vm.filteredTorrents(state)
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(state.serverName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, "刷新", tint = if (state.connected) Color.Unspecified else MaterialTheme.colorScheme.error)
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) { Icon(Icons.Default.Sort, "排序") }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            state.sort.let { current ->
                                TorrentSort.entries.forEach { s ->
                                    val selected = s == current
                                    DropdownMenuItem(
                                        text = { Text(if (selected) "${s.label} ${if (state.sortDesc) "↓" else "↑"}" else s.label) },
                                        leadingIcon = {
                                            if (selected) Icon(Icons.Default.SwapVert, null, Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            vm.setSort(s)
                                            showSortMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTorrent) { Icon(Icons.Default.Add, "添加种子") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.transfer?.let { t ->
                GlobalSpeedBar(dl = t.dlInfoSpeed, up = t.upInfoSpeed, connection = state.connected)
            }
            FilterChipsRow(
                filters = TorrentFilter.entries,
                selected = state.filter,
                onSelect = vm::setFilter,
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("搜索种子名称…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
            )
            when {
                state.loading && torrents.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                state.error != null && torrents.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("连接失败: ${state.error}", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { vm.refresh() }) { Text("重试") }
                    }
                }
                torrents.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("没有种子", color = MaterialTheme.colorScheme.outline)
                    }
                }
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item { Text("共 ${state.torrents.size} 个种子", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
                        items(torrents, key = { it.hash }) { t ->
                            TorrentItem(t, onClick = { onOpenDetail(t.hash) })
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalSpeedBar(dl: Long, up: Long, connection: Boolean) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.ArrowDownward, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Text(" ${Formatters.speed(dl)}  ", style = MaterialTheme.typography.titleSmall)
            Icon(Icons.Default.ArrowUpward, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
            Text(" ${Formatters.speed(up)}", style = MaterialTheme.typography.titleSmall)
Spacer(Modifier.weight(1f))
                        HorizontalDivider(modifier = Modifier.width(1.dp).height(16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.width(8.dp))
            Text(
                if (connection) "已连接" else "连接失败",
                style = MaterialTheme.typography.labelSmall,
                color = if (connection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    filters: List<TorrentFilter>,
    selected: TorrentFilter,
    onSelect: (TorrentFilter) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { f ->
            FilterChip(
                selected = f == selected,
                onClick = { onSelect(f) },
                label = { Text(f.label) },
            )
        }
    }
}

@Composable
fun TorrentItem(t: TorrentInfo, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    t.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StateBadge(t)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { t.progress.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(StateColor.colorFor(t.state)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${if (t.isCompleted) "已完成" else "下载中"} ${Formatters.percent(t.progress)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(StateColor.colorFor(t.state)),
                )
                Spacer(Modifier.weight(1f))
                Text(Formatters.eta(t.eta), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("↓ ${Formatters.speed(t.dlspeed)}", style = MaterialTheme.typography.labelSmall)
                Text("  ↑ ${Formatters.speed(t.upspeed)}", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Text("分享率 ${Formatters.ratio(t.ratio)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (t.isCompleted) Formatters.bytes(t.size) else "${Formatters.bytes(t.size - t.amountLeft)}/${Formatters.bytes(t.size)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun StateBadge(t: TorrentInfo) {
    val stateColor = StateColor.colorFor(t.state)
    Surface(color = Color(stateColor).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
        Text(
            t.displayState(),
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(stateColor),
        )
    }
}