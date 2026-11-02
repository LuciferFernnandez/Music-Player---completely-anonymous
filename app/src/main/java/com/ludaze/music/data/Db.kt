package com.ludaze.music.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uri: String,
    val isFavorite: Boolean = false
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "playlist_song", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(val playlistId: Long, val songId: Long)

@Dao
interface MusicDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSongs(s: List<SongEntity>)
    @Query("DELETE FROM songs WHERE id NOT IN (:keep)") suspend fun deleteMissing(keep: List<Long>)
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE") fun songs(): Flow<List<SongEntity>>
    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE") fun favorites(): Flow<List<SongEntity>>
    @Query("UPDATE songs SET isFavorite = NOT isFavorite WHERE id = :id") suspend fun toggleFavorite(id: Long)

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE") fun playlists(): Flow<List<PlaylistEntity>>
    @Insert suspend fun insertPlaylist(p: PlaylistEntity): Long
    @Update suspend fun updatePlaylist(p: PlaylistEntity)
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun deletePlaylist(id: Long)
    @Query("DELETE FROM playlist_song WHERE playlistId = :id") suspend fun clearPlaylist(id: Long)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addToPlaylist(r: PlaylistSongCrossRef)
    @Query("DELETE FROM playlist_song WHERE playlistId = :pid AND songId = :sid") suspend fun removeFromPlaylist(pid: Long, sid: Long)
    @Query("SELECT s.* FROM songs s JOIN playlist_song r ON s.id = r.songId WHERE r.playlistId = :pid ORDER BY s.title COLLATE NOCASE")
    fun playlistSongs(pid: Long): Flow<List<SongEntity>>
}

@Database(entities = [SongEntity::class, PlaylistEntity::class, PlaylistSongCrossRef::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): MusicDao
    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "ludaze.db").build().also { inst = it }
        }
    }
}
