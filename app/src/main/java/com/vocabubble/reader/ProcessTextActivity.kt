package com.vocabubble.reader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * Activity trong suốt nhận đoạn văn bản bôi đen từ menu Copy / Cut / Paste của mọi ứng dụng Android
 * thông qua ACTION_PROCESS_TEXT, khởi chạy ngay FloatingReaderService và tự đóng để không rời khỏi app hiện tại.
 */
class ProcessTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }?.trim()

        if (selectedText.isNullOrEmpty()) {
            finish()
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Vui lòng cấp quyền hiển thị trên ứng dụng khác cho VocaBubble", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
            return
        }

        val serviceIntent = Intent(this, FloatingReaderService::class.java).apply {
            putExtra(FloatingReaderService.EXTRA_TEXT, selectedText)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
        finish()
    }
}
