package br.com.alongamento.tela.monitor

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import br.com.alongamento.tela.AlongamentoApp
import br.com.alongamento.tela.MainActivity
import br.com.alongamento.tela.R
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.display.DisplayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameWatchService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var applied = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESTORE) {
            scope.launch {
                runCatching { DisplayController.restore() }
                applied = false
                stopSelf()
            }
            return START_NOT_STICKY
        }
        startForegroundCompat()
        if (job?.isActive != true) {
            job = scope.launch { loop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val open = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val restore = PendingIntent.getService(
            this, 2,
            Intent(this, GameWatchService::class.java).setAction(ACTION_RESTORE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = NotificationCompat.Builder(this, AlongamentoApp.CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_stretch)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.monitor_running, AppPrefs.selectedLabel.ifBlank { "jogo" }))
            .setContentIntent(open)
            .addAction(R.drawable.ic_restore, getString(R.string.restore), restore)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(42, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(42, n)
        }
    }

    private suspend fun loop() {
        while (scope.isActive) delay(5000)
    }

    companion object {
        const val ACTION_RESTORE = "br.com.alongamento.tela.RESTORE"
        fun start(context: Context) {
            val i = Intent(context, GameWatchService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, GameWatchService::class.java))
        }
    }
}
