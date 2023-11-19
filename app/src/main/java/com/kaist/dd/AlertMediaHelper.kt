package com.kaist.dd

import android.content.Context
import android.media.MediaPlayer

class AlertMediaHelper(val context: Context) {
    private lateinit var mediaPlayer: MediaPlayer

    fun playMedia(type: Int) {
        val mediaType = if (type == 1) R.raw.alert_sound_1 else if (type == 2) R.raw.alert_sound_2 else R.raw.alert_sound_3
        mediaPlayer = MediaPlayer.create(context, mediaType)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
    }

    fun stopMedia() {
        mediaPlayer.stop()
        mediaPlayer.release()
    }
}