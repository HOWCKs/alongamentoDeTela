package br.com.alongamento.tela.display

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.data.Projection
import br.com.alongamento.tela.data.ProjectionMode
import br.com.alongamento.tela.data.ProjectionPlan
import br.com.alongamento.tela.data.WmState
import br.com.alongamento.tela.shell.ShellExecutor

object DisplayController {
    fun metrics(context: Context): Triple<Int, Int, Int> {
        val dm = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        return Triple(dm.widthPixels, dm.heightPixels, dm.densityDpi)
    }

    suspend fun snapshot(context: Context): WmState {
        val (mw, mh, md) = metrics(context)
        if (!ShellExecutor.isReady()) {
            rememberNative(mw, mh, md)
            return WmState(mw, mh, null, null, md, null)
        }
        val size = ShellExecutor.exec("wm size")
        val density = ShellExecutor.exec("wm density")
        val state = WmState.parse(size, density, mw, mh, md)
        rememberNative(state.physicalW, state.physicalH, state.physicalDpi)
        return state
    }

    fun currentPlan(context: Context): ProjectionPlan {
        val (mw, mh, _) = metrics(context)
        val w = if (AppPrefs.nativeW > 0) AppPrefs.nativeW else mw
        val h = if (AppPrefs.nativeH > 0) AppPrefs.nativeH else mh
        return Projection.plan(w, h, AppPrefs.multiplier, AppPrefs.mode)
    }

    private fun rememberNative(w: Int, h: Int, dpi: Int) {
        if (w > 0) {
            AppPrefs.nativeW = w
            AppPrefs.nativeH = h
            AppPrefs.nativeDpi = dpi
        }
    }

    suspend fun applyPlan(plan: ProjectionPlan) {
        require(plan.projW in 240..7680 && plan.projH in 240..7680) { "Resolução fora do intervalo seguro." }
        ShellExecutor.exec("wm size ${plan.projW}x${plan.projH}")
        if (plan.mode == ProjectionMode.CORTE && plan.cropEachSide > 0) {
            val c = plan.cropEachSide
            runCatching { ShellExecutor.exec("wm overscan $c,0,$c,0") }
        } else {
            runCatching { ShellExecutor.exec("wm overscan 0,0,0,0") }
        }
        AppPrefs.lastWidth = plan.projW
        AppPrefs.lastHeight = plan.projH
        AppPrefs.stretched = true
    }

    suspend fun restore() {
        runCatching { ShellExecutor.exec("wm overscan reset") }
        runCatching { ShellExecutor.exec("wm overscan 0,0,0,0") }
        ShellExecutor.exec("wm size reset")
        runCatching { ShellExecutor.exec("wm density reset") }
        AppPrefs.stretched = false
    }
}
