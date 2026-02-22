package com.lanrhyme.micyou.platform

import com.lanrhyme.micyou.Logger
import javax.sound.sampled.AudioSystem

/**
 * BlackHole 虚拟音频驱动管理器 (macOS 平台)
 * 
 * BlackHole 是一个 macOS 虚拟音频循环驱动，允许应用程序之间传递音频
 * 官网: https://existential.audio/blackhole/
 */
object BlackHoleManager {
    
    // BlackHole 设备名称正则匹配 (BlackHole 2ch, 16ch, 64ch 等)
    private val BLACKHOLE_PATTERN = Regex("BlackHole\\s*\\d*ch", RegexOption.IGNORE_CASE)
    
    /**
     * 检测 BlackHole 是否已安装
     * 
     * @return true 如果找到 BlackHole 设备
     */
    fun isInstalled(): Boolean {
        if (!PlatformInfo.isMacOS) {
            return false
        }
        
        return try {
            val mixers = AudioSystem.getMixerInfo()
            mixers.any { mixerInfo ->
                BLACKHOLE_PATTERN.matches(mixerInfo.name)
            }
        } catch (e: Exception) {
            Logger.e("BlackHoleManager", "检测 BlackHole 设备时出错", e)
            false
        }
    }
    
    /**
     * 获取已安装的 BlackHole 设备名称
     * 
     * @return BlackHole 设备名称，如果未安装返回 null
     */
    fun getDeviceName(): String? {
        if (!PlatformInfo.isMacOS) {
            return null
        }
        
        return try {
            val mixers = AudioSystem.getMixerInfo()
            mixers.find { mixerInfo ->
                BLACKHOLE_PATTERN.matches(mixerInfo.name)
            }?.name
        } catch (e: Exception) {
            Logger.e("BlackHoleManager", "获取 BlackHole 设备名称时出错", e)
            null
        }
    }
    
    /**
     * 获取 BlackHole 设备的所有可用混音器信息
     * 
     * @return BlackHole 混音器信息列表
     */
    fun getAvailableDevices(): List<MixerInfo> {
        if (!PlatformInfo.isMacOS) {
            return emptyList()
        }
        
        return try {
            val mixers = AudioSystem.getMixerInfo()
            mixers.filter { mixerInfo ->
                BLACKHOLE_PATTERN.matches(mixerInfo.name)
            }.map { info ->
                MixerInfo(
                    name = info.name,
                    description = info.description,
                    vendor = info.vendor,
                    version = info.version
                )
            }
        } catch (e: Exception) {
            Logger.e("BlackHoleManager", "获取 BlackHole 设备列表时出错", e)
            emptyList()
        }
    }
    
    /**
     * 获取 BlackHole 输入设备 (用于麦克风输入)
     * BlackHole 通常以 "BlackHole 2ch" 或 "BlackHole 16ch" 形式出现
     * 
     * @return BlackHole 输入混音器，如果未找到返回 null
     */
    fun findInputMixer(): javax.sound.sampled.Mixer.Info? {
        if (!PlatformInfo.isMacOS) {
            return null
        }
        
        return try {
            val mixers = AudioSystem.getMixerInfo()
            mixers.find { mixerInfo ->
                BLACKHOLE_PATTERN.matches(mixerInfo.name)
            }
        } catch (e: Exception) {
            Logger.e("BlackHoleManager", "查找 BlackHole 输入混音器时出错", e)
            null
        }
    }
    
    /**
     * 检查是否支持自动安装 (macOS 不支持自动安装)
     * 
     * @return false - macOS 需要手动安装
     */
    fun isAutoInstallSupported(): Boolean {
        return false
    }
    
    /**
     * 获取安装说明
     * 
     * @return 安装说明文本
     */
    fun getInstallInstructions(): String {
        return """
            |macOS 平台需要手动安装 BlackHole 虚拟音频驱动:
            |
            |1. 下载 BlackHole: https://existential.audio/blackhole/
            |2. 运行安装程序
            |3. 在"系统偏好设置" > "安全性与隐私" > "通用"中允许加载扩展
            |4. 重启电脑
            |
            |注意: 如果在音频设备中看不到 BlackHole，请确保在 Audio MIDI Setup 中启用它
        """.trimMargin()
    }
    
    data class MixerInfo(
        val name: String,
        val description: String,
        val vendor: String,
        val version: String
    )
}
