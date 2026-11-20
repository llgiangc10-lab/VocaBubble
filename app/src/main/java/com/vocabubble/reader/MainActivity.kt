package com.vocabubble.reader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 84, 48, 48)
        }

        val title = TextView(this).apply {
            text = "VocaBubble — Đọc Màn Hình Karaoke Tiếng Việt"
            textSize = 22f
        }

        val desc = TextView(this).apply {
            text = "Cấp quyền Hiển thị trên ứng dụng khác để khi bạn bôi đen văn bản ở bất kỳ ứng dụng nào (Chrome, Zalo, Facebook, truyện chữ...) và bấm 'Đọc với VocaBubble', bong bóng karaoke sẽ hiện lên ngay lập tức."
            setPadding(0, 24, 0, 32)
        }

        val permBtn = Button(this).apply {
            text = "1. Cấp quyền hiển thị Bubble nổi"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            }
        }

        val input = EditText(this).apply {
            hint = "Dán thử một đoạn văn bản Tiếng Việt dài tại đây..."
            setText("Chào mừng bạn đến với VocaBubble trên Android. Bạn có thể bôi đen đoạn văn ở bất kỳ ứng dụng nào rồi chọn Đọc với VocaBubble ngay trong menu sao chép.")
            minLines = 4
        }

        val testBtn = Button(this).apply {
            text = "2. Mở Bubble đọc thử ngay"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                }
                val svc = Intent(this@MainActivity, FloatingReaderService::class.java).apply {
                    putExtra(FloatingReaderService.EXTRA_TEXT, input.text.toString())
                }
                ContextCompat.startForegroundService(this@MainActivity, svc)
            }
        }

        root.addView(title)
        root.addView(desc)
        root.addView(permBtn)
        root.addView(input)
        root.addView(testBtn)
        setContentView(root)
    }
}
