package com.lanrhyme.micyou.network

import com.lanrhyme.micyou.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.BindException
import javax.bluetooth.DiscoveryAgent
import javax.bluetooth.LocalDevice
import javax.bluetooth.UUID
import javax.microedition.io.Connector
import javax.microedition.io.StreamConnection
import javax.microedition.io.StreamConnectionNotifier

/**
 * 管理网络服务器（TCP 或蓝牙）的生命周期。
 * 职责包括：
 * 1. 绑定端口/蓝牙 URL
 * 2. 接受传入连接
 * 3. 将连接处理委托给 ConnectionHandler
 * 4. 报告服务器状态
 */
class NetworkServer(
    private val onAudioPacketReceived: suspend (AudioPacketMessage) -> Unit,
    private val onMuteStateChanged: (Boolean) -> Unit
) {
    private val _state = MutableStateFlow(StreamState.Idle)
    val state = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    private var serverJob: Job? = null
    private var selectorManager: SelectorManager? = null
    
    // TCP 资源
    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    
    // 蓝牙资源
    private var btNotifier: StreamConnectionNotifier? = null
    private var activeBtConnection: StreamConnection? = null

    // 当前活动的连接处理器
    private var activeHandler: ConnectionHandler? = null

    suspend fun start(
        port: Int,
        mode: ConnectionMode
    ) {
        if (serverJob != null && serverJob!!.isActive) {
            Logger.w("NetworkServer", "服务器已在运行")
            return
        }

        _state.value = StreamState.Connecting
        _lastError.value = null

        coroutineScope {
            serverJob = launch(Dispatchers.IO) {
                try {
                    if (mode == ConnectionMode.Bluetooth) {
                        runBluetoothServer()
                    } else {
                        runTcpServer(port)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e("NetworkServer", "服务器致命错误", e)
                    _state.value = StreamState.Error
                    _lastError.value = "服务器错误: ${e.message}"
                } finally {
                    cleanup()
                    if (_state.value != StreamState.Error) {
                        _state.value = StreamState.Idle
                    }
                }
            }
        }
    }

    suspend fun stop() {
        serverJob?.cancel()
        serverJob?.join()
        cleanup()
    }

    suspend fun sendMuteState(muted: Boolean) {
        activeHandler?.sendMuteState(muted)
    }

    private suspend fun runTcpServer(port: Int) {
        try {
            selectorManager = SelectorManager(Dispatchers.IO)
            serverSocket = aSocket(selectorManager!!).tcp().bind("0.0.0.0", port = port)
            Logger.i("NetworkServer", "正在监听 TCP 端口 $port")
            
            while (currentCoroutineContext().isActive) {
                val socket = serverSocket?.accept() ?: break
                activeSocket = socket
                Logger.i("NetworkServer", "接受来自 ${socket.remoteAddress} 的 TCP 连接")
                
                handleConnection(
                    input = socket.openReadChannel(),
                    output = socket.openWriteChannel(autoFlush = true),
                    closeAction = { 
                        socket.close() 
                        activeSocket = null
                    }
                )
            }
        } catch (e: BindException) {
            val msg = "端口 $port 已被占用。"
            Logger.e("NetworkServer", msg)
            _lastError.value = msg
            _state.value = StreamState.Error
        }
    }

    private suspend fun runBluetoothServer() {
        while (currentCoroutineContext().isActive) {
            try {
                val localDevice = LocalDevice.getLocalDevice()
                localDevice.discoverable = DiscoveryAgent.GIAC
                Logger.i("NetworkServer", "本地蓝牙: ${localDevice.friendlyName} ${localDevice.bluetoothAddress}")
                
                val uuid = UUID("0000110100001000800000805F9B34FB", false)
                val url = "btspp://localhost:$uuid;name=MicYouServer"
                
                btNotifier = Connector.open(url) as StreamConnectionNotifier
                Logger.i("NetworkServer", "蓝牙服务已启动: $url")
                
                while (currentCoroutineContext().isActive) {
                    val connection = btNotifier?.acceptAndOpen() ?: break
                    activeBtConnection = connection
                    Logger.i("NetworkServer", "接受蓝牙连接")
                    
                    val input = connection.openInputStream().toByteReadChannel()
                    val output = connection.openOutputStream().toByteWriteChannel()
                    
                    handleConnection(
                        input = input,
                        output = output,
                        closeAction = {
                            connection.close()
                            activeBtConnection = null
                        }
                    )
                }
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Logger.e("NetworkServer", "蓝牙服务器错误", e)
                     if (_state.value != StreamState.Connecting) {
                         _state.value = StreamState.Error
                         _lastError.value = "蓝牙错误: ${e.message}"
                         delay(5000) // 重试延迟
                         _state.value = StreamState.Connecting
                     }
                }
            }
        }
    }

    private suspend fun handleConnection(
        input: ByteReadChannel,
        output: ByteWriteChannel,
        closeAction: suspend () -> Unit
    ) {
        _state.value = StreamState.Streaming
        _lastError.value = null

        val handler = ConnectionHandler(
            input = input,
            output = output,
            onAudioPacketReceived = onAudioPacketReceived,
            onMuteStateChanged = onMuteStateChanged,
            onError = { error ->
                _lastError.value = error
            }
        )
        activeHandler = handler
        
        try {
            handler.run()
        } finally {
            activeHandler = null
            closeAction()
            Logger.i("NetworkServer", "连接已关闭")
            _state.value = StreamState.Connecting
        }
    }

    private fun cleanup() {
        try {
            activeSocket?.close()
            activeSocket = null
            serverSocket?.close()
            serverSocket = null
            
            btNotifier?.close()
            btNotifier = null
            activeBtConnection?.close()
            activeBtConnection = null
            
            selectorManager?.close()
            selectorManager = null
        } catch (e: Exception) {
            Logger.e("NetworkServer", "清理资源出错", e)
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun java.io.OutputStream.toByteWriteChannel(context: kotlin.coroutines.CoroutineContext = Dispatchers.IO): ByteWriteChannel {
    val channel = ByteChannel()
    GlobalScope.launch(context) {
        val buffer = ByteArray(4096)
        try {
            while (!channel.isClosedForRead) {
                val count = channel.readAvailable(buffer)
                if (count == -1) break
                write(buffer, 0, count)
                flush()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    return channel
}
