package br.com.alongamento.tela.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.shell.ShellExecutor

sealed class SessionStart {
    data object Ok : SessionStart()
    data class Error(val message: String) : SessionStart()
    data object NeedOverlay : SessionStart()
    data object NeedShell : SessionStart()
    data object NeedGame : SessionStart()
}

object GameSession {
    fun canDraw(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        if (context !is android.app.Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun prepare(context: Context): SessionStart {
        if (!ShellExecutor.isReady()) return SessionStart.NeedShell
        if (AppPrefs.selectedPackage.isBlank()) return SessionStart.NeedGame
        if (!canDraw(context)) return SessionStart.NeedOverlay
        context.packageManager.getLaunchIntentForPackage(AppPrefs.selectedPackage)
            ?: return SessionStart.Error("Não achei como abrir ${AppPrefs.selectedLabel}.")
        AppPrefs.flush()
        val app = context.applicationContext
        OverlayService.start(context)
        val h = Handler(Looper.getMainLooper())
        h.postDelayed({ OverlayService.raise(app) }, 450)
        h.postDelayed({ OverlayService.raise(app) }, 1600)
        h.postDelayed({ OverlayService.raise(app) }, 2800)
        return SessionStart.Ok
    }

    fun overlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            android.view.WindowManager.LayoutParams.TYPE_PHONE
        }
    }
}
