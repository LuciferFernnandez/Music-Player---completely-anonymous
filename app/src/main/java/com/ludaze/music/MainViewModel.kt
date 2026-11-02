package com.ludaze.music

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.ludaze.music.audio.PlaybackService
import com.ludaze.music.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlayerUi(
    val currentId: Long? = null, val title: String = "", val artist: String = "",
    val isPlaying: Boolean = false, val pos: Long = 0, val dur: Long = 0,
    val shuffle: Boolean = false, val repeat: Int = Player.REPEAT_MODE_OFF, val speed: Float = 1f
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    val songs = dao.songs().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val favorites = dao.favorites().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val playlists = dao.playlists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val ui = MutableStateFlow(PlayerUi())
    private var controller: MediaController? = null

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val f = MediaController.Builder(app, token).buildAsync()
        f.addListener({
            controller = f.get().also { c ->
                c.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) = sync()
                })
                sync()
            }
        }, MoreExecutors.directExecutor())
        viewModelScope.launch { while (true) { delay(300); sync() } }
    }

    private fun sync() {
        val c = controller ?: return
        ui.value = PlayerUi(
            currentId = c.currentMediaItem?.mediaId?.toLongOrNull(),
            title = c.mediaMetadata.title?.toString() ?: "",
            artist = c.mediaMetadata.artist?.toString() ?: "",
            isPlaying = c.isPlaying, pos = c.currentPosition, dur = c.duration.coerceAtLeast(0),
            shuffle = c.shuffleModeEnabled, repeat = c.repeatMode, speed = c.playbackParameters.speed
        )
    }

    fun scan() = viewModelScope.launch(Dispatchers.IO) {
        val found = MediaScanner.scan(getApplication())
        dao.insertSongs(found)
        dao.deleteMissing(found.map { it.id })
    }

    private fun item(s: SongEntity) = MediaItem.Builder()
        .setMediaId(s.id.toString()).setUri(s.uri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).build()).build()

    fun play(list: List<SongEntity>, index: Int) {
        val c = controller ?: return
        c.setMediaItems(list.map(::item), index, 0); c.prepare(); c.play()
    }
    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNext() }
    fun prev() { controller?.seekToPrevious() }
    fun seek(ms: Long) { controller?.seekTo(ms) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
    fun setSpeed(s: Float) { controller?.setPlaybackSpeed(s) }

    fun toggleFavorite(id: Long) = viewModelScope.launch { dao.toggleFavorite(id) }
    fun playlistSongs(id: Long) = dao.playlistSongs(id)
    fun createPlaylist(name: String) = viewModelScope.launch { dao.insertPlaylist(PlaylistEntity(name = name.trim())) }
    fun renamePlaylist(p: PlaylistEntity, name: String) = viewModelScope.launch { dao.updatePlaylist(p.copy(name = name.trim())) }
    fun deletePlaylist(id: Long) = viewModelScope.launch { dao.clearPlaylist(id); dao.deletePlaylist(id) }
    fun addToPlaylist(pid: Long, sid: Long) = viewModelScope.launch { dao.addToPlaylist(PlaylistSongCrossRef(pid, sid)) }
    fun removeFromPlaylist(pid: Long, sid: Long) = viewModelScope.launch { dao.removeFromPlaylist(pid, sid) }

    override fun onCleared() { controller?.release() }
}
