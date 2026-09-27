package br.com.alongamento.tela.hub

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.overlay.GameSession
import br.com.alongamento.tela.overlay.OverlayMutex
import br.com.alongamento.tela.overlay.SessionStart
import br.com.alongamento.tela.shell.ShellExecutor

object HubSession {
    fun prepare(context: Context, launchGame: Boolean): SessionStart {
        if (!GameSession.canDraw(context)) return SessionStart.NeedOverlay
        if (launchGame && AppPrefs.selectedPackage.isBlank()) return SessionStart.NeedGame
        val launch = if (launchGame) {
            context.packageManager.getLaunchIntentForPackage(AppPrefs.selectedPackage)
                ?: return SessionStart.Error("Não achei como abrir ${AppPrefs.selectedLabel}.")
        } else null
        AppPrefs.flush()
        OverlayMutex.claimHub(context)
        HubOverlayService.start(context)
        val app = context.applicationContext
        val h = Handler(Looper.getMainLooper())
        h.postDelayed({ HubOverlayService.raise(app) }, 400)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            h.postDelayed({ runCatching { app.startActivity(launch) } }, 700)
            h.postDelayed({ HubOverlayService.raise(app) }, 1600)
        }
        return SessionStart.Ok
    }

    fun needsShellForProfiles(): Boolean = !ShellExecutor.isReady()
}
