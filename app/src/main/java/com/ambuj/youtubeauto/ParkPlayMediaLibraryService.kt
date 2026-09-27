package com.ambuj.youtubeauto

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Experimental Media3 media service for Android Auto.
 *
 * This branch keeps the existing MediaProjection phone-cast path unchanged.
 * The service is intentionally a small, real MediaLibraryService so we can
 * test whether Android Auto discovers ParkPlay through the media channel.
 *
 * Audio/video from the phone cast is NOT routed through this service yet.
 * That is the next experiment after service discovery is verified.
 */
class ParkPlayMediaLibraryService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null

    private val callback = object : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(
                LibraryResult.ofItem(ROOT_ITEM, params)
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children = when (parentId) {
                ROOT_ID -> ImmutableList.of(PHONE_CAST_ITEM)
                PHONE_CAST_ID -> ImmutableList.of(PHONE_CAST_STATUS_ITEM)
                else -> ImmutableList.of()
            }

            return Futures.immediateFuture(
                LibraryResult.ofItemList(children, params)
            )
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = when (mediaId) {
                ROOT_ID -> ROOT_ITEM
                PHONE_CAST_ID -> PHONE_CAST_ITEM
                PHONE_CAST_STATUS_ID -> PHONE_CAST_STATUS_ITEM
                else -> null
            }

            return if (item != null) {
                Futures.immediateFuture(LibraryResult.ofItem(item, null))
            } else {
                Futures.immediateFuture(
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                )
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            // Keep the requested item metadata. Actual phone-cast audio
            // routing will be connected in a later experiment.
            return Futures.immediateFuture(mediaItems)
        }
    }

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this).build()

        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            player,
            callback
        ).build()

        Log.i(TAG, "ParkPlay MediaLibraryService created")
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaLibrarySession? {
        Log.i(
            TAG,
            "Media controller connected: package=" + controllerInfo.packageName
        )
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
        }
        mediaLibrarySession = null

        Log.i(TAG, "ParkPlay MediaLibraryService destroyed")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ParkPlayMedia"

        private const val ROOT_ID = "parkplay_root"
        private const val PHONE_CAST_ID = "phone_cast"
        private const val PHONE_CAST_STATUS_ID = "phone_cast_status"

        private val ROOT_ITEM = MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("ParkPlay")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

        private val PHONE_CAST_ITEM = MediaItem.Builder()
            .setMediaId(PHONE_CAST_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Phone Cast")
                    .setSubtitle("Phone screen media")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

        private val PHONE_CAST_STATUS_ITEM = MediaItem.Builder()
            .setMediaId(PHONE_CAST_STATUS_ID)
            .setUri(Uri.parse("parkplay://phone-cast"))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Phone Cast Ready")
                    .setSubtitle("Video is supplied by the ParkPlay phone-cast channel")
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }
}
