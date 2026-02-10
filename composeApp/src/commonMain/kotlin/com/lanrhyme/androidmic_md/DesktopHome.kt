package com.lanrhyme.androidmic_md

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun DesktopHome(
    viewModel: MainViewModel,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val audioLevel by viewModel.audioLevels.collectAsState(initial = 0f)
    val platform = remember { getPlatform() }
    
    // Startup Animation
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = tween(500)
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500)
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxSize().graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 左侧：状态与信息
            Card(
                modifier = Modifier.weight(1.2f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("AndroidMic Desktop", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SelectionContainer {
                         Text("本机 IP: ${platform.ipAddress}", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("连接方式", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.mode == ConnectionMode.Wifi,
                            onClick = { viewModel.setMode(ConnectionMode.Wifi) },
                            label = { Text("Wi-Fi") },
                            leadingIcon = { if (state.mode == ConnectionMode.Wifi) Icon(Icons.Filled.Check, null) else null }
                        )
                        FilterChip(
                            selected = state.mode == ConnectionMode.Usb,
                            onClick = { viewModel.setMode(ConnectionMode.Usb) },
                            label = { Text("USB") },
                            leadingIcon = { if (state.mode == ConnectionMode.Usb) Icon(Icons.Filled.Check, null) else null }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.port,
                        onValueChange = { viewModel.setPort(it) },
                        label = { Text("端口") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val statusText = when(state.streamState) {
                        StreamState.Idle -> "空闲"
                        StreamState.Connecting -> "连接中..."
                        StreamState.Streaming -> "正在串流"
                        StreamState.Error -> "错误"
                    }
                    Text("状态: $statusText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(state.errorMessage ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // 中间：控制与可视化
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val isRunning = state.streamState == StreamState.Streaming
                    
                    // 背景可视化 (简单模拟)
                    if (isRunning) {
                        CircularProgressIndicator(
                            progress = { audioLevel },
                            modifier = Modifier.fillMaxSize(0.8f),
                            strokeWidth = 8.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                            trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f),
                        )
                    }

                    // 开关按钮
                    val buttonSize by animateDpAsState(if (isRunning) 72.dp else 64.dp)
                    val buttonColor by animateColorAsState(
                        if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    
                    IconButton(
                        onClick = { viewModel.toggleStream() },
                        modifier = Modifier.size(buttonSize).background(buttonColor, CircleShape)
                    ) {
                        Icon(
                            if (isRunning) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = "Toggle Stream",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // 右侧：系统操作
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 窗口控制
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
                IconButton(onClick = onMinimize) {
                    Icon(Icons.Filled.Minimize, contentDescription = "Minimize")
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 设置
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        }
    }
}
