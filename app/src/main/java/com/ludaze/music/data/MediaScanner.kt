package com.ludaze.music.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

object MediaScanner {
    private val exts = setOf("mp3", "flac", "wav", "m4a")

    fun scan(ctx: Context): List<SongEntity> {
        val out = mutableListOf<SongEntity>()
        val proj = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DISPLAY_NAME
        )
        val sel = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"
        ctx.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, sel, null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iT = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iA = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iD = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val iN = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            while (c.moveToNext()) {
                val name = c.getString(iN) ?: ""
                if (name.substringAfterLast('.', "").lowercase() !in exts) continue
                val id = c.getLong(iId)
                val artist = c.getString(iA)?.takeIf { it != "<unknown>" } ?: "Unknown artist"
                out += SongEntity(
                    id, c.getString(iT) ?: name, artist, c.getLong(iD),
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
                )
            }
        }
        return out
    }
}
