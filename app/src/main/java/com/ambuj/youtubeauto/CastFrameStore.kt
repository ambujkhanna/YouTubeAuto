package com.ambuj.youtubeauto

import android.graphics.Bitmap
import java.util.concurrent.CopyOnWriteArrayList

object CastFrameStore {
    @Volatile
    private var latestFrame: Bitmap? = null

    @Volatile
    var isCasting: Boolean = false
        private set

    private val listeners = CopyOnWriteArrayList<(Bitmap?) -> Unit>()

    fun setCasting(active: Boolean) {
        isCasting = active
        if (!active) setFrame(null)
    }

    fun setFrame(frame: Bitmap?) {
        latestFrame = frame
        listeners.forEach { listener ->
            try { listener(frame) } catch (_: Exception) {}
        }
    }

    fun getFrame(): Bitmap? = latestFrame

    fun addListener(listener: (Bitmap?) -> Unit) {
        listeners.add(listener)
        listener(latestFrame)
    }

    fun removeListener(listener: (Bitmap?) -> Unit) {
        listeners.remove(listener)
    }
}
