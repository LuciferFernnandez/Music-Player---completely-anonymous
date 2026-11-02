package com.ludaze.music.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.sqrt

/** Pass-through AudioProcessor that measures RMS volume of the PCM stream. */
@UnstableApi
class AmplitudeProcessor : BaseAudioProcessor() {
    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) throw UnhandledAudioFormatException(inputAudioFormat)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val view = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        var sum = 0.0; var n = 0
        while (view.remaining() >= 2) {
            val s = view.short / 32768.0
            sum += s * s; n++
        }
        if (n > 0) AudioBus.push(min(1f, (sqrt(sum / n) * 3.0).toFloat()))
        val out = replaceOutputBuffer(size)
        out.put(inputBuffer)
        out.flip()
    }

    override fun onFlush() { AudioBus.push(0f) }
    override fun onReset() { AudioBus.push(0f) }
}
