package br.com.alongamento.tela.hub

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Choreographer
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import br.com.alongamento.tela.AlongamentoApp
import br.com.alongamento.tela.MainActivity
import br.com.alongamento.tela.R
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.overlay.GameSession
import br.com.alongamento.tela.overlay.OverlayKind
import br.com.alongamento.tela.overlay.OverlayMutex
import kotlin.math.abs

class HubOverlayService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private var root: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false
    private var frames = 0
    private var lastNs = 0L
    private val choreo = Choreographer.getInstance()
    private val frameCb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frames++
            if (lastNs == 0L) lastNs = frameTimeNanos
            val dt = frameTimeNanos - lastNs
            if (dt >= 500_000_000L) {
                val fps = frames * 1_000_000_000f / dt
                PerformanceSampler.overlayFps = fps
                PerformanceSampler.overlaySource = FpsSource.CHOREOGRAPHER
                frames = 0
                lastNs = frameTimeNanos
                root?.let { bindMeters(it) }
            }
            if (root != null) choreo.postFrameCallback(this)
        }
    }
    private val tick = object : Runnable {
        override fun run() {
            root?.let { bindMeters(it) }
            if (root != null) main.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        OverlayMutex.claimHub(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startFg()
        attach()
        choreo.postFrameCallback(frameCb)
        main.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RAISE -> restack()
        }
        if (root == null) attach()
        return START_STICKY
    }

    override fun onDestroy() {
        choreo.removeFrameCallback(frameCb)
        main.removeCallbacks(tick)
        detach()
        OverlayMutex.release(OverlayKind.HUB)
        super.onDestroy()
    }

    private fun startFg() {
        val open = PendingIntent.getActivity(
            this, 31, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 32, Intent(this, HubOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = NotificationCompat.Builder(this, AlongamentoApp.CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_hub)
            .setContentTitle(getString(R.string.hub_title))
            .setContentText(getString(R.string.hub_overlay_running))
            .setContentIntent(open)
            .addAction(0, getString(R.string.overlay_close), stop)
            .setOngoing(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(44, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(44, n)
            }
        } catch (_: Throwable) {
            runCatching { startForeground(44, n) }
        }
    }

    private fun attach() {
        if (root != null) return
        if (!GameSession.canDraw(this)) return
        val themed = ContextThemeWrapper(this, R.style.Theme_Alongamento)
        val view = LayoutInflater.from(themed).inflate(R.layout.overlay_hub, null)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            GameSession.overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = dp(16)
        lp.y = dp(120)
        params = lp
        root = view
        bind(view)
        view.findViewById<View>(R.id.hubPanel).visibility = View.GONE
        view.findViewById<View>(R.id.hubBubble).visibility = View.VISIBLE
        windowManager.addView(view, lp)
    }

    private fun detach() {
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
    }

    private fun restack() {
        val view = root
        val lp = params
        if (view != null && lp != null && view.isAttachedToWindow) {
            runCatching {
                windowManager.removeView(view)
                windowManager.addView(view, lp)
            }
        } else {
            detach()
            attach()
        }
    }

    private fun bind(view: View) {
        view.findViewById<View>(R.id.hubBubble).setOnTouchListener(dragOrTap {
            expanded = !expanded
            if (expanded) expand(view) else collapse(view)
        })
        view.findViewById<View>(R.id.hubClose).setOnClickListener {
            expanded = false
            collapse(view)
        }
        view.findViewById<View>(R.id.hubModePerf).setOnClickListener { AppPrefs.hubProfile = HubProfile.PERFORMANCE.name; bindMeters(view) }
        view.findViewById<View>(R.id.hubModeBal).setOnClickListener { AppPrefs.hubProfile = HubProfile.BALANCED.name; bindMeters(view) }
        view.findViewById<View>(R.id.hubModeBat).setOnClickListener { AppPrefs.hubProfile = HubProfile.BATTERY.name; bindMeters(view) }
        bindMeters(view)
    }

    private fun bindMeters(view: View) {
        val s = PerformanceSampler.snapshot(this)
        view.findViewById<TextView>(R.id.hubFps).text = if (s.fps > 0) "%.0f".format(s.fps) else "—"
        view.findViewById<TextView>(R.id.hubCpu).text = "%.0f".format(s.cpuUsage)
        view.findViewById<TextView>(R.id.hubRam).text = s.memoryUsedMb.toString()
        view.findViewById<TextView>(R.id.hubGpu).text = s.gpu
        view.findViewById<TextView>(R.id.hubHz).text = "%.0f Hz".format(s.refreshRate)
        view.findViewById<TextView>(R.id.hubFt).text = "%.1f ms".format(s.frameTimeMs)
        view.findViewById<TextView>(R.id.hubSrc).text = getString(R.string.hub_fps_source_overlay)
        view.findViewById<TextView>(R.id.hubTemp).text = "%.1f °C".format(s.batteryTemperature)
        view.findViewById<TextView>(R.id.hubProfileLabel).text = AppPrefs.hubProfile
        val p = AppPrefs.hubProfile
        view.findViewById<View>(R.id.hubModePerf).alpha = if (p == HubProfile.PERFORMANCE.name) 1f else 0.45f
        view.findViewById<View>(R.id.hubModeBal).alpha = if (p == HubProfile.BALANCED.name) 1f else 0.45f
        view.findViewById<View>(R.id.hubModeBat).alpha = if (p == HubProfile.BATTERY.name) 1f else 0.45f
    }

    private fun expand(view: View) {
        view.findViewById<View>(R.id.hubPanel).visibility = View.VISIBLE
        view.findViewById<View>(R.id.hubBubble).visibility = View.GONE
        params?.let {
            it.gravity = Gravity.CENTER
            it.x = 0
            it.y = 0
            if (view.isAttachedToWindow) runCatching { windowManager.updateViewLayout(view, it) }
        }
    }

    private fun collapse(view: View) {
        view.findViewById<View>(R.id.hubPanel).visibility = View.GONE
        view.findViewById<View>(R.id.hubBubble).visibility = View.VISIBLE
        params?.let {
            it.gravity = Gravity.TOP or Gravity.START
            if (view.isAttachedToWindow) runCatching { windowManager.updateViewLayout(view, it) }
        }
    }

    private fun dragOrTap(onTap: () -> Unit): View.OnTouchListener {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragged = false
        return View.OnTouchListener { _, event ->
            val lp = params ?: return@OnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = lp.x; startY = lp.y; dragged = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > dp(8) || abs(dy) > dp(8)) dragged = true
                    if (dragged && !expanded) {
                        lp.x = startX + dx
                        lp.y = startY + dy
                        root?.let { if (it.isAttachedToWindow) runCatching { windowManager.updateViewLayout(it, lp) } }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!dragged) onTap(); true
                }
                else -> false
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_STOP = "br.com.alongamento.tela.HUB_STOP"
        const val ACTION_RAISE = "br.com.alongamento.tela.HUB_RAISE"

        fun start(context: Context) {
            OverlayMutex.claimHub(context)
            val i = Intent(context, HubOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }

        fun raise(context: Context) {
            val i = Intent(context, HubOverlayService::class.java).setAction(ACTION_RAISE)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HubOverlayService::class.java))
        }
    }
}
