package com.snoozecontrol.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.snoozecontrol.AlarmDismissActivity
import com.snoozecontrol.R
import com.snoozecontrol.data.AlarmDao
import com.snoozecontrol.data.AlarmDatabase
import com.snoozecontrol.receiver.AlarmReceiver
import com.snoozecontrol.scheduler.AndroidAlarmScheduler
import com.snoozecontrol.util.UpcomingAlarmNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class AlarmService : Service() {
    @Inject
    lateinit var alarmDao: AlarmDao

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val incomingAction = intent?.action
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1

        if (incomingAction == ACTION_DISMISS) {
            stopSelf()
            return START_NOT_STICKY
        }

        acquireWakeLock()

        val fullScreenIntent = AlarmDismissActivity.createIntent(this, alarmId)

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val turnOffIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TURN_OFF
            putExtra("ALARM_ID", alarmId)
            data = Uri.parse("snoozecontrol://alarm/service/$alarmId")
        }
        val turnOffPendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId,
            turnOffIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            notificationManager?.canUseFullScreenIntent() == true
        } else {
            true
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.rise_and_shine))
            .setContentText(getString(R.string.notification_dismiss_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_snooze_control_1,
                "Turn Off",
                turnOffPendingIntent
            )

        if (canUseFullScreen) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        } else {
            builder.setContentIntent(fullScreenPendingIntent)
        }

        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch(Dispatchers.IO) {
            val alarm = if (alarmId != -1) alarmDao.getAlarmById(alarmId) else null
            val customUriStr = alarm?.ringtoneUri
            withContext(Dispatchers.Main) {
                playAlarmSound(customUriStr)
                startVibration()
            }
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SnoozeControl::AlarmWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L /*10 minutes*/)
            }
        }
    }

    private fun startVibration() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = v
        if (v.hasVibrator()) {
            val pattern = longArrayOf(0, 500, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
        }
    }

    private fun playAlarmSound(customRingtoneUriStr: String? = null) {
        if (mediaPlayer?.isPlaying == true) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(audioAttributes)
                    .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }

        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val soundUri = customRingtoneUriStr?.let { Uri.parse(it) } ?: defaultUri

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(this@AlarmService, soundUri)
                isLooping = true
                setVolume(0.2f, 0.2f) // Start soft for crescendo
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(this@AlarmService, defaultUri)
                    isLooping = true
                    setVolume(0.2f, 0.2f)
                    prepare()
                    start()
                }
                mediaPlayer = player
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        // Ramp volume up from 0.2 to 1.0 over 20 seconds (5 steps of 500ms)
        serviceScope.launch {
            val totalSteps = 5
            val initialVol = 0.2f
            val targetVol = 1.0f
            val volStep = (targetVol - initialVol) / totalSteps
            var currentVol = initialVol

            repeat(totalSteps) {
                delay(500L.milliseconds)
                if (mediaPlayer == null || mediaPlayer?.isPlaying != true) return@launch
                currentVol += volStep
                val clampedVol = currentVol.coerceAtMost(1.0f)
                try {
                    mediaPlayer?.setVolume(clampedVol, clampedVol)
                } catch (_: Exception) {
                    return@launch
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_description)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val context = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AlarmDatabase.getDatabase(context)
                val enabledAlarms = db.alarmDao().getEnabledAlarms()
                val scheduler = AndroidAlarmScheduler(context)

                enabledAlarms.forEach { alarm ->
                    scheduler.schedule(alarm)
                }

                UpcomingAlarmNotificationManager.refreshUpcomingNotification(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
        vibrator = null

        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_DISMISS = "com.snoozecontrol.ACTION_DISMISS"
        private const val CHANNEL_ID = "ALARM_SERVICE_CHANNEL"
        private const val NOTIFICATION_ID = 69
    }
}
