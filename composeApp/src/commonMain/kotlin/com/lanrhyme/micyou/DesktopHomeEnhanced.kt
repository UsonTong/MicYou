package com.lanrhyme.micyou

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lanrhyme.micyou.animation.EasingFunctions
import kotlinx.coroutines.delay
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopHomeEnhanced(
    viewModel: MainViewModel,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onExitApp: () -> Unit,
    onHideApp: () -> Unit,
    onOpenSettings: () -> Unit,
    isBluetoothDisabled: Boolean = false
) {
    val state by viewModel.uiState.collectAsState()
    val audioLevel by viewModel.audioLevels.collectAsState(initial = 0f)
    val platform = remember { getPlatform() }
    val strings = LocalAppStrings.current
    
    var visible by remember { mutableStateOf(false) }
    var cardsVisible by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, easing = EasingFunctions.EaseOutExpo)
    )
    
    LaunchedEffect(Unit) {
        visible = true
        delay(100)
        cardsVisible = true
    }

    LaunchedEffect(isBluetoothDisabled, state.mode) {
        if (isBluetoothDisabled && state.mode == ConnectionMode.Bluetooth) {
            viewModel.setMode(ConnectionMode.Wifi)
        }
    }

    if (state.installMessage != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(strings.systemConfigTitle) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(state.installMessage ?: "")
                }
            },
            confirmButton = {}
        )
    }

    if (state.showFirewallDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFirewallDialog() },
            title = { Text(strings.firewallTitle) },
            text = { Text(strings.firewallMessage.replace("%d", state.pendingFirewallPort?.toString() ?: "")) },
            confirmButton = {
                Button(onClick = { viewModel.confirmAddFirewallRule() }) {
                    Text(strings.firewallConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissFirewallDialog() }) {
                    Text(strings.firewallDismiss)
                }
            }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedCard(
                visible = cardsVisible,
                delayMillis = 100,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                NetworkPanel(
                    state = state,
                    viewModel = viewModel,
                    platform = platform,
                    strings = strings,
                    isBluetoothDisabled = isBluetoothDisabled
                )
            }

            AnimatedCard(
                visible = cardsVisible,
                delayMillis = 200,
                modifier = Modifier.weight(1.4f).fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                ControlPanel(
                    state = state,
                    viewModel = viewModel,
                    audioLevel = audioLevel,
                    strings = strings
                )
            }

            AnimatedCard(
                visible = cardsVisible,
                delayMillis = 300,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                StatusPanel(
                    state = state,
                    viewModel = viewModel,
                    onMinimize = onMinimize,
                    onClose = onClose,
                    onOpenSettings = onOpenSettings,
                    strings = strings
                )
            }
        }
    }
}

@Composable
private fun AnimatedCard(
    visible: Boolean,
    delayMillis: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit
) {
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, delayMillis, easing = EasingFunctions.EaseOutExpo)
    )
    val cardScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
            visibilityThreshold = 0.001f
        )
    )
    val cardOffsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 30f,
        animationSpec = tween(500, delayMillis, easing = EasingFunctions.EaseOutExpo)
    )

    Card(
        modifier = modifier.graphicsLayer {
            this.alpha = cardAlpha
            this.scaleX = cardScale
            this.scaleY = cardScale
            translationY = cardOffsetY
        },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(22.dp)
    ) {
        content()
    }
}

@Composable
private fun NetworkPanel(
    state: AppUiState,
    viewModel: MainViewModel,
    platform: Platform,
    strings: AppStrings,
    isBluetoothDisabled: Boolean
) {
    var titleVisible by remember { mutableStateOf(false) }
    var fieldsVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        titleVisible = true
        delay(100)
        fieldsVisible = true
    }

    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { -20 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ),
                exit = fadeOut(tween(200))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Router,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            "MicYou Desktop",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Server",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(tween(300, 100)),
                exit = fadeOut(tween(200))
            ) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
            
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(tween(300, 150)),
                exit = fadeOut(tween(200))
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            SelectionContainer {
                                Text(
                                    platform.ipAddress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "LAN",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = fieldsVisible,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ),
            exit = fadeOut(tween(200))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    strings.connectionModeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                ConnectionModeSelector(
                    selectedMode = state.mode,
                    onModeSelected = { viewModel.setMode(it) },
                    isBluetoothDisabled = isBluetoothDisabled,
                    strings = strings
                )

                AnimatedVisibility(
                    visible = state.mode != ConnectionMode.Bluetooth,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = state.port,
                        onValueChange = { viewModel.setPort(it) },
                        label = { Text(strings.portLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionModeSelector(
    selectedMode: ConnectionMode,
    onModeSelected: (ConnectionMode) -> Unit,
    isBluetoothDisabled: Boolean,
    strings: AppStrings
) {
    val modes = listOfNotNull(
        ConnectionMode.Wifi to (strings.modeWifi to Icons.Rounded.Wifi),
        if (!isBluetoothDisabled) ConnectionMode.Bluetooth to (strings.modeBluetooth to Icons.Rounded.Bluetooth) else null,
        ConnectionMode.Usb to (strings.modeUsb to Icons.Rounded.Usb)
    )
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            modes.forEach { (mode, info) ->
                val (label, icon) = info
                val isSelected = selectedMode == mode
                
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = tween(200)
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200)
                )
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = bgColor,
                    modifier = Modifier.weight(1f).height(44.dp).clickable { onModeSelected(mode) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlPanel(
    state: AppUiState,
    viewModel: MainViewModel,
    audioLevel: Float,
    strings: AppStrings
) {
    var contentVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(200)
        contentVisible = true
    }
    
    val isRunning = state.streamState == StreamState.Streaming
    val isConnecting = state.streamState == StreamState.Connecting

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isRunning) {
            AdvancedAudioVisualizer(
                modifier = Modifier.fillMaxSize(0.85f),
                audioLevel = audioLevel,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (isConnecting) {
            ConnectingAnimation(
                modifier = Modifier.fillMaxSize(0.85f),
                color = MaterialTheme.colorScheme.primary
            )
        }

        MainControlButton(
            isRunning = isRunning,
            isConnecting = isConnecting,
            onClick = {
                if (isRunning || isConnecting) viewModel.stopStream() else viewModel.startStream()
            },
            strings = strings,
            visible = contentVisible
        )
    }
}

@Composable
private fun AdvancedAudioVisualizer(
    modifier: Modifier = Modifier,
    audioLevel: Float,
    color: Color
) {
    val safeAudioLevel = audioLevel.coerceIn(0f, 1f)
    
    val infiniteTransition = rememberInfiniteTransition(label = "AudioVisualizer")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EasingFunctions.EaseInOutExpo),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breath"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Wave"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EasingFunctions.EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )
    
    Canvas(modifier = modifier.graphicsLayer { 
        scaleX = breathScale
        scaleY = breathScale
    }) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = min(size.width, size.height) / 2
        
        for (i in 0..3) {
            val waveRadius = baseRadius * (0.6f + i * 0.12f * safeAudioLevel)
            val alpha = (0.4f - i * 0.1f) * safeAudioLevel
            
            drawCircle(
                color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = waveRadius,
                center = center,
                style = Stroke(width = (3 - i * 0.5f).dp.toPx())
            )
        }
        
        val barCount = 36
        for (i in 0 until barCount) {
            val angle = (i.toFloat() / barCount) * 360f + wavePhase
            val radians = Math.toRadians(angle.toDouble()).toFloat()
            
            val dynamicLevel = safeAudioLevel * (0.5f + 0.5f * sin(angle * 0.05f + wavePhase * 0.02f))
            val barHeight = baseRadius * 0.15f * dynamicLevel
            
            val innerRadius = baseRadius * 0.55f
            val startX = center.x + innerRadius * cos(radians)
            val startY = center.y + innerRadius * sin(radians)
            val endX = center.x + (innerRadius + barHeight) * cos(radians)
            val endY = center.y + (innerRadius + barHeight) * sin(radians)
            
            drawLine(
                color = color.copy(alpha = 0.6f * safeAudioLevel),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        val glowSteps = 8
        for (i in 0 until glowSteps) {
            val progress = i.toFloat() / glowSteps
            val glowRadius = baseRadius * 0.3f * (1f + progress * 0.5f)
            val alpha = glowAlpha * (1f - progress) * safeAudioLevel
            
            drawCircle(
                color = color.copy(alpha = alpha.coerceIn(0f, 0.3f)),
                radius = glowRadius,
                center = center
            )
        }
    }
}

@Composable
private fun ConnectingAnimation(
    modifier: Modifier = Modifier,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ConnectingAnim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EasingFunctions.EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )
    
    Canvas(modifier = modifier.graphicsLayer { 
        scaleX = pulse
        scaleY = pulse
    }) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = min(size.width, size.height) / 2
        
        for (i in 0..2) {
            val arcAngle = rotation + i * 120f
            val sweepAngle = 60f + 20f * sin(rotation * 0.02f)
            
            drawArc(
                color = color.copy(alpha = 0.4f - i * 0.1f),
                startAngle = arcAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius * (0.5f + i * 0.15f), center.y - radius * (0.5f + i * 0.15f)),
                size = Size(radius * 2 * (0.5f + i * 0.15f), radius * 2 * (0.5f + i * 0.15f)),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun MainControlButton(
    isRunning: Boolean,
    isConnecting: Boolean,
    onClick: () -> Unit,
    strings: AppStrings,
    visible: Boolean
) {
    val buttonSize by animateDpAsState(
        targetValue = if (isRunning) 88.dp else 76.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val buttonColor by animateColorAsState(
        targetValue = when {
            isRunning -> MaterialTheme.colorScheme.error
            isConnecting -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(400)
    )
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
    )
    
    val buttonAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, easing = EasingFunctions.EaseOutExpo)
    )
    val buttonScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "ButtonGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EasingFunctions.EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(buttonSize + 24.dp)
                .graphicsLayer {
                    this.alpha = buttonAlpha
                    scaleX = buttonScale * pressScale
                    scaleY = buttonScale * pressScale
                }
        ) {
            if (isRunning) {
                Canvas(modifier = Modifier.size(buttonSize + 20.dp)) {
                    drawCircle(
                        color = buttonColor.copy(alpha = glowAlpha * 0.3f),
                        radius = size.width / 2
                    )
                }
            }
            
            FloatingActionButton(
                onClick = onClick,
                interactionSource = interactionSource,
                containerColor = buttonColor,
                modifier = Modifier.size(buttonSize),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = if (isPressed) 2.dp else 8.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Icon(
                    if (isConnecting) Icons.Rounded.Refresh else if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = if (isRunning) strings.stop else strings.start,
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }
        
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(300, 200))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when {
                        isRunning -> strings.statusStreaming
                        isConnecting -> strings.statusConnecting
                        else -> strings.clickToStart
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = buttonColor
                )
                if (isRunning) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = buttonColor
                    ) {
                        Text(
                            "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPanel(
    state: AppUiState,
    viewModel: MainViewModel,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    strings: AppStrings
) {
    var contentVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(300)
        contentVisible = true
    }

    Column(
        modifier = Modifier.padding(12.dp).fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(300)) + slideInVertically(
                initialOffsetY = { -20 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ),
            exit = fadeOut(tween(200))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onMinimize, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Minimize, strings.minimize, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        strings.close,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(400, 100)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ),
            exit = fadeOut(tween(200))
        ) {
            StatusDisplay(
                streamState = state.streamState,
                errorMessage = state.errorMessage,
                strings = strings
            )
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(400, 200)) + slideInVertically(
                initialOffsetY = { 20 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ),
            exit = fadeOut(tween(200))
        ) {
            QuickActions(
                isMuted = state.isMuted,
                onToggleMute = { viewModel.toggleMute() },
                onOpenSettings = onOpenSettings,
                strings = strings
            )
        }
    }
}

@Composable
private fun StatusDisplay(
    streamState: StreamState,
    errorMessage: String?,
    strings: AppStrings
) {
    val statusColor by animateColorAsState(
        targetValue = when (streamState) {
            StreamState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
            StreamState.Connecting -> MaterialTheme.colorScheme.tertiary
            StreamState.Streaming -> MaterialTheme.colorScheme.primary
            StreamState.Error -> MaterialTheme.colorScheme.error
        },
        animationSpec = tween(300)
    )
    
    val statusIcon = when (streamState) {
        StreamState.Idle -> Icons.Rounded.Info
        StreamState.Connecting -> Icons.Rounded.HourglassTop
        StreamState.Streaming -> Icons.Rounded.CheckCircle
        StreamState.Error -> Icons.Rounded.Error
    }
    
    val statusText = when (streamState) {
        StreamState.Idle -> strings.statusIdle
        StreamState.Connecting -> strings.statusConnecting
        StreamState.Streaming -> strings.statusStreaming
        StreamState.Error -> strings.statusError
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = statusColor.copy(alpha = 0.1f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(28.dp))
            }
        }
        
        Text(
            statusText,
            style = MaterialTheme.typography.titleSmall,
            color = statusColor,
            fontWeight = FontWeight.SemiBold
        )
        
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 3,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActions(
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onOpenSettings: () -> Unit,
    strings: AppStrings
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val muteInteractionSource = remember { MutableInteractionSource() }
        val isMutePressed by muteInteractionSource.collectIsPressedAsState()
        val muteScale by animateFloatAsState(
            targetValue = if (isMutePressed) 0.85f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
        )
        
        val muteColor by animateColorAsState(
            targetValue = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(300)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = onToggleMute,
                interactionSource = muteInteractionSource,
                modifier = Modifier.size(40.dp).scale(muteScale)
            ) {
                Icon(
                    if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    contentDescription = if (isMuted) strings.unmuteLabel else strings.muteLabel,
                    modifier = Modifier.size(20.dp),
                    tint = muteColor
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Rounded.Settings, strings.settingsTitle, modifier = Modifier.size(20.dp))
            }
        }
    }
}
