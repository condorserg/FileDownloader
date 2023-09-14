package com.condorserg.scopedstorage

import android.content.ContentUris
import android.content.ContentValues
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class VideoRepository {
    private val context = App.appContext
    private var observer: ContentObserver? = null

    fun observeVideos(onChange:() -> Unit ){
        observer = object: ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                onChange()
            }

        }
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer!!
        )
    }
    fun unregObserver(){
        observer?.let { context.contentResolver.unregisterContentObserver(it) }
    }

    suspend fun getVideos(): List<Video> {
        val videos = mutableListOf<Video>()
        withContext(Dispatchers.IO) {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id: Long =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                    val name =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                    val size: Int =
                        cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))

                    val uri =
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    videos += Video(id, uri, name, size)

                }

            }
        }

        return videos
    }

    suspend fun saveVideo(url: String, name: String) {
        withContext(Dispatchers.IO) {
            val videoUri = saveVideoDetails(name)
            downloadVideo(url, videoUri)
            makeVideoVisible(videoUri)
        }
    }

    private fun saveVideoDetails(name: String): Uri {
        val volume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        } else {
            MediaStore.VOLUME_EXTERNAL
        }
        val videoCollectionUri = MediaStore.Video.Media.getContentUri(volume)
        val videoDetails = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/*")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies")
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        return context.contentResolver.insert(videoCollectionUri, videoDetails)!!
    }

    suspend fun downloadVideo(url: String, uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { outputstream ->
            Networking.api
                .getFile(url)
                .byteStream()
                .use { inputStream: InputStream ->
                    inputStream.copyTo(outputstream)
                }

        }
    }

    private fun makeVideoVisible(videoUri: Uri){
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q).not()) return
        val videoDetails = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            context.contentResolver.update(videoUri, videoDetails, null,null)
        }

    suspend fun deleteVideo(id: Long) {
        withContext(Dispatchers.IO){
            val uri =
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            context.contentResolver.delete(uri, null,null)
        }
    }
}