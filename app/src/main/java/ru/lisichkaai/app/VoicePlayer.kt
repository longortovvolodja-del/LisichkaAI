package ru.lisichkaai.app

import android.content.Context
import android.media.MediaPlayer
import java.io.File

object VoicePlayer {
    private var player: MediaPlayer? = null
    private var audioFile: File? = null

    fun play(context: Context, audio: ByteArray, onPlayingChanged: (Boolean) -> Unit) {
        stop()
        val file = File(context.cacheDir, "lisichka-voice-${System.currentTimeMillis()}.mp3")
        file.writeBytes(audio)
        val media = MediaPlayer()
        player = media
        audioFile = file
        media.setOnPreparedListener {
            it.start()
            onPlayingChanged(true)
        }
        media.setOnCompletionListener {
            release()
            onPlayingChanged(false)
        }
        media.setOnErrorListener { _, _, _ ->
            release()
            onPlayingChanged(false)
            true
        }
        try {
            media.setDataSource(file.absolutePath)
            media.prepareAsync()
        } catch (error: Exception) {
            release()
            onPlayingChanged(false)
        }
    }

    fun stop() {
        runCatching { player?.stop() }
        release()
    }

    private fun release() {
        runCatching { player?.reset() }
        runCatching { player?.release() }
        player = null
        runCatching { audioFile?.delete() }
        audioFile = null
    }
}
