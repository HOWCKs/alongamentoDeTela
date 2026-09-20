package br.com.alongamento.tela

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.shell.ShellExecutor
import org.conscrypt.Conscrypt
import rikka.shizuku.Shizuku
import java.security.Security

class AlongamentoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        } catch (_: Throwable) {
        }
        AppPrefs.init(this)
        ShellExecutor.init(this)
        runCatching {
            Shizuku.addBinderReceivedListenerSticky { }
        }
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITOR,
                getString(R.string.channel_monitor),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERT,
                getString(R.string.channel_alert),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    companion object {
        const val CHANNEL_MONITOR = "monitor"
        const val CHANNEL_ALERT = "alerta"
        lateinit var instance: AlongamentoApp
            private set
    }
}
