package ru.lisichkaai.app

import android.content.Context
import android.media.MediaPlayer
import java.io.File

object VoicePlayer {
    private var player: MediaPlayer? = null
    private var audioFile: File? = null

    fun play(
        context: Context,
        audio: ByteArray,
        onDuration: (Int) -> Unit = {},
        onPlayingChanged: (Boolean) -> Unit
    ) {
        stop()
        val file = File(context.cacheDir, "lisichka-voice-${System.currentTimeMillis()}.mp3")
        file.writeBytes(audio)
        val media = MediaPlayer()
        player = media
        audioFile = file
        media.setOnPreparedListener {
            onDuration(it.duration.coerceAtLeast(0))
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
            throw error
        }
    }

    fun currentPositionMs(): Int = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)

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
