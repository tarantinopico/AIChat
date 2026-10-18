package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class GenerationForegroundService : Service {

    constructor() : super()

    companion object {
        const val CHANNEL_STREAMING_ID = "vertex_ai_streaming"
        const val CHANNEL_COMPLETION_ID = "vertex_ai_completion"
        const val NOTIFICATION_ID = 1001
        const val COMPLETION_NOTIFICATION_ID = 1002

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_UPDATE = "com.example.service.ACTION_UPDATE"
        const val ACTION_COMPLETE = "com.example.service.ACTION_COMPLETE"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_LINE_COUNT = "extra_line_count"
        const val EXTRA_FILES_COUNT = "extra_files_count"
        const val EXTRA_MODEL_ID = "extra_model_id"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun startService(context: Context, modelId: String) {
            val intent = Intent(context, GenerationForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MODEL_ID, modelId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateProgress(context: Context, status: String, lineCount: Int, filesCount: Int) {
            if (!isRunning) return
            val intent = Intent(context, GenerationForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_STATUS, status)
                putExtra(EXTRA_LINE_COUNT, lineCount)
                putExtra(EXTRA_FILES_COUNT, filesCount)
            }
            context.startService(intent)
        }

        fun completeService(context: Context, filesCount: Int, lineCount: Int) {
            val intent = Intent(context, GenerationForegroundService::class.java).apply {
                action = ACTION_COMPLETE
                putExtra(EXTRA_FILES_COUNT, filesCount)
                putExtra(EXTRA_LINE_COUNT, lineCount)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, GenerationForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var notificationManager: NotificationManager
    private var currentModelId: String = "Vertex AI"
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "VertexAI:GenerationServiceWakeLock")
        } catch (_: Exception) {}
        createNotificationChannels()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                isRunning = true
                currentModelId = intent.getStringExtra(EXTRA_MODEL_ID) ?: "Vertex AI"
                try {
                    if (wakeLock?.isHeld == false) {
                        wakeLock?.acquire(15 * 60 * 1000L)
                    }
                } catch (_: Exception) {}

                val notification = buildOngoingNotification(
                    status = "Inicializuji spojení s modelem...",
                    lineCount = 0,
                    filesCount = 0
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }

            ACTION_UPDATE -> {
                val status = intent.getStringExtra(EXTRA_STATUS) ?: "Generuji odpověď..."
                val lines = intent.getIntExtra(EXTRA_LINE_COUNT, 0)
                val files = intent.getIntExtra(EXTRA_FILES_COUNT, 0)
                val notification = buildOngoingNotification(status, lines, files)
                notificationManager.notify(NOTIFICATION_ID, notification)
            }

            ACTION_COMPLETE -> {
                val files = intent.getIntExtra(EXTRA_FILES_COUNT, 0)
                val lines = intent.getIntExtra(EXTRA_LINE_COUNT, 0)
                showCompletionNotification(files, lines)
                stopForeground(STOP_FOREGROUND_REMOVE)
                try {
                    if (wakeLock?.isHeld == true) wakeLock?.release()
                } catch (_: Exception) {}
                isRunning = false
                stopSelf()
            }

            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                try {
                    if (wakeLock?.isHeld == true) wakeLock?.release()
                } catch (_: Exception) {}
                isRunning = false
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
        isRunning = false
        super.onDestroy()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val streamChannel = NotificationChannel(
                CHANNEL_STREAMING_ID,
                "Vertex AI Streamování",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Průběh generování dlouhého kódu a souborů na pozadí"
                setShowBadge(false)
            }

            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETION_ID,
                "Dokončení generování",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Upozornění na dokončení generování souborů"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(streamChannel)
            notificationManager.createNotificationChannel(completeChannel)
        }
    }

    private fun buildOngoingNotification(status: String, lineCount: Int, filesCount: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GenerationForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subText = if (filesCount > 0) {
            "Souborů: $filesCount • Řádků: $lineCount"
        } else {
            "Generování aktivní"
        }

        return NotificationCompat.Builder(this, CHANNEL_STREAMING_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Vertex AI: $currentModelId")
            .setContentText(status)
            .setSubText(subText)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Zastavit", stopPendingIntent)
            .setProgress(0, 0, true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showCompletionNotification(filesCount: Int, lineCount: Int) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            2,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (filesCount > 0) {
            "Vygenerováno $filesCount souborů ($lineCount řádků kódu)."
        } else {
            "Odpověď byla úspěšně vygenerována."
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_COMPLETION_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Generování dokončeno")
            .setContentText(contentText)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }
}
