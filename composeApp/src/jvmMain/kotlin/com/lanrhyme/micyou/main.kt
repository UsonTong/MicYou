package com.lanrhyme.micyou

// import androidx.compose.ui.window.Tray // Remove official Tray
// import io.github.kdroidfilter.composenativetray.Tray // Remove failing dependency
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.runBlocking
import micyou.composeapp.generated.resources.Res
import micyou.composeapp.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Font
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.imageio.ImageIO
import javax.swing.UIManager
import kotlin.system.exitProcess

fun main() {
    Logger.init(JvmLogger())
    // 强制设置编码，必须在所有 GUI 初始化之前设置
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
        // Windows 默认字体
        var fontName = "Microsoft YaHei"
        
        // 检测操作系统，如果是 Linux 则尝试使用系统默认字体或常见的中文字体
        val os = System.getProperty("os.name").lowercase()
        if (os.contains("linux")) {
            fontName = "WenQuanYi Micro Hei" // Linux 上常见的中文字体
        }
        
        val font = Font(fontName, Font.PLAIN, 12)
        val keys = arrayOf(
            "MenuItem.font", "Menu.font", "PopupMenu.font", 
            "CheckBoxMenuItem.font", "RadioButtonMenuItem.font",
            "Label.font", "Button.font", "ToolTip.font"
        )
        
        for (key in keys) {
            UIManager.put(key, font)
        }
        
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
        val isStreaming = streamState == StreamState.Streaming || streamState == StreamState.Connecting

        // 图标资源
        val icon = painterResource(Res.drawable.app_icon)
        
        // 自定义托盘菜单状态
        var isTrayMenuOpen by remember { mutableStateOf(false) }
        var trayMenuPosition by remember { mutableStateOf<WindowPosition>(WindowPosition(Alignment.Center)) }

        // AWT SystemTray Implementation
        DisposableEffect(Unit) {
            val tray = SystemTray.getSystemTray()
            val image = try {
                // 尝试从资源加载图标
                val resourcePath = "composeResources/micyou.composeapp.generated.resources/drawable/app_icon.png"
                val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
                if (stream != null) {
                    ImageIO.read(stream)
                } else {
                    // 如果找不到资源，尝试加载一个简单的 BufferedImage 或者报错
                    // 这里可以回退到默认图标或继续尝试其他路径
                     Logger.e("Tray", "Icon resource not found at $resourcePath")
                     // 尝试直接用 ImageIO 读取文件（仅用于调试，实际打包后可能不可用）
                     null
                }
            } catch (e: Exception) {
                Logger.e("Tray", "Failed to load icon: ${e.message}")
                null
            }

            if (image != null) {
                val trayIcon = TrayIcon(image, "MicYou")
                trayIcon.isImageAutoSize = true
                
                trayIcon.addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (e.button == MouseEvent.BUTTON1) { // 左键
                            isVisible = !isVisible
                        } else if (e.button == MouseEvent.BUTTON3) { // 右键
                            val point = e.point
                            trayMenuPosition = WindowPosition(point.x.dp, point.y.dp)
                            isTrayMenuOpen = true
                        }
                    }
                })
                
                try {
                    tray.add(trayIcon)
                } catch (e: Exception) {
                    Logger.e("Tray", "Failed to add tray icon: ${e.message}")
                }
                
                onDispose {
                    tray.remove(trayIcon)
                }
            } else {
                onDispose {}
            }
        }
        
        // 自定义托盘菜单窗口
        if (isTrayMenuOpen) {
            Window(
                onCloseRequest = { isTrayMenuOpen = false },
                visible = true,
                title = "Tray Menu",
                state = rememberWindowState(
                    position = trayMenuPosition,
                    width = 160.dp, // 菜单宽度
                    height = 180.dp // 菜单高度，根据项目数量调整
                ),
                undecorated = true,
                transparent = true,
                alwaysOnTop = true,
                resizable = false,
                focusable = true // 必须可聚焦才能检测失去焦点
            ) {
                // 监听窗口失去焦点事件以关闭菜单
                DisposableEffect(Unit) {
                    val window = this@Window.window
                    val focusListener = object : WindowAdapter() {
                        override fun windowLostFocus(e: WindowEvent?) {
                            isTrayMenuOpen = false
                        }
                    }
                    window.addWindowFocusListener(focusListener)
                    onDispose {
                        window.removeWindowFocusListener(focusListener)
                    }
                }
                
                // 应用主题以支持深色模式
                val themeMode by viewModel.uiState.collectAsState().let { state ->
                    derivedStateOf { state.value.themeMode }
                }
                val seedColor by viewModel.uiState.collectAsState().let { state ->
                    derivedStateOf { state.value.seedColor }
                }
                val seedColorObj = androidx.compose.ui.graphics.Color(seedColor.toInt())
                
                AppTheme(themeMode = themeMode, seedColor = seedColorObj) {
                    Card(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp)
                        ) {
                            // 菜单项组件
                            @Composable
                            fun TrayMenuItem(text: String, onClick: () -> Unit) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            onClick()
                                            isTrayMenuOpen = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = text,
                                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            TrayMenuItem(
                                text = if (isVisible) strings.trayHide else strings.trayShow,
                                onClick = { isVisible = !isVisible }
                            )
                            TrayMenuItem(
                                text = if (isStreaming) strings.stop else strings.start,
                                onClick = { viewModel.toggleStream() }
                            )
                            TrayMenuItem(
                                text = strings.settingsTitle,
                                onClick = {
                                    isSettingsOpen = true
                                    isVisible = true
                                }
                            )
                            TrayMenuItem(
                                text = strings.trayExit,
                                onClick = {
                                    runBlocking {
                                        VBCableManager.setSystemDefaultMicrophone(toCable = false)
                                    }
                                    exitProcess(0)
                                }
                            )
                        }
                    }
                }
            }
        }

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
                            runBlocking {
                                VBCableManager.setSystemDefaultMicrophone(toCable = false)
                            }
                            exitProcess(0)
                        },
                        onHide = { isVisible = false }
                    )
                },
                state = windowState,
                title = strings.appName,
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
                                    runBlocking {
                                        VBCableManager.setSystemDefaultMicrophone(toCable = false)
                                    }
                                    exitProcess(0)
                                },
                                onHide = { isVisible = false }
                            )
                        },
                        onExitApp = { 
                            runBlocking {
                                VBCableManager.setSystemDefaultMicrophone(toCable = false)
                            }
                            exitProcess(0)
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
                title = strings.settingsTitle,
                icon = icon,
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
