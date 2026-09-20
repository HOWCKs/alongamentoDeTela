package br.com.alongamento.tela.hub

enum class SupportState {
    SUPPORTED,
    PARTIALLY_SUPPORTED,
    NOT_SUPPORTED,
    PERMISSION_REQUIRED,
    SHIZUKU_REQUIRED,
    ROOT_REQUIRED,
    OEM_RESTRICTED,
    TEMPORARILY_UNAVAILABLE
}

enum class FpsSource { CHOREOGRAPHER, FRAME_METRICS, GFXINFO, SURFACE_FLINGER, UNKNOWN }

enum class RiskLevel { SAFE, MODERATE, ADVANCED, UNSUPPORTED }

enum class HubProfile { BALANCED, PERFORMANCE, BATTERY }

enum class ThermalBand { COOL, WARM, HOT, CRITICAL, THROTTLING_SUSPECTED }

enum class DiagKind {
    CPU_BOUND, GPU_BOUND, THERMAL_BOUND, MEMORY_PRESSURE,
    REFRESH_RATE_LIMIT, GAME_LIMIT, UNKNOWN
}

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val abi: String,
    val soc: String,
    val gpu: String,
    val totalRamMb: Long,
    val displayWidth: Int,
    val displayHeight: Int,
    val refreshRate: Float,
    val batteryLevel: Int,
    val batteryTempC: Float,
    val shizukuReady: Boolean
)

data class PerformanceSnapshot(
    val timestamp: Long,
    val packageName: String,
    val fps: Float,
    val averageFps: Float,
    val minFps: Float,
    val maxFps: Float,
    val frameTimeMs: Float,
    val refreshRate: Float,
    val cpuUsage: Float,
    val memoryUsedMb: Long,
    val memoryAvailMb: Long,
    val batteryTemperature: Float,
    val thermalStatus: Int,
    val thermalBand: ThermalBand,
    val droppedHint: String,
    val jankPercentage: Float,
    val source: FpsSource,
    val confidence: String,
    val gpu: String
)

data class ActionResult(
    val id: String,
    val state: SupportState,
    val message: String,
    val reversible: Boolean = false
)

data class DiagResult(
    val kind: DiagKind,
    val reason: String,
    val evidence: String,
    val confidence: String,
    val advice: String
)

data class HistoryEntry(
    val timestamp: Long,
    val packageName: String,
    val avgFps: Float,
    val minFps: Float,
    val maxFps: Float,
    val tempC: Float,
    val profile: String,
    val durationSec: Int
)
