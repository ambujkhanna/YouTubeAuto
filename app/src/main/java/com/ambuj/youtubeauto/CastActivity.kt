package com.ambuj.youtubeauto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CastActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var startButton: Button
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var server: ServerSocket? = null
    private var serverThread: Thread? = null

    @Volatile private var latestJpeg: ByteArray? = null
    private val running = AtomicBoolean(false)
    private val executor = Executors.newCachedThreadPool()
    private val frameEncoding = AtomicBoolean(false)

    private val serviceReadyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == CastProjectionService.ACTION_READY && pendingResultData != null) {
                val data = pendingResultData
                val code = pendingResultCode
                pendingResultData = null
                pendingResultCode = -1
                continueCasting(code, data!!)
            }
        }
    }

    private var pendingResultCode = -1
    private var pendingResultData: Intent? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            runOnUiThread {
                stopCasting()
                status.text = "Screen capture stopped."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cast)
        status = findViewById(R.id.castStatus)
        startButton = findViewById(R.id.startCastButton)

        registerReceiver(serviceReadyReceiver, IntentFilter(CastProjectionService.ACTION_READY), RECEIVER_NOT_EXPORTED)

        startButton.setOnClickListener {
            if (running.get()) stopCasting() else requestScreenCapture()
        }
        findViewById<Button>(R.id.closeCastButton).setOnClickListener { finish() }
    }

    private fun requestScreenCapture() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    @Deprecated("Deprecated in Android API 35; retained for compatibility.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAPTURE || resultCode != RESULT_OK || data == null) {
            status.text = "Screen capture permission was not granted."
            return
        }
        startCasting(resultCode, data)
    }

    private fun startCasting(resultCode: Int, data: Intent) {
        try {
            status.text = "Starting screen capture..."
            pendingResultCode = resultCode
            pendingResultData = data
            val serviceIntent = Intent(this, CastProjectionService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)
        } catch (e: Exception) {
            stopCasting()
            status.text = "Unable to start casting: ${e.message ?: "unknown error"}"
        }
    }

    private fun continueCasting(resultCode: Int, data: Intent) {
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            projection?.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                if (!frameEncoding.compareAndSet(false, true)) {
                    image.close()
                    return@setOnImageAvailableListener
                }
                executor.execute {
                    try {
                        val plane = image.planes[0]
                        val pixelStride = plane.pixelStride
                        val rowStride = plane.rowStride
                        val rowPadding = rowStride - pixelStride * width
                        val paddedWidth = width + rowPadding / pixelStride
                        val bitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                        bitmap.copyPixelsFromBuffer(plane.buffer)
                        val cropped = if (paddedWidth != width) {
                            Bitmap.createBitmap(bitmap, 0, 0, width, height)
                        } else bitmap

                        val output = java.io.ByteArrayOutputStream()
                        cropped.compress(Bitmap.CompressFormat.JPEG, 65, output)
                        latestJpeg = output.toByteArray()
                        CastFrameStore.setFrame(cropped.copy(Bitmap.Config.ARGB_8888, false))

                        if (cropped !== bitmap) cropped.recycle()
                        bitmap.recycle()
                    } catch (_: Exception) {
                        // Ignore a frame invalidated during rotation/display changes.
                    } finally {
                        image.close()
                        frameEncoding.set(false)
                    }
                }
            }, null)

            virtualDisplay = projection?.createVirtualDisplay(
                "ParkPlayPhoneCast", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, null
            )

            startServer()
            running.set(true)
            CastFrameStore.setCasting(true)
            status.text = "Casting locally.\nOpen on another device:\nhttp://${localIp()}:${SERVER_PORT}/"
            startButton.text = "Stop Casting"
        } catch (e: Exception) {
            stopCasting()
            status.text = "Unable to start casting: ${e.message ?: "unknown error"}"
        }
    }

    private fun startServer() {
        server = ServerSocket(SERVER_PORT)
        serverThread = Thread {
            try {
                while (!server!!.isClosed) {
                    val client = server!!.accept()
                    executor.execute { serveClient(client) }
                }
            } catch (_: Exception) {
                // Expected when the server socket is closed.
            }
        }.apply {
            name = "ParkPlayCastServer"
            start()
        }
    }

    private fun serveClient(socket: Socket) {
        socket.use { client ->
            try {
                val reader = client.getInputStream().bufferedReader()
                val request = reader.readLine() ?: return
                val path = request.split(" ").getOrNull(1)?.substringBefore("?") ?: "/"
                while (reader.readLine() != "") { /* consume headers */ }

                val output = BufferedOutputStream(client.getOutputStream())
                if (path == "/stream") streamMjpeg(output) else writeHtml(output)
            } catch (_: Exception) {
                // Client disconnected.
            }
        }
    }

    private fun writeHtml(output: OutputStream) {
        val body = """<!doctype html><html><head>
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>ParkPlay Phone Cast</title>
<style>
html,body{margin:0;width:100%;height:100%;background:#000;color:#fff;font-family:Arial,sans-serif;overflow:hidden}
#toolbar{height:58px;display:flex;align-items:center;gap:10px;padding:0 12px;box-sizing:border-box;background:#111}
#toolbar button{font-family:Arial,sans-serif;font-size:18px;font-weight:600;padding:8px 16px}
#viewer{height:calc(100% - 58px);display:flex;align-items:center;justify-content:center;background:#000;overflow:hidden}
#screen{width:100%;height:100%;object-fit:contain}
.fill #screen{object-fit:cover}
.fit #screen{object-fit:contain}
#hint{font-size:18px;margin-left:auto;opacity:.85}
</style>
</head>
<body class="fit">
<div id="toolbar">
  <button onclick="setMode('fill')">Fill</button>
  <button onclick="setMode('fit')">Fit</button>
  <button onclick="reloadStream()">Reload</button>
  <span id="hint">Phone Cast</span>
</div>
<div id="viewer"><img id="screen" src="/stream" alt="Phone screen cast"></div>
<script>
function setMode(mode){document.body.className=mode;}
function reloadStream(){document.getElementById('screen').src='/stream?t='+Date.now();}
</script>
</body></html>""".trimIndent()
        val bytes = body.toByteArray()
        output.write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
        output.write(bytes)
        output.flush()
    }

    private fun streamMjpeg(output: OutputStream) {
        output.write("HTTP/1.1 200 OK\r\nCache-Control: no-cache\r\nConnection: close\r\nContent-Type: multipart/x-mixed-replace; boundary=frame\r\n\r\n".toByteArray())
        output.flush()
        var lastSent: ByteArray? = null
        while (running.get()) {
            val frame = latestJpeg
            if (frame != null && frame !== lastSent) {
                output.write("--frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${frame.size}\r\n\r\n".toByteArray())
                output.write(frame)
                output.write("\r\n".toByteArray())
                output.flush()
                lastSent = frame
            }
            Thread.sleep(100)
        }
    }

    private fun stopCasting() {
        running.set(false)
        latestJpeg = null
        CastFrameStore.setCasting(false)
        try { server?.close() } catch (_: Exception) {}
        server = null
        serverThread = null
        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null
        try { imageReader?.close() } catch (_: Exception) {}
        imageReader = null
        try { projection?.unregisterCallback(projectionCallback) } catch (_: Exception) {}
        try { projection?.stop() } catch (_: Exception) {}
        projection = null
        try { stopService(Intent(this, CastProjectionService::class.java)) } catch (_: Exception) {}
        if (::startButton.isInitialized) startButton.text = "Start Phone Cast"
    }

    override fun onDestroy() {
        try { unregisterReceiver(serviceReadyReceiver) } catch (_: Exception) {}
        stopCasting()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun localIp(): String = try {
        Collections.list(NetworkInterface.getNetworkInterfaces())
            .flatMap { Collections.list(it.inetAddresses) }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress ?: "PHONE_IP"
    } catch (_: Exception) { "PHONE_IP" }

    companion object {
        private const val REQUEST_CAPTURE = 4201
        private const val SERVER_PORT = 8080
    }
}
