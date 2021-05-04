package com.breakfastquay.rubberbandexample

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.breakfastquay.rubberbandexample.databinding.ActivityMainBinding
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioSink
import com.google.android.exoplayer2.audio.DefaultAudioSink
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var player: SimpleExoPlayer? = null

    private var playbackPosition = 0L
    private var currentWindow = 0
    private var playWhenReady = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
    }

    private fun playSample(context: Context) {
        if (player == null) {
            val trackSelector = DefaultTrackSelector(context)
            val processor = RubberBandAudioProcessor()
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(32 * 1024, 64 * 1024, 1024, 1024)
                .build()
            processor.setPitch(1.4f)
            val rendererFactory = object : DefaultRenderersFactory(context) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean,
                    enableOffload: Boolean
                ): AudioSink {
                    return DefaultAudioSink(null, arrayOf(processor))
                }
            }

            val mainPcv = findViewById<PlayerControlView>(R.id.main_pcv)
            player = ExoPlayerFactory.newSimpleInstance(
                context,
                rendererFactory,
                trackSelector,
                loadControl
            )
            mainPcv.player = player!!
            mainPcv.showTimeoutMs = 0
            val factory =
                DefaultDataSourceFactory(context, Util.getUserAgent(context, "RubberBandFlutter"))
            val audioSource = ProgressiveMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri("file:///android_asset/step2.mp3"))
            
            player!!.setMediaSource(audioSource)
            player!!.prepare()
            player!!.seekTo(currentWindow, playbackPosition)
            player!!.playWhenReady = playWhenReady
        }
    }

    private fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            currentWindow = it.currentWindowIndex
            playWhenReady = it.playWhenReady
            it.playWhenReady = false
            it.release()
            player = null
        }
    }

    override fun onResume() {
        super.onResume()
        playSample(this)
    }

    override fun onRestart() {
        super.onRestart()
        playSample(this)
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }
}