package de.tadris.flang.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import de.tadris.flang.R

class AudioController(context: Context) {

    companion object {

        var SOUND_BERSERK = 0
        var SOUND_ENERGY = 0
        var SOUND_NOTIFY_GENERIC = 0
        var SOUND_NOTIFY_SOCIAL = 0
        var SOUND_PING = 0
        var SOUND_SELECT = 0
        var SOUND_FAILURE = 0
        var SOUND_LOW_TIME = 0
        var SOUND_MOVE = 0
        var SOUND_MOVE_CAPTURE = 0
        var SOUND_MOVE_CONFIRMATION = 0

        private var instance: AudioController? = null

        fun getInstance(context: Context): AudioController {
            if(instance == null){
                instance = AudioController(context)
            }
            return instance!!
        }
    }

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(attrs)
        .build()

    val soundVolume = 0.4f
    var soundEnabled = true

    init {
        SOUND_BERSERK = soundPool.load(context, R.raw.berserk, 1)
        SOUND_ENERGY = soundPool.load(context, R.raw.energy, 1)
        SOUND_NOTIFY_GENERIC = soundPool.load(context, R.raw.notify, 1)
        SOUND_NOTIFY_SOCIAL = soundPool.load(context, R.raw.social_notify, 1)
        SOUND_PING = soundPool.load(context, R.raw.ping, 1)
        SOUND_SELECT = soundPool.load(context, R.raw.select, 1)
        SOUND_FAILURE = soundPool.load(context, R.raw.failure, 1)
        SOUND_LOW_TIME = soundPool.load(context, R.raw.low_time, 1)
        SOUND_MOVE = soundPool.load(context, R.raw.move, 1)
        SOUND_MOVE_CAPTURE = soundPool.load(context, R.raw.capture, 1)
        SOUND_MOVE_CONFIRMATION = soundPool.load(context, R.raw.confirmation, 1)
    }

    fun playSound(soundRes: Int) {
        if (!soundEnabled) {
            return
        }
        soundPool.play(soundRes, soundVolume, soundVolume, 1, 0, 1.0f)
    }

    fun release() {
        soundPool.release()
    }


}