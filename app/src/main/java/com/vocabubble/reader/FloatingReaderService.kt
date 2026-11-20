package com.vocabubble.reader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.session.MediaSessionCompat
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import java.util.Locale
import kotlin.math.abs

class FloatingReaderService : Service(), TextToSpeech.OnInitListener {
    companion object {
        const val EXTRA_TEXT = "extra_text"
        const val ACTION_TOGGLE_PLAY = "com.vocabubble.ACTION_TOGGLE_PLAY"
        const val ACTION_STOP = "com.vocabubble.ACTION_STOP"
        private const val CHANNEL_ID = "vocabubble_playback"
        private const val NOTIF_ID = 1001
    }

    private lateinit var windowManager: WindowManager
    private lateinit var mediaSession: MediaSessionCompat
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rootOverlay: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var karaokeTextView: TextView? = null
    private var playPauseBtn: Button? = null
    private var expandedPanel: LinearLayout? = null
    private var miniButton: Button? = null

    private var chunks: List<String> = emptyList()
    private var currentChunkIndex = 0
    private var isPlaying = false
    private var isMini = false
    private var tapToSeekEnabled = false

    // Cấu hình đồng bộ từ bản Web VocaBubble của bạn
    private var fontSizeSp = 17f
    private var speechRate = 1.0f
    private var bgColor = Color.parseColor("#FAF9F6")
    private var textColor = Color.parseColor("#1C1917")
    private var highlightColor = Color.parseColor("#FEF08A")

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        mediaSession = MediaSessionCompat(this, "VocaBubbleSession").apply { isActive = true }
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PLAY -> {
                togglePlayback()
                return START_STICKY
            }
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val incomingText = intent?.getStringExtra(EXTRA_TEXT)?.trim()
        if (!incomingText.isNullOrEmpty()) {
            chunks = splitVietnameseSentences(incomingText)
            currentChunkIndex = 0
            startForeground(NOTIF_ID, buildNotification("Đang đọc văn bản Tiếng Việt"))
            ensureOverlay()
            updateKaraokeDisplay(0, 0)
            if (ttsReady) {
                startSpeakingCurrentChunk()
            } else {
                isPlaying = true
            }
        }
        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("vi", "VN")
            tts?.setSpeechRate(speechRate)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    mainHandler.post { updateKaraokeDisplay(start, end) }
                }
                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        if (isPlaying && currentChunkIndex + 1 < chunks.size) {
                            currentChunkIndex++
                            startSpeakingCurrentChunk()
                        } else {
                            isPlaying = false
                            refreshButtons()
                        }
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {}
            })
            ttsReady = true
            if (isPlaying && chunks.isNotEmpty()) {
                startSpeakingCurrentChunk()
            }
        }
    }

    private fun splitVietnameseSentences(text: String): List<String> {
        val cleaned = text.replace(Regex("\\s+"), " ").trim()
        return cleaned.split(Regex("(?<=[.!?…;])\\s+")).filter { it.isNotBlank() }
    }

    private fun startSpeakingCurrentChunk() {
        val chunk = chunks.getOrNull(currentChunkIndex) ?: return
        isPlaying = true
        refreshButtons()
        tts?.setSpeechRate(speechRate)
        tts?.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, "chunk_$currentChunkIndex")
        updateNotification("Đoạn ${currentChunkIndex + 1}/${chunks.size}")
    }

    private fun togglePlayback() {
        if (isPlaying) {
            isPlaying = false
            tts?.stop()
        } else {
            startSpeakingCurrentChunk()
        }
        refreshButtons()
        updateNotification(if (isPlaying) "Đang phát nền" else "Đã tạm dừng")
    }

    private fun updateKaraokeDisplay(start: Int, end: Int) {
        val chunk = chunks.getOrNull(currentChunkIndex) ?: return
        val spannable = SpannableString(chunk)
        val safeStart = start.coerceIn(0, chunk.length)
        val safeEnd = end.coerceIn(safeStart, chunk.length)
        if (safeEnd > safeStart) {
            spannable.setSpan(BackgroundColorSpan(highlightColor), safeStart, safeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(Color.BLACK), safeStart, safeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        karaokeTextView?.text = spannable
    }

    private fun refreshButtons() {
        playPauseBtn?.text = if (isPlaying) "Tạm dừng" else "Tiếp tục"
        miniButton?.text = if (isPlaying) "❚❚" else "▶"
    }

    private fun ensureOverlay() {
        if (rootOverlay != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 220
        }
        overlayParams = params

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val expanded = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 40f
            }
            elevation = 16f
        }

        val textView = TextView(this).apply {
            setTextColor(textColor)
            textSize = fontSizeSp
            setLineSpacing(6f, 1.3f)
        }
        karaokeTextView = textView

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(780, 360)
            addView(textView)
        }

        val lockSwitch = Switch(this).apply {
            text = "Chạm từ để nhảy"
            setTextColor(textColor)
            isChecked = tapToSeekEnabled
            setOnCheckedChangeListener { _, checked -> tapToSeekEnabled = checked }
        }

        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        val playBtn = Button(this).apply {
            text = "Tạm dừng"
            setOnClickListener { togglePlayback() }
        }
        playPauseBtn = playBtn

        val speedDown = Button(this).apply {
            text = "Tốc độ -"
            setOnClickListener {
                speechRate = (speechRate - 0.1f).coerceAtLeast(0.5f)
                if (isPlaying) startSpeakingCurrentChunk()
            }
        }

        val speedUp = Button(this).apply {
            text = "Tốc độ +"
            setOnClickListener {
                speechRate = (speechRate + 0.1f).coerceAtMost(2.5f)
                if (isPlaying) startSpeakingCurrentChunk()
            }
        }

        val minimizeBtn = Button(this).apply {
            text = "Thu gọn"
            setOnClickListener { setMiniMode(true) }
        }

        controlsRow.addView(playBtn)
        controlsRow.addView(speedDown)
        controlsRow.addView(speedUp)
        controlsRow.addView(minimizeBtn)

        expanded.addView(scroll)
        expanded.addView(lockSwitch)
        expanded.addView(controlsRow)
        expandedPanel = expanded

        val mini = Button(this).apply {
            text = "❚❚"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0F766E"))
            }
            layoutParams = LinearLayout.LayoutParams(156, 156)
            visibility = View.GONE
        }
        attachDraggableEdgeSnap(mini)
        miniButton = mini

        container.addView(expanded)
        container.addView(mini)
        rootOverlay = container
        windowManager.addView(container, params)
    }

    private fun setMiniMode(mini: Boolean) {
        isMini = mini
        expandedPanel?.visibility = if (mini) View.GONE else View.VISIBLE
        miniButton?.visibility = if (mini) View.VISIBLE else View.GONE
        if (mini) snapToNearestEdge()
    }

    private fun attachDraggableEdgeSnap(view: View) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            val p = overlayParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    p.x = initialX + dx
                    p.y = initialY + dy
                    windowManager.updateViewLayout(rootOverlay, p)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        togglePlayback()
                    } else {
                        snapToNearestEdge()
                    }
                    true
                }
                else -> false
            }
        }
        view.setOnLongClickListener {
            setMiniMode(false)
            true
        }
    }

    private fun snapToNearestEdge() {
        val p = overlayParams ?: return
        val screenWidth = resources.displayMetrics.widthPixels
        p.x = if (p.x + 78 < screenWidth / 2) 24 else screenWidth - 180
        windowManager.updateViewLayout(rootOverlay, p)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VocaBubble Đọc Nền",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val toggleIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FloatingReaderService::class.java).setAction(ACTION_TOGGLE_PLAY),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, FloatingReaderService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VocaBubble — Đọc Tiếng Việt")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .addAction(android.R.drawable.ic_media_pause, if (isPlaying) "Tạm dừng" else "Phát", toggleIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Đóng", stopIntent)
            .setStyle(MediaStyle().setMediaSession(mediaSession.sessionToken))
            .setOngoing(isPlaying)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(statusText))
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        mediaSession.release()
        rootOverlay?.let { windowManager.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
