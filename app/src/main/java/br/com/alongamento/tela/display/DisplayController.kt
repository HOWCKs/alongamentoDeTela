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
import kotlinx.coroutines.delay

object DisplayController {
    fun metrics(context: Context): Triple<Int, Int, Int> {
        val dm = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        val w = if (AppPrefs.nativeW > 0) AppPrefs.nativeW else dm.widthPixels
        val h = if (AppPrefs.nativeH > 0) AppPrefs.nativeH else dm.heightPixels
        return Triple(w, h, dm.densityDpi)
    }

    suspend fun snapshot(context: Context): WmState {
        val dm = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        if (!ShellExecutor.isReady()) {
            rememberNativeFromPhysical(dm.widthPixels, dm.heightPixels, dm.densityDpi)
            return WmState(dm.widthPixels, dm.heightPixels, null, null, dm.densityDpi, null)
        }
        val size = ShellExecutor.exec("wm size")
        val density = ShellExecutor.exec("wm density")
        val state = WmState.parse(size, density, dm.widthPixels, dm.heightPixels, dm.densityDpi)
        rememberNativeFromPhysical(state.physicalW, state.physicalH, state.physicalDpi)
        return state
    }

    fun currentPlan(context: Context): ProjectionPlan {
        val (mw, mh, _) = metrics(context)
        val w = if (AppPrefs.nativeW > 0) AppPrefs.nativeW else mw
        val h = if (AppPrefs.nativeH > 0) AppPrefs.nativeH else mh
        return Projection.plan(w, h, AppPrefs.multiplier, AppPrefs.mode)
    }

    private fun rememberNativeFromPhysical(w: Int, h: Int, dpi: Int) {
        if (w <= 0 || h <= 0) return
        AppPrefs.nativeW = w
        AppPrefs.nativeH = h
        AppPrefs.nativeDpi = dpi
    }

    suspend fun applyPlan(context: Context, plan: ProjectionPlan) {
        require(plan.projW in 240..7680 && plan.projH in 240..7680) { "Resolução fora do intervalo seguro." }
        snapshot(context)
        val live = currentPlan(context)
        val w = live.projW
        val h = live.projH
        runCatching { ShellExecutor.exec("wm overscan 0,0,0,0") }
        if (live.mode == ProjectionMode.CORTE) {
            runCatching { ShellExecutor.exec("wm scaling off") }
        } else {
            runCatching { ShellExecutor.exec("wm scaling auto") }
        }
        ShellExecutor.exec("wm size ${w}x${h}")
        delay(90)
        ShellExecutor.exec("wm size ${w}x${h}")
        AppPrefs.lastWidth = w
        AppPrefs.lastHeight = h
        AppPrefs.stretched = true
        AppPrefs.flush()
    }

    suspend fun restore() {
        runCatching { ShellExecutor.exec("wm overscan reset") }
        runCatching { ShellExecutor.exec("wm overscan 0,0,0,0") }
        runCatching { ShellExecutor.exec("wm scaling auto") }
        ShellExecutor.exec("wm size reset")
        runCatching { ShellExecutor.exec("wm density reset") }
        runCatching { ShellExecutor.exec("wm set-user-rotation free") }
        runCatching { ShellExecutor.exec("settings put system accelerometer_rotation 1") }
        AppPrefs.stretched = false
        AppPrefs.flush()
    }
}
