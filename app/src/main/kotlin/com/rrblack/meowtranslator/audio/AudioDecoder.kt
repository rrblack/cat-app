package com.rrblack.meowtranslator.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.rrblack.meowtranslator.core.audio.PcmAudio
import com.rrblack.meowtranslator.core.audio.WavIO
import java.nio.ByteOrder

/** Decodes an audio file picked by the user (WAV, M4A/AAC, MP3, OGG, … — whatever the device supports). */
object AudioDecoder {
    private const val MAX_SECONDS = 60

    fun decode(context: Context, uri: Uri): PcmAudio {
        val resolver = context.contentResolver
        val header = ByteArray(12)
        val read = resolver.openInputStream(uri)?.use { input ->
            var total = 0
            while (total < header.size) {
                val r = input.read(header, total, header.size - total)
                if (r < 0) break
                total += r
            }
            total
        } ?: throw IllegalArgumentException("Can't open file")
        if (read >= 12 && String(header, 0, 4, Charsets.US_ASCII) == "RIFF" && String(header, 8, 4, Charsets.US_ASCII) == "WAVE") {
            val wav = resolver.openInputStream(uri)!!.use { WavIO.read(it) }
            val max = wav.sampleRate * MAX_SECONDS
            return if (wav.samples.size > max) PcmAudio(wav.samples.copyOf(max), wav.sampleRate) else wav
        }
        return decodeWithMediaCodec(context, uri)
    }

    private fun decodeWithMediaCodec(context: Context, uri: Uri): PcmAudio {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: throw IllegalArgumentException("No audio track in this file")
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var pcmFloat = false
        var out = FloatArray(1 shl 16)
        var count = 0
        fun append(v: Float) {
            if (count == out.size) out = out.copyOf(out.size * 2)
            out[count++] = v
        }
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buf = codec.getInputBuffer(inIndex)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val f = codec.outputFormat
                        sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        pcmFloat = f.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                    }
                    outIndex >= 0 -> {
                        val buf = codec.getOutputBuffer(outIndex)!!.order(ByteOrder.LITTLE_ENDIAN)
                        buf.position(info.offset)
                        buf.limit(info.offset + info.size)
                        if (pcmFloat) {
                            val fb = buf.asFloatBuffer()
                            while (fb.remaining() >= channels) {
                                var s = 0f
                                repeat(channels) { s += fb.get() }
                                append(s / channels)
                            }
                        } else {
                            val sb = buf.asShortBuffer()
                            while (sb.remaining() >= channels) {
                                var s = 0f
                                repeat(channels) { s += sb.get() / 32768f }
                                append(s / channels)
                            }
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                        if (count >= sampleRate * MAX_SECONDS) outputDone = true
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }
        return PcmAudio(out.copyOf(count), sampleRate)
    }
}
