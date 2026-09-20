package br.com.alongamento.tela.hub

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.view.WindowManager
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.shell.ShellExecutor
import kotlin.math.roundToInt

object PerformanceSampler {
    @Volatile var overlayFps: Float = 0f
    @Volatile var overlaySource: FpsSource = FpsSource.CHOREOGRAPHER
    @Volatile private var cachedGpu: String = "—"

    private var lastCpu: LongArray? = null
    private val fpsWindow = ArrayDeque<Float>()
    private var minFps = 999f
    private var maxFps = 0f

    fun resetSession() {
        fpsWindow.clear()
        minFps = 999f
        maxFps = 0f
    }

    fun snapshot(context: Context): PerformanceSnapshot {
        val fps = overlayFps
        if (FpsMath.plausibleFps(fps)) {
            fpsWindow.addLast(fps)
            while (fpsWindow.size > 40) fpsWindow.removeFirst()
            if (fps < minFps) minFps = fps
            if (fps > maxFps) maxFps = fps
        }
        val avg = if (fpsWindow.isEmpty()) 0f else fpsWindow.average().toFloat()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val used = ((mi.totalMem - mi.availMem) / (1024 * 1024))
        val avail = mi.availMem / (1024 * 1024)
        val bat = battery(context)
        val thermal = thermalStatus(context)
        val band = band(thermal, bat.second)
        val hz = refresh(context)
        val pkg = AppPrefs.selectedPackage
        return PerformanceSnapshot(
            timestamp = System.currentTimeMillis(),
            packageName = pkg,
            fps = fps,
            averageFps = avg,
            minFps = if (minFps > 240f) 0f else minFps,
            maxFps = maxFps,
            frameTimeMs = FpsMath.frameTimeMs(fps),
            refreshRate = hz,
            cpuUsage = cpuUsage(),
            memoryUsedMb = used,
            memoryAvailMb = avail,
            batteryTemperature = bat.second,
            thermalStatus = thermal,
            thermalBand = band,
            droppedHint = "",
            jankPercentage = 0f,
            source = overlaySource,
            confidence = if (overlaySource == FpsSource.CHOREOGRAPHER) "FPS da bolha, não do jogo" else "estimado",
            gpu = cachedGpu
        )
    }

    fun warmupGpu() {
        if (cachedGpu != "—") return
        cachedGpu = runCatching { GpuInfo.family(GpuInfo.glesRenderer()) }.getOrDefault("GL")
    }

    fun device(context: Context): DeviceInfo {
        val dm = context.resources.displayMetrics
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val bat = battery(context)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val hz = refresh(context)
        val w = if (AppPrefs.nativeW > 0) AppPrefs.nativeW else dm.widthPixels
        val h = if (AppPrefs.nativeH > 0) AppPrefs.nativeH else dm.heightPixels
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            abi = GpuInfo.abi(),
            soc = GpuInfo.socHint(),
            gpu = cachedGpu,
            totalRamMb = mi.totalMem / (1024 * 1024),
            displayWidth = w,
            displayHeight = h,
            refreshRate = hz,
            batteryLevel = bat.first,
            batteryTempC = bat.second,
            shizukuReady = ShellExecutor.isReady()
        )
    }

    private fun refresh(context: Context): Float {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= 30) {
            wm.defaultDisplay.mode.refreshRate
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.refreshRate
        }
    }

    private fun battery(context: Context): Pair<Int, Float> {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) (level * 100f / scale).roundToInt() else -1
        val temp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        return pct to temp
    }

    private fun thermalStatus(context: Context): Int {
        if (Build.VERSION.SDK_INT < 29) return -1
        return runCatching {
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus
        }.getOrDefault(-1)
    }

    private fun band(status: Int, tempC: Float): ThermalBand {
        if (Build.VERSION.SDK_INT >= 29) {
            when (status) {
                PowerManager.THERMAL_STATUS_CRITICAL, PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> return ThermalBand.CRITICAL
                PowerManager.THERMAL_STATUS_SEVERE -> return ThermalBand.HOT
                PowerManager.THERMAL_STATUS_MODERATE -> return ThermalBand.WARM
                PowerManager.THERMAL_STATUS_LIGHT -> return ThermalBand.WARM
            }
        }
        return when {
            tempC >= 45f -> ThermalBand.CRITICAL
            tempC >= 42f -> ThermalBand.HOT
            tempC >= 38f -> ThermalBand.WARM
            else -> ThermalBand.COOL
        }
    }

    private fun cpuUsage(): Float {
        return try {
            val cur = readProcStat()
            val prev = lastCpu
            lastCpu = cur
            if (prev == null || cur == null) return 0f
            val idle = cur[3] - prev[3]
            val total = cur.sum() - prev.sum()
            if (total <= 0) 0f else ((total - idle) * 100f / total).coerceIn(0f, 100f)
        } catch (_: Throwable) {
            0f
        }
    }

    private fun readProcStat(): LongArray? {
        val line = java.io.File("/proc/stat").bufferedReader().use { it.readLine() } ?: return null
        val parts = line.split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
        if (parts.size < 4) return null
        return parts.take(8).toLongArray()
    }

    @Suppress("unused")
    fun pid(): Int = Process.myPid()
}
