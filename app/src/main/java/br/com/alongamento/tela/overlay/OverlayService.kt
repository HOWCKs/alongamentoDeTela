package br.com.alongamento.tela.overlay

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
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import br.com.alongamento.tela.AlongamentoApp
import br.com.alongamento.tela.MainActivity
import br.com.alongamento.tela.R
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.data.Projection
import br.com.alongamento.tela.display.DisplayController
import br.com.alongamento.tela.shell.ShellExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class OverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private var root: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false
    private var watchLeft = 0
    private val watchRunnable = object : Runnable {
        override fun run() {
            if (watchLeft <= 0) return
            watchLeft--
            if (root == null || root?.isAttachedToWindow != true) {
                detach()
                attach()
            }
            if (watchLeft > 0) mainHandler.postDelayed(this, 1200)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        OverlayMutex.claimStretch(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startFg()
        attach()
        startWatch()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RESTORE -> {
                scope.launch(Dispatchers.IO) {
                    runCatching { DisplayController.restore() }
                }
            }
            ACTION_RAISE -> restack()
        }
        if (root == null) attach()
        startWatch()
        return START_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(watchRunnable)
        detach()
        scope.cancel()
        OverlayMutex.release(OverlayKind.STRETCH)
        super.onDestroy()
    }

    private fun startWatch() {
        mainHandler.removeCallbacks(watchRunnable)
        watchLeft = 16
        mainHandler.postDelayed(watchRunnable, 800)
    }

    private fun restack() {
        val view = root
        val lp = params
        if (view != null && lp != null && view.isAttachedToWindow) {
            runCatching {
                windowManager.removeView(view)
                windowManager.addView(view, lp)
            }.onFailure {
                detach()
                attach()
            }
        } else {
            detach()
            attach()
        }
        startWatch()
    }

    private fun startFg() {
        val open = PendingIntent.getActivity(
            this, 11,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 12,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val restore = PendingIntent.getService(
            this, 13,
            Intent(this, OverlayService::class.java).setAction(ACTION_RESTORE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = NotificationCompat.Builder(this, AlongamentoApp.CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_stretch)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_running))
            .setContentIntent(open)
            .addAction(R.drawable.ic_restore, getString(R.string.restore), restore)
            .addAction(0, getString(R.string.overlay_close), stop)
            .setOngoing(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(43, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(43, n)
            }
        } catch (_: Throwable) {
            runCatching { startForeground(43, n) }
        }
    }

    private fun attach() {
        if (root != null) return
        if (!GameSession.canDraw(this)) return
        try {
            val themed = ContextThemeWrapper(this, R.style.Theme_Alongamento)
            val view = LayoutInflater.from(themed).inflate(R.layout.overlay_root, null)
            show(view)
        } catch (_: Throwable) {
            attachFallback()
        }
    }

    private fun attachFallback() {
        try {
            val bubble = ImageView(this).apply {
                setImageResource(R.drawable.ic_stretch)
                setBackgroundResource(R.drawable.bg_bubble)
                setPadding(dp(16), dp(16), dp(16), dp(16))
                contentDescription = getString(R.string.overlay_fallback)
                setOnClickListener {
                    scope.launch { runDisplayAction(this@apply, apply = true) }
                }
                setOnLongClickListener {
                    scope.launch { runDisplayAction(this@apply, apply = false) }
                    true
                }
            }
            show(bubble, bindPanel = false)
            Toast.makeText(this, R.string.overlay_fallback, Toast.LENGTH_LONG).show()
        } catch (_: Throwable) {
        }
    }

    private fun show(view: View, bindPanel: Boolean = true) {
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
        lp.x = dp(12)
        lp.y = dp(140)
        params = lp
        root = view
        if (bindPanel) {
            bind(view)
            view.findViewById<View>(R.id.panel).visibility = View.GONE
            view.findViewById<View>(R.id.bubble).visibility = View.VISIBLE
        }
        windowManager.addView(view, lp)
    }

    private fun detach() {
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
    }

    private fun applyLayout(view: View) {
        val lp = params ?: return
        if (view.isAttachedToWindow) {
            runCatching { windowManager.updateViewLayout(view, lp) }
        }
    }

    private fun bind(view: View) {
        val bubble = view.findViewById<View>(R.id.bubble)
        view.findViewById<ImageView>(R.id.imgBubble)
            .setColorFilter(0xFFE10600.toInt())
        view.findViewById<ImageView>(R.id.btnMinimize)
            .setColorFilter(0xFFF4F4F4.toInt())
        bubble.setOnTouchListener(dragOrTap {
            expanded = !expanded
            if (expanded) expand(view) else collapse(view)
        })
        view.findViewById<View>(R.id.btnMinimize).setOnClickListener {
            expanded = false
            collapse(view)
        }
        val txtMult = view.findViewById<TextView>(R.id.txtMult)
        val txtNative = view.findViewById<TextView>(R.id.txtNative)
        val txtProj = view.findViewById<TextView>(R.id.txtProj)
        val slider = view.findViewById<SeekBar>(R.id.sliderMult)
        val btnAlongar = view.findViewById<Button>(R.id.btnModeAlongar)
        val btnCorte = view.findViewById<Button>(R.id.btnModeCorte)

        fun refreshPlan() {
            val plan = DisplayController.currentPlan(this)
            txtMult.text = Projection.multiplierLabel(plan.multiplier)
            txtNative.text = plan.nativeLabel
            txtProj.text = plan.projLabel
            val corte = AppPrefs.corteLateral
            btnCorte.alpha = if (corte) 1f else 0.45f
            btnAlongar.alpha = if (corte) 0.45f else 1f
        }

        slider.max = 20
        slider.progress = (AppPrefs.multiplierCents - 100).coerceIn(0, 20)
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                AppPrefs.multiplierCents = 100 + progress
                refreshPlan()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        btnAlongar.setOnClickListener {
            AppPrefs.corteLateral = false
            refreshPlan()
        }
        btnCorte.setOnClickListener {
            AppPrefs.corteLateral = true
            refreshPlan()
        }
        view.findViewById<Button>(R.id.btnApply).setOnClickListener {
            scope.launch { runDisplayAction(view, apply = true) }
        }
        view.findViewById<Button>(R.id.btnRestore).setOnClickListener {
            scope.launch { runDisplayAction(view, apply = false) }
        }
        refreshPlan()
    }

    private suspend fun runDisplayAction(view: View, apply: Boolean) {
        try {
            if (!ShellExecutor.awaitReady()) {
                Toast.makeText(this, R.string.need_shell, Toast.LENGTH_LONG).show()
                return
            }
            withContext(Dispatchers.IO) {
                if (apply) {
                    DisplayController.applyPlan(
                        this@OverlayService,
                        DisplayController.currentPlan(this@OverlayService)
                    )
                } else {
                    DisplayController.restore()
                }
            }
            Toast.makeText(
                this,
                if (apply) R.string.applied_overlay else R.string.restored,
                Toast.LENGTH_SHORT
            ).show()
            if (apply && view.findViewById<View?>(R.id.panel) != null) {
                expanded = false
                collapse(view)
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "falha", Toast.LENGTH_LONG).show()
        }
    }

    private fun expand(view: View) {
        view.findViewById<View>(R.id.panel).visibility = View.VISIBLE
        view.findViewById<View>(R.id.bubble).visibility = View.GONE
        params?.let {
            it.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            it.gravity = Gravity.CENTER
            it.x = 0
            it.y = 0
            applyLayout(view)
        }
    }

    private fun collapse(view: View) {
        view.findViewById<View>(R.id.panel).visibility = View.GONE
        view.findViewById<View>(R.id.bubble).visibility = View.VISIBLE
        params?.let {
            it.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            it.gravity = Gravity.TOP or Gravity.START
            applyLayout(view)
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
                    downX = event.rawX
                    downY = event.rawY
                    startX = lp.x
                    startY = lp.y
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > dp(8) || abs(dy) > dp(8)) dragged = true
                    if (dragged && !expanded) {
                        lp.x = startX + dx
                        lp.y = startY + dy
                        root?.let { applyLayout(it) }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!dragged) onTap()
                    true
                }
                else -> false
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_STOP = "br.com.alongamento.tela.OVERLAY_STOP"
        const val ACTION_RESTORE = "br.com.alongamento.tela.OVERLAY_RESTORE"
        const val ACTION_RAISE = "br.com.alongamento.tela.OVERLAY_RAISE"

        fun start(context: Context) {
            OverlayMutex.claimStretch(context)
            val i = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }

        fun raise(context: Context) {
            val i = Intent(context, OverlayService::class.java).setAction(ACTION_RAISE)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
