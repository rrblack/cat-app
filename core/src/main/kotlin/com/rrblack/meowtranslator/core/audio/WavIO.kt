package com.rrblack.meowtranslator.core.audio

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Mono audio as floats in [-1, 1]. */
class PcmAudio(val samples: FloatArray, val sampleRate: Int) {
    val durationSec: Double get() = samples.size.toDouble() / sampleRate
}

/** Minimal RIFF/WAVE reader (PCM 8/16/24/32-bit and 32-bit float, any channel count) and 16-bit writer. */
object WavIO {
    fun read(input: InputStream): PcmAudio {
        val data = DataInputStream(input.buffered())
        val header = ByteArray(12)
        data.readFully(header)
        require(String(header, 0, 4, Charsets.US_ASCII) == "RIFF" && String(header, 8, 4, Charsets.US_ASCII) == "WAVE") {
            "Not a WAV file"
        }
        var format = 0
        var channels = 0
        var sampleRate = 0
        var bits = 0
        while (true) {
            val chunkHeader = ByteArray(8)
            try {
                data.readFully(chunkHeader)
            } catch (e: EOFException) {
                throw IllegalArgumentException("WAV file has no data chunk")
            }
            val id = String(chunkHeader, 0, 4, Charsets.US_ASCII)
            val size = ByteBuffer.wrap(chunkHeader, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xffffffffL
            when (id) {
                "fmt " -> {
                    val fmt = ByteArray(size.toInt())
                    data.readFully(fmt)
                    val b = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN)
                    format = b.short.toInt() and 0xffff
                    channels = b.short.toInt()
                    sampleRate = b.int
                    b.int // byte rate
                    b.short // block align
                    bits = b.short.toInt()
                    if (format == 0xFFFE && fmt.size >= 26) format = ByteBuffer.wrap(fmt, 24, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                    if (size % 2 == 1L) data.skipBytes(1)
                }
                "data" -> {
                    require(channels > 0 && sampleRate > 0) { "WAV data chunk before fmt chunk" }
                    val bytes = readUpTo(data, size)
                    return PcmAudio(decode(bytes, format, channels, bits), sampleRate)
                }
                else -> {
                    var remaining = size + (size % 2)
                    while (remaining > 0) {
                        val skipped = data.skip(remaining)
                        if (skipped <= 0) throw IllegalArgumentException("Truncated WAV chunk '$id'")
                        remaining -= skipped
                    }
                }
            }
        }
    }

    private fun readUpTo(input: InputStream, size: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var remaining = size
        while (remaining > 0) {
            val r = input.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
            if (r < 0) break // Tolerate truncated files and streaming writers that left the size field wrong.
            out.write(buf, 0, r)
            remaining -= r
        }
        return out.toByteArray()
    }

    private fun decode(bytes: ByteArray, format: Int, channels: Int, bits: Int): FloatArray {
        val bytesPerSample = bits / 8
        val frames = bytes.size / (bytesPerSample * channels)
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(frames)
        for (i in 0 until frames) {
            var sum = 0f
            for (c in 0 until channels) {
                val pos = (i * channels + c) * bytesPerSample
                sum += when {
                    format == 3 && bits == 32 -> b.getFloat(pos)
                    bits == 8 -> ((bytes[pos].toInt() and 0xff) - 128) / 128f
                    bits == 16 -> b.getShort(pos) / 32768f
                    bits == 24 -> {
                        val v = (bytes[pos].toInt() and 0xff) or ((bytes[pos + 1].toInt() and 0xff) shl 8) or (bytes[pos + 2].toInt() shl 16)
                        v / 8388608f
                    }
                    bits == 32 -> b.getInt(pos) / 2147483648f
                    else -> throw IllegalArgumentException("Unsupported WAV encoding: format=$format bits=$bits")
                }
            }
            out[i] = sum / channels
        }
        return out
    }

    fun write(audio: PcmAudio, output: OutputStream) {
        val dataSize = audio.samples.size * 2
        val b = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray(Charsets.US_ASCII)).putInt(36 + dataSize).put("WAVE".toByteArray(Charsets.US_ASCII))
        b.put("fmt ".toByteArray(Charsets.US_ASCII)).putInt(16).putShort(1).putShort(1)
            .putInt(audio.sampleRate).putInt(audio.sampleRate * 2).putShort(2).putShort(16)
        b.put("data".toByteArray(Charsets.US_ASCII)).putInt(dataSize)
        for (s in audio.samples) b.putShort((s.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        output.write(b.array())
        output.flush()
    }
}
