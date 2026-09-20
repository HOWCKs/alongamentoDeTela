package br.com.alongamento.tela.display

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.Surface
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
        val (w, h) = Projection.landscapeNative(dm.widthPixels, dm.heightPixels)
        return Triple(w, h, dm.densityDpi)
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
        val (pw, ph) = Projection.landscapeNative(state.physicalW, state.physicalH)
        rememberNative(pw, ph, state.physicalDpi)
        return state
    }

    fun currentPlan(context: Context): ProjectionPlan {
        val (mw, mh, _) = metrics(context)
        val w = if (AppPrefs.nativeW > 0) AppPrefs.nativeW else mw
        val h = if (AppPrefs.nativeH > 0) AppPrefs.nativeH else mh
        val (nw, nh) = Projection.landscapeNative(w, h)
        return Projection.plan(nw, nh, AppPrefs.multiplier, AppPrefs.mode)
    }

    private fun rememberNative(w: Int, h: Int, dpi: Int) {
        if (w <= 0 || h <= 0) return
        val (nw, nh) = Projection.landscapeNative(w, h)
        AppPrefs.nativeW = nw
        AppPrefs.nativeH = nh
        AppPrefs.nativeDpi = dpi
    }

    suspend fun applyPlan(context: Context, plan: ProjectionPlan) {
        require(plan.projW in 240..7680 && plan.projH in 240..7680) { "Resolução fora do intervalo seguro." }
        val (w, h) = Projection.landscapeNative(plan.projW, plan.projH)
        lockLandscape(context)
        ShellExecutor.exec("wm size ${w}x${h}")
        delay(80)
        ShellExecutor.exec("wm size ${w}x${h}")
        if (plan.mode == ProjectionMode.CORTE && plan.cropEachSide > 0) {
            val c = plan.cropEachSide
            runCatching { ShellExecutor.exec("wm overscan $c,0,$c,0") }
        } else {
            runCatching { ShellExecutor.exec("wm overscan 0,0,0,0") }
        }
        AppPrefs.lastWidth = w
        AppPrefs.lastHeight = h
        AppPrefs.stretched = true
        AppPrefs.flush()
    }

    suspend fun restore() {
        runCatching { ShellExecutor.exec("wm overscan reset") }
        runCatching { ShellExecutor.exec("wm overscan 0,0,0,0") }
        ShellExecutor.exec("wm size reset")
        runCatching { ShellExecutor.exec("wm density reset") }
        runCatching { ShellExecutor.exec("wm set-user-rotation free") }
        runCatching { ShellExecutor.exec("settings put system accelerometer_rotation 1") }
        AppPrefs.stretched = false
        AppPrefs.flush()
    }

    private suspend fun lockLandscape(context: Context) {
        val rot = try {
            if (Build.VERSION.SDK_INT >= 30) context.display.rotation
            else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
            }
        } catch (_: Throwable) {
            Surface.ROTATION_90
        }
        val code = when (rot) {
            Surface.ROTATION_270 -> 3
            Surface.ROTATION_180 -> 2
            Surface.ROTATION_0 -> 1
            else -> 1
        }
        runCatching { ShellExecutor.exec("wm set-user-rotation lock $code") }
    }
}
