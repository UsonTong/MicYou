package com.lanrhyme.micyou.platform

object PlatformInfo {
    enum class OS {
        WINDOWS, LINUX, MACOS, OTHER
    }
    
    val currentOS: OS by lazy {
        val osName = System.getProperty("os.name", "").lowercase()
        when {
            osName.contains("win") -> OS.WINDOWS
            osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> OS.LINUX
            osName.contains("mac") -> OS.MACOS
            else -> OS.OTHER
        }
    }
    
    val isWindows: Boolean get() = currentOS == OS.WINDOWS
    val isLinux: Boolean get() = currentOS == OS.LINUX
    val isMacOS: Boolean get() = currentOS == OS.MACOS
    
    val osName: String get() = System.getProperty("os.name", "Unknown")
    val osVersion: String get() = System.getProperty("os.version", "Unknown")
    val osArch: String get() = System.getProperty("os.arch", "Unknown")
}
