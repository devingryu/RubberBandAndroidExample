package com.breakfastquay.rubberbandexample

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.audio.AudioProcessor
import com.google.android.exoplayer2.audio.AudioProcessor.UnhandledAudioFormatException
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Util
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.abs


class RubberBandAudioProcessor : AudioProcessor {

    /** Indicates that the output sample rate should be the same as the input.  */
    val SAMPLE_RATE_NO_CHANGE = -1

    /** The threshold below which the difference between two pitch/speed factors is negligible.  */
    private val CLOSE_THRESHOLD = 0.0001f

    /**
     * The minimum number of output bytes required for duration scaling to be calculated using the
     * input and output byte counts, rather than using the current playback speed.
     */
    private val MIN_BYTES_FOR_DURATION_SCALING_CALCULATION = 1024

    private var pendingOutputSampleRate = 0
    private var speed = 0f
    private var pitch = 0f

    private var pendingInputAudioFormat: AudioProcessor.AudioFormat
    private var pendingOutputAudioFormat: AudioProcessor.AudioFormat
    private var inputAudioFormat: AudioProcessor.AudioFormat
    private var outputAudioFormat: AudioProcessor.AudioFormat

    private var pendingSonicRecreation = false
    private var rb: RubberBandStretcher? = null
    private var buffer: ByteBuffer
    private var shortBuffer: ShortBuffer
    private var outputBuffer: ByteBuffer
    private var inputBytes: Long = 0
    private var outputBytes: Long = 0
    private var inputEnded = false

    /** Creates a new Sonic audio processor.  */
    init {
        speed = 1f
        pitch = 1f
        pendingInputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        buffer = AudioProcessor.EMPTY_BUFFER
        shortBuffer = buffer.asShortBuffer()
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        pendingOutputSampleRate = SAMPLE_RATE_NO_CHANGE
    }

    /**
     * Sets the target playback speed. This method may only be called after draining data through the
     * processor. The value returned by [.isActive] may change, and the processor must be
     * [flushed][.flush] before queueing more data.
     *
     * @param speed The target factor by which playback should be sped up.
     */
    fun setSpeed(speed: Float) {
        if (this.speed != speed) {
            this.speed = speed
            pendingSonicRecreation = true
        }
    }

    /**
     * Sets the target playback pitch. This method may only be called after draining data through the
     * processor. The value returned by [.isActive] may change, and the processor must be
     * [flushed][.flush] before queueing more data.
     *
     * @param pitch The target pitch.
     */
    fun setPitch(pitch: Float) {
        if (this.pitch != pitch) {
            this.pitch = pitch
            pendingSonicRecreation = true
        }
    }

    /**
     * Sets the sample rate for output audio, in Hertz. Pass [.SAMPLE_RATE_NO_CHANGE] to output
     * audio at the same sample rate as the input. After calling this method, call [ ][.configure] to configure the processor with the new sample rate.
     *
     * @param sampleRateHz The sample rate for output audio, in Hertz.
     * @see .configure
     */
    fun setOutputSampleRateHz(sampleRateHz: Int) {
        pendingOutputSampleRate = sampleRateHz
    }

    /**
     * Returns the media duration corresponding to the specified playout duration, taking speed
     * adjustment into account.
     *
     *
     * The scaling performed by this method will use the actual playback speed achieved by the
     * audio processor, on average, since it was last flushed. This may differ very slightly from the
     * target playback speed.
     *
     * @param playoutDuration The playout duration to scale.
     * @return The corresponding media duration, in the same units as `duration`.
     */
    fun getMediaDuration(playoutDuration: Long): Long {
        return if (outputBytes >= MIN_BYTES_FOR_DURATION_SCALING_CALCULATION) {
            val processedInputBytes = inputBytes - Assertions.checkNotNull(rb).samplesRequired
            if (outputAudioFormat.sampleRate == inputAudioFormat.sampleRate) Util.scaleLargeTimestamp(
                playoutDuration,
                processedInputBytes,
                outputBytes
            ) else Util.scaleLargeTimestamp(
                playoutDuration,
                processedInputBytes * outputAudioFormat.sampleRate,
                outputBytes * inputAudioFormat.sampleRate
            )
        } else {
            (speed.toDouble() * playoutDuration).toLong()
        }
    }

    @Throws(UnhandledAudioFormatException::class)
    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        val outputSampleRateHz =
            if (pendingOutputSampleRate == SAMPLE_RATE_NO_CHANGE) inputAudioFormat.sampleRate else pendingOutputSampleRate
        pendingInputAudioFormat = inputAudioFormat
        pendingOutputAudioFormat = AudioProcessor.AudioFormat(
            outputSampleRateHz,
            inputAudioFormat.channelCount,
            C.ENCODING_PCM_16BIT
        )
        pendingSonicRecreation = true
        return pendingOutputAudioFormat
    }

    override fun isActive(): Boolean {
        return (pendingOutputAudioFormat.sampleRate != Format.NO_VALUE
                && (abs(speed - 1f) >= CLOSE_THRESHOLD || abs(pitch - 1f) >= CLOSE_THRESHOLD || pendingOutputAudioFormat.sampleRate != pendingInputAudioFormat.sampleRate))
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            return
        }
        val rb = Assertions.checkNotNull(rb)
        val shortBuffer = inputBuffer.asShortBuffer()
        val inputSize = inputBuffer.remaining()
        inputBytes += inputSize.toLong()
        rb.process(shortBuffer, inputAudioFormat.channelCount, false)
        inputBuffer.position(inputBuffer.position() + inputSize)
    }

    override fun queueEndOfStream() {
        if (rb != null) {
            rb!!.queueEndOfStream()
        }
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {

        val rb = rb
        val channelCount = inputAudioFormat.channelCount
        if (rb != null) {
            val outputSize = rb.available()
            if (outputSize > 0) {
                if (buffer.capacity() < outputSize * channelCount * 2) {
                    buffer = ByteBuffer.allocateDirect(outputSize * channelCount * 2)
                        .order(ByteOrder.nativeOrder())
                    shortBuffer = buffer.asShortBuffer()
                } else {
                    buffer.clear()
                    shortBuffer.clear()
                }
                val retrieved = rb.retrieve(shortBuffer, channelCount)
                outputBytes += retrieved.toLong() * channelCount * 2
                buffer.limit(retrieved * channelCount * 2)
                outputBuffer = buffer
            }
        }
        val outputBuffer = outputBuffer
        this.outputBuffer = AudioProcessor.EMPTY_BUFFER
        return outputBuffer
    }

    override fun isEnded(): Boolean {
        return inputEnded && (rb == null || rb!!.samplesRequired == 0)
    }

    override fun flush() {
        if (isActive) {
            inputAudioFormat = pendingInputAudioFormat
            outputAudioFormat = pendingOutputAudioFormat
            if (pendingSonicRecreation) {
                rb = RubberBandStretcher(
                    inputAudioFormat.sampleRate,
                    inputAudioFormat.channelCount,
                    RubberBandStretcher.OptionProcessRealTime +
                            RubberBandStretcher.OptionPitchHighQuality,
                    speed.toDouble(),
                    pitch.toDouble()
                )
            } else if (rb != null) {
                rb!!.reset()
            }
        }
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputBytes = 0
        outputBytes = 0
        inputEnded = false
    }

    override fun reset() {
        speed = 1f
        pitch = 1f
        pendingInputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        buffer = AudioProcessor.EMPTY_BUFFER
        shortBuffer = buffer.asShortBuffer()
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        pendingOutputSampleRate = SAMPLE_RATE_NO_CHANGE
        pendingSonicRecreation = false
        rb = null
        inputBytes = 0
        outputBytes = 0
        inputEnded = false
    }
}