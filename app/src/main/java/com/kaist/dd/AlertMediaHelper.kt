package com.kaist.dd

import android.content.Context
import android.media.MediaPlayer

class AlertMediaHelper(val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    private var mediaPlayerList: List<MediaPlayer> =
        listOf(
            MediaPlayer.create(context, R.raw.alert_sound_1),
            MediaPlayer.create(context, R.raw.alert_sound_2),
            MediaPlayer.create(context, R.raw.alert_sound_3)
        )

    fun playMedia(type: Int) {
        stopMedia()
        val player = mediaPlayerList[type]
        player.isLooping = false
        player.seekTo(0)
        player.start()
    }

    fun stopMedia() {
        for(player in mediaPlayerList) {
            if(player.isPlaying) {
                player.pause()
            }
        }
    }
}