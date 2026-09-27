package br.com.alongamento.tela.display

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.data.Projection
import br.com.alongamento.tela.data.ProjectionPlan
import br.com.alongamento.tela.data.WmState
import br.com.alongamento.tela.monitor.GameWatchService
import br.com.alongamento.tela.shell.ShellExecutor
import kotlinx.coroutines.delay

object DisplayController {
    fun metrics(context: Context): Triple<Int, Int, Int> {
        val dm = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        val w = if (AppPrefs.nativeW > 0) AppPrefs.nativeW else minOf(dm.widthPixels, dm.heightPixels)
        val h = if (AppPrefs.nativeH > 0) AppPrefs.nativeH else maxOf(dm.widthPixels, dm.heightPixels)
        return Triple(w, h, dm.densityDpi)
    }

    suspend fun snapshot(context: Context): WmState {
        val dm = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        if (!ShellExecutor.isReady()) {
            rememberNative(dm.widthPixels, dm.heightPixels, dm.densityDpi)
            return WmState(dm.widthPixels, dm.heightPixels, null, null, dm.densityDpi, null)
        }
        val size = ShellExecutor.exec("wm size")
        val density = ShellExecutor.exec("wm density")
        val state = WmState.parse(size, density, dm.widthPixels, dm.heightPixels, dm.densityDpi)
        rememberNative(state.physicalW, state.physicalH, state.physicalDpi)
        return state
    }

    fun currentPlan(context: Context): ProjectionPlan {
        val (mw, mh, _) = metrics(context)
        val w = if (AppPrefs.nativeW > 0) AppPrefs.nativeW else mw
        val h = if (AppPrefs.nativeH > 0) AppPrefs.nativeH else mh
        return Projection.plan(w, h, AppPrefs.multiplier, AppPrefs.mode)
    }

    /** Always store Physical size in natural phone order (1080×2340), never landscape. */
    private fun rememberNative(w: Int, h: Int, dpi: Int) {
        if (w <= 0 || h <= 0) return
        AppPrefs.nativeW = minOf(w, h)
        AppPrefs.nativeH = maxOf(w, h)
        AppPrefs.nativeDpi = dpi
    }

    suspend fun applyPlan(context: Context, plan: ProjectionPlan) {
        require(plan.projW in 240..7680 && plan.projH in 240..7680) { "Resolução fora do intervalo seguro." }
        snapshot(context)
        val live = currentPlan(context)
        val w = live.projW
        val h = live.projH
        runCatching { ShellExecutor.exec("wm density reset") }
        runCatching { ShellExecutor.exec("wm overscan 0,0,0,0") }
        runCatching { ShellExecutor.exec("wm scaling auto") }
        pushSize(w, h)
        delay(150)
        pushSize(w, h)
        delay(800)
        pushSize(w, h)
        AppPrefs.lastWidth = w
        AppPrefs.lastHeight = h
        AppPrefs.stretched = true
        AppPrefs.flush()
        GameWatchService.start(context.applicationContext)
    }

    suspend fun pushSize(w: Int, h: Int) {
        ShellExecutor.exec("wm size ${w}x${h}")
    }

    suspend fun reholdIfDropped(): Boolean {
        if (!AppPrefs.stretched) return false
        val w = AppPrefs.lastWidth
        val h = AppPrefs.lastHeight
        if (w < 240 || h < 240) return false
        if (!ShellExecutor.isReady()) return false
        val raw = runCatching { ShellExecutor.exec("wm size") }.getOrNull() ?: return false
        val over = Regex("""Override size:\s*(\d+)x(\d+)""").find(raw)
        val ow = over?.groupValues?.get(1)?.toIntOrNull()
        val oh = over?.groupValues?.get(2)?.toIntOrNull()
        if (ow == w && oh == h) return false
        pushSize(w, h)
        return true
    }

    suspend fun restore() {
        AppPrefs.stretched = false
        AppPrefs.flush()
        runCatching { ShellExecutor.exec("wm overscan reset") }
        runCatching { ShellExecutor.exec("wm overscan 0,0,0,0") }
        runCatching { ShellExecutor.exec("wm scaling auto") }
        ShellExecutor.exec("wm size reset")
        runCatching { ShellExecutor.exec("wm density reset") }
        runCatching { ShellExecutor.exec("wm set-user-rotation free") }
        runCatching { ShellExecutor.exec("settings put system accelerometer_rotation 1") }
    }
}
