package com.lanrhyme.micyou

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import java.awt.Font
import java.awt.SystemColor
import javax.swing.UIManager
import micyou.composeapp.generated.resources.Res
import micyou.composeapp.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource

fun main() {
    Logger.init(JvmLogger())
    // 设置编码属性以尝试修复 AWT 乱码
    System.setProperty("file.encoding", "UTF-8")
    System.setProperty("sun.jnu.encoding", "UTF-8")
    // 强制 AWT 使用 Unicode 并解决部分渲染问题
    System.setProperty("sun.java2d.noddraw", "true")
    System.setProperty("sun.java2d.d3d", "false") // 禁用 D3D 尝试解决部分系统黑屏
    
    // 修复 Windows 10 上可能出现的透明/黑色窗口问题 (渲染兼容性)
    // 优先尝试使用 SOFTWARE_FAST，这在老旧设备或驱动不兼容的 Win10 上最稳定
    System.setProperty("skiko.renderApi", "SOFTWARE_FAST")
    System.setProperty("skiko.vsync", "false") // 禁用 vsync 解决部分显卡导致的渲染延迟
    System.setProperty("skiko.fps.enabled", "false")

    // 设置全局 Swing 属性以修复托盘菜单乱码
    try {
        val font = Font("Microsoft YaHei", Font.PLAIN, 12)
        UIManager.put("MenuItem.font", font)
        UIManager.put("Menu.font", font)
        UIManager.put("PopupMenu.font", font)
        UIManager.put("CheckBoxMenuItem.font", font)
        UIManager.put("RadioButtonMenuItem.font", font)
        UIManager.put("Label.font", font)
        UIManager.put("Button.font", font)
        
        // 尝试使用系统外观
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (e: Exception) {
        e.printStackTrace()
    }

    Logger.i("Main", "App started")
    application {
        val viewModel = remember { MainViewModel() }
        var isSettingsOpen by remember { mutableStateOf(false) }
        var isVisible by remember { mutableStateOf(true) }

        val language by viewModel.uiState.collectAsState().let { state ->
            derivedStateOf { state.value.language }
        }
        val strings = getStrings(language)

        val streamState by viewModel.uiState.collectAsState().let { state ->
            derivedStateOf { state.value.streamState }
        }

        // System Tray
        val trayState = rememberTrayState()
        val icon = painterResource(Res.drawable.app_icon)

        Tray(
            state = trayState,
            icon = icon,
            tooltip = "MicYou",
            onAction = { isVisible = true }, // Left click to show window
            menu = {
                Item(
                    text = if (isVisible) "Hide Window" else "Show Window",
                    onClick = { isVisible = !isVisible }
                )
                Item(
                    text = if (streamState == StreamState.Streaming || streamState == StreamState.Connecting) "Disconnect" else "Connect",
                    onClick = { viewModel.toggleStream() }
                )
                Item(
                    text = "Settings",
                    onClick = { 
                        isSettingsOpen = true
                        isVisible = true 
                    }
                )
                Separator()
                Item(
                    text = "Exit",
                    onClick = { 
                        // 退出前尝试恢复默认麦克风 (使用 runBlocking 确保在进程结束前完成)
                        kotlinx.coroutines.runBlocking {
                            VBCableManager.setSystemDefaultMicrophone(toCable = false)
                        }
                        exitApplication() 
                    }
                )
            }
        )

        // Main Window State
        val windowState = rememberWindowState(
            width = 600.dp, 
            height = 240.dp,
            position = WindowPosition(Alignment.Center)
        )

        // Main Window (Undecorated)
        if (isVisible) {
            Window(
                onCloseRequest = { 
                    viewModel.handleCloseRequest(
                        onExit = { 
                            kotlinx.coroutines.runBlocking {
                                VBCableManager.setSystemDefaultMicrophone(toCable = false)
                            }
                            exitApplication() 
                        },
                        onHide = { isVisible = false }
                    )
                },
                state = windowState,
                title = "MicYou",
                icon = icon,
                undecorated = true,
                transparent = true, // Allows rounded corners via Surface in DesktopHome
                resizable = false
            ) {
                WindowDraggableArea {
                    App(
                        viewModel = viewModel,
                        onMinimize = { windowState.isMinimized = true },
                        onClose = { 
                            viewModel.handleCloseRequest(
                                onExit = { 
                                    kotlinx.coroutines.runBlocking {
                                        VBCableManager.setSystemDefaultMicrophone(toCable = false)
                                    }
                                    exitApplication() 
                                },
                                onHide = { isVisible = false }
                            )
                        },
                        onExitApp = { 
                            kotlinx.coroutines.runBlocking {
                                VBCableManager.setSystemDefaultMicrophone(toCable = false)
                            }
                            exitApplication() 
                        },
                        onHideApp = { isVisible = false },
                        onOpenSettings = { isSettingsOpen = true }
                    )
                }
            }
        }

    // Settings Window
    if (isSettingsOpen) {
        val settingsState = rememberWindowState(
            width = 530.dp,
            height = 500.dp,
            position = WindowPosition(Alignment.Center)
        )
        
        Window(
            onCloseRequest = { isSettingsOpen = false },
            state = settingsState,
            title = "Settings",
            icon = painterResource(Res.drawable.app_icon),
            resizable = false
        ) {
            // Re-use theme logic from AppTheme but apply to settings window content
            val themeMode by viewModel.uiState.collectAsState().let { state ->
                derivedStateOf { state.value.themeMode }
            }
            val seedColor by viewModel.uiState.collectAsState().let { state ->
                derivedStateOf { state.value.seedColor }
            }
            val language by viewModel.uiState.collectAsState().let { state ->
                derivedStateOf { state.value.language }
            }
            val seedColorObj = androidx.compose.ui.graphics.Color(seedColor.toInt())
            val strings = getStrings(language)

            CompositionLocalProvider(LocalAppStrings provides strings) {
                AppTheme(themeMode = themeMode, seedColor = seedColorObj) {
                    DesktopSettings(
                        viewModel = viewModel,
                        onClose = { isSettingsOpen = false }
                    )
                }
            }
        }
    }
}
}

