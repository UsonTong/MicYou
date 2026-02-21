package com.lanrhyme.micyou

import com.lanrhyme.micyou.util.JvmSettings

actual object SettingsFactory {
    actual fun getSettings(): Settings = JvmSettings
}
