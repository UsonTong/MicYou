package com.lanrhyme.micyou.platform

import com.lanrhyme.micyou.Logger

object FirewallManager {
    fun isPortAllowed(port: Int): Boolean {
        if (!PlatformInfo.isWindows) {
            return true
        }
        
        return try {
            val process = ProcessBuilder(
                "powershell.exe",
                "-Command",
                "netsh advfirewall firewall show rule name=all | Select-String 'MicYou-$port'"
            ).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            output.contains("MicYou-$port")
        } catch (e: Exception) {
            Logger.e("FirewallManager", "检查防火墙规则失败", e)
            false
        }
    }
    
    fun addFirewallRule(port: Int): Boolean {
        if (!PlatformInfo.isWindows) {
            return true
        }
        
        if (isPortAllowed(port)) {
            Logger.d("FirewallManager", "防火墙规则已存在: MicYou-$port")
            return true
        }
        
        return try {
            val command = """
                New-NetFirewallRule -DisplayName "MicYou-$port" -Direction Inbound -LocalPort $port -Protocol TCP -Action Allow
            """.trimIndent()
            
            val process = ProcessBuilder(
                "powershell.exe",
                "-Command",
                command
            ).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Logger.i("FirewallManager", "防火墙规则添加成功: MicYou-$port")
                true
            } else {
                Logger.e("FirewallManager", "防火墙规则添加失败: $output")
                tryNetshFallback(port)
            }
        } catch (e: Exception) {
            Logger.e("FirewallManager", "添加防火墙规则时出错", e)
            tryNetshFallback(port)
        }
    }
    
    private fun tryNetshFallback(port: Int): Boolean {
        return try {
            val process = ProcessBuilder(
                "netsh", "advfirewall", "firewall", "add", "rule",
                "name=MicYou-$port",
                "dir=in",
                "action=allow",
                "protocol=TCP",
                "localport=$port"
            ).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Logger.i("FirewallManager", "防火墙规则添加成功: MicYou-$port")
                true
            } else {
                Logger.e("FirewallManager", "防火墙规则添加失败: $output")
                false
            }
        } catch (e: Exception) {
            Logger.e("FirewallManager", "添加防火墙规则时出错", e)
            false
        }
    }
    
    fun removeFirewallRule(port: Int): Boolean {
        if (!PlatformInfo.isWindows) {
            return true
        }
        
        return try {
            val process = ProcessBuilder(
                "powershell.exe",
                "-Command",
                "Remove-NetFirewallRule -DisplayName 'MicYou-$port'"
            ).redirectErrorStream(true).start()
            
            process.waitFor()
            Logger.i("FirewallManager", "防火墙规则已移除: MicYou-$port")
            true
        } catch (e: Exception) {
            Logger.e("FirewallManager", "移除防火墙规则时出错", e)
            false
        }
    }
}
