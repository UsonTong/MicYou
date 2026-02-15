package com.lanrhyme.micyou.audio

/**
 * 放大器效果器。
 * 简单的线性音量放大。
 */
class AmplifierEffect : AudioEffect {
    /** 放大倍数 (1.0 = 原始音量) */
    var amplification: Float = 1.0f

    override fun process(input: ShortArray, channelCount: Int): ShortArray {
        if (amplification == 1.0f) return input

        for (i in input.indices) {
            val sample = input[i].toInt()
            val amplified = (sample * amplification).toInt()
            input[i] = amplified.coerceIn(-32768, 32767).toShort()
        }
        return input
    }

    override fun reset() {
    }

    override fun release() {
    }
}
