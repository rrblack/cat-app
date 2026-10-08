package com.rrblack.meowtranslator.core.dsp

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sqrt

/** Per-frame acoustic measurements for a whole recording (10 ms hop). */
class FrameTrack(
    val sampleRate: Int,
    val hopSize: Int,
    val frameSize: Int,
    /** Frame energy in dBFS. */
    val rmsDb: DoubleArray,
    /** Tracked F0 in Hz, 0 where the frame is unvoiced. */
    val f0: DoubleArray,
    /** YIN aperiodicity of the tracked pitch (or of the best candidate when unvoiced); 0 = periodic, 1 = noise. */
    val aperiodicity: DoubleArray,
    /** Whether the pitch tracker considers the frame voiced. */
    val voiced: BooleanArray,
    /** Spectral centroid in Hz (of the pre-emphasised spectrum). */
    val centroid: DoubleArray,
    /** Spectral flatness (Wiener entropy) in 0..1; high for hiss-like noise. */
    val flatness: DoubleArray,
    /** Frequency below which 85% of the spectral energy lies. */
    val rolloff: DoubleArray,
    /** Fraction of energy above 2 kHz. */
    val highBandRatio: DoubleArray,
    /** Fraction of energy below 500 Hz (raw spectrum). */
    val lowBandRatio: DoubleArray,
    /** MFCCs (13 per frame). */
    val mfcc: Array<DoubleArray>,
) {
    val size: Int get() = rmsDb.size
    fun timeOf(frame: Int): Double = (frame * hopSize + frameSize / 2.0) / sampleRate
}

class FrameAnalyzer(
    private val sampleRate: Int,
    val frameSize: Int = (sampleRate * 0.05).toInt(),
    val hopSize: Int = sampleRate / 100,
    minF0: Double = 70.0,
    maxF0: Double = 1800.0,
) {
    private val fftSize = Fft.nextPowerOfTwo(frameSize)
    private val window = Fft.hann(frameSize)
    private val yin = Yin(sampleRate, minF0, maxF0)
    private val mfcc = Mfcc(sampleRate, fftSize)
    private val binHz = sampleRate.toDouble() / fftSize
    private val flatLo = (150 / binHz).toInt()
    private val flatHi = (minOf(3800.0, sampleRate / 2.0 - 100) / binHz).toInt()

    fun analyze(samples: FloatArray): FrameTrack {
        val n = if (samples.size < frameSize) 0 else 1 + (samples.size - frameSize) / hopSize
        val rmsDb = DoubleArray(n)
        val pitchCandidates = ArrayList<List<Yin.Candidate>>(n)
        val centroid = DoubleArray(n)
        val flatness = DoubleArray(n)
        val rolloff = DoubleArray(n)
        val high = DoubleArray(n)
        val low = DoubleArray(n)
        val ceps = Array(n) { DoubleArray(13) }
        val frame = DoubleArray(frameSize)
        val emphasized = DoubleArray(frameSize)
        // Pitch is estimated on a low-passed copy: strong upper harmonics otherwise pull YIN towards
        // subharmonics because at 8 kHz their periods don't fall on whole-sample lags (YIN paper, §IV.B).
        val forPitch = Filters.lowPass(samples, PITCH_LOWPASS_HZ / sampleRate)

        for (i in 0 until n) {
            val off = i * hopSize
            var energy = 0.0
            var prev = if (off > 0) samples[off - 1].toDouble() else 0.0
            for (j in 0 until frameSize) {
                val s = samples[off + j].toDouble()
                energy += s * s
                frame[j] = s * window[j]
                // Pre-emphasis flattens the spectral tilt so formant structure is visible to the MFCCs.
                emphasized[j] = (s - 0.97 * prev) * window[j]
                prev = s
            }
            rmsDb[i] = 10 * log10(energy / frameSize + 1e-12)

            pitchCandidates += yin.candidates(forPitch, off, frameSize)

            // Spectral shape is measured on the pre-emphasised spectrum (as for the MFCCs), except the low-band
            // share, which needs the raw spectrum: pre-emphasis removes exactly the energy purrs and growls carry.
            val p = Fft.powerSpectrum(emphasized, fftSize)
            val raw = Fft.powerSpectrum(frame, fftSize)
            var total = 0.0
            var weighted = 0.0
            var hi = 0.0
            var rawTotal = 0.0
            var rawLow = 0.0
            for (k in 1 until p.size) {
                val hz = k * binHz
                total += p[k]
                weighted += p[k] * hz
                if (hz >= 2000) hi += p[k]
                rawTotal += raw[k]
                if (hz < 500) rawLow += raw[k]
            }
            centroid[i] = if (total > 0) weighted / total else 0.0
            high[i] = if (total > 0) hi / total else 0.0
            low[i] = if (rawTotal > 0) rawLow / rawTotal else 0.0
            var cum = 0.0
            var roll = 0
            for (k in 1 until p.size) {
                cum += p[k]
                if (cum >= 0.85 * total) { roll = k; break }
            }
            rolloff[i] = roll * binHz

            var logSum = 0.0
            var linSum = 0.0
            for (k in flatLo..flatHi) {
                logSum += ln(p[k] + 1e-12)
                linSum += p[k] + 1e-12
            }
            val count = (flatHi - flatLo + 1).toDouble()
            flatness[i] = (exp(logSum / count) / (linSum / count)).coerceIn(0.0, 1.0)

            ceps[i] = mfcc.compute(p)
        }
        val pitch = PitchTracker.track(pitchCandidates)
        return FrameTrack(sampleRate, hopSize, frameSize, rmsDb, pitch.f0, pitch.aperiodicity, pitch.voiced, centroid, flatness, rolloff, high, low, ceps)
    }

    companion object {
        private const val PITCH_LOWPASS_HZ = 1900.0

        fun rms(x: FloatArray, from: Int = 0, to: Int = x.size): Double {
            var s = 0.0
            for (i in from until to) s += x[i] * x[i]
            return sqrt(s / (to - from).coerceAtLeast(1))
        }
    }
}
