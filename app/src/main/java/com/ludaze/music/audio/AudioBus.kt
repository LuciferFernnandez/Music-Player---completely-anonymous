package com.ludaze.music.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Real-time amplitude (0..1) published from the audio thread. */
object AudioBus {
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level
    fun push(v: Float) { _level.value = v }
}

/** Equalizer + BassBoost bound to ExoPlayer's audioSessionId, with saved settings. */
object Effects {
    var eq: Equalizer? = null; private set
    var bass: BassBoost? = null; private set
    private val _ready = MutableStateFlow(0)
    val ready: StateFlow<Int> = _ready
    private var prefs: android.content.SharedPreferences? = null

    fun init(ctx: Context) { prefs = ctx.applicationContext.getSharedPreferences("ludaze_fx", Context.MODE_PRIVATE) }

    fun attach(sessionId: Int) {
        release()
        if (sessionId == 0) return
        try {
            eq = Equalizer(0, sessionId).apply {
                enabled = true
                for (b in 0 until numberOfBands.toInt()) {
                    prefs?.getInt("band_$b", Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE }?.let {
                        setBandLevel(b.toShort(), it.coerceIn(bandLevelRange[0].toInt(), bandLevelRange[1].toInt()).toShort())
                    }
                }
            }
            bass = BassBoost(0, sessionId).apply {
                enabled = true
                if (strengthSupported) setStrength((prefs?.getInt("bass", 0) ?: 0).toShort())
            }
        } catch (_: Exception) { }
        _ready.value = _ready.value + 1
    }

    fun setBand(band: Int, level: Int) {
        eq?.setBandLevel(band.toShort(), level.toShort())
        prefs?.edit()?.putInt("band_$band", level)?.apply()
    }

    fun setBass(strength: Int) {
        bass?.setStrength(strength.toShort())
        prefs?.edit()?.putInt("bass", strength)?.apply()
    }

    fun resetAll() {
        eq?.let { e -> for (b in 0 until e.numberOfBands.toInt()) setBand(b, 0) }
        setBass(0)
        _ready.value = _ready.value + 1
    }

    fun release() { eq?.release(); bass?.release(); eq = null; bass = null }
}
