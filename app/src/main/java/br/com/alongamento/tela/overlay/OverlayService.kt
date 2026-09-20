package br.com.alongamento.tela.overlay

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import br.com.alongamento.tela.AlongamentoApp
import br.com.alongamento.tela.MainActivity
import br.com.alongamento.tela.R
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.data.Projection
import br.com.alongamento.tela.display.DisplayController
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

class OverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private var root: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startFg()
        attach()
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
        }
        return START_STICKY
    }

    override fun onDestroy() {
        detach()
        scope.cancel()
        super.onDestroy()
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
        val n: Notification = NotificationCompat.Builder(this, AlongamentoApp.CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_stretch)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_running))
            .setContentIntent(open)
            .addAction(R.drawable.ic_restore, getString(R.string.restore), PendingIntent.getService(
                this, 13,
                Intent(this, OverlayService::class.java).setAction(ACTION_RESTORE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))
            .addAction(0, getString(R.string.overlay_close), stop)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(43, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(43, n)
        }
    }

    private fun attach() {
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_root, null)
        root = view
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            GameSession.overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = dp(12)
        lp.y = dp(120)
        params = lp
        bind(view)
        collapse(view)
        windowManager.addView(view, lp)
    }

    private fun detach() {
        root?.let {
            runCatching { windowManager.removeView(it) }
        }
        root = null
    }

    private fun bind(view: View) {
        val bubble = view.findViewById<View>(R.id.bubble)
        val panel = view.findViewById<View>(R.id.panel)
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
        val slider = view.findViewById<Slider>(R.id.sliderMult)
        val btnAlongar = view.findViewById<MaterialButton>(R.id.btnModeAlongar)
        val btnCorte = view.findViewById<MaterialButton>(R.id.btnModeCorte)

        fun refreshPlan() {
            val plan = DisplayController.currentPlan(this)
            txtMult.text = Projection.multiplierLabel(plan.multiplier)
            txtNative.text = plan.nativeLabel
            txtProj.text = plan.projLabel
            val corte = AppPrefs.corteLateral
            btnCorte.alpha = if (corte) 1f else 0.45f
            btnAlongar.alpha = if (corte) 0.45f else 1f
        }

        slider.valueFrom = 1.01f
        slider.valueTo = 1.99f
        slider.stepSize = 0.01f
        slider.value = AppPrefs.multiplier
        slider.addOnChangeListener { _, value, _ ->
            AppPrefs.multiplierCents = (value * 100).toInt()
            refreshPlan()
        }
        btnAlongar.setOnClickListener {
            AppPrefs.corteLateral = false
            refreshPlan()
        }
        btnCorte.setOnClickListener {
            AppPrefs.corteLateral = true
            refreshPlan()
        }
        view.findViewById<MaterialButton>(R.id.btnApply).setOnClickListener {
            scope.launch {
                try {
                    val plan = DisplayController.currentPlan(this@OverlayService)
                    kotlinx.coroutines.withContext(Dispatchers.IO) { DisplayController.applyPlan(plan) }
                    Toast.makeText(this@OverlayService, R.string.applied_overlay, Toast.LENGTH_SHORT).show()
                    expanded = false
                    collapse(view)
                } catch (e: Exception) {
                    Toast.makeText(this@OverlayService, e.message ?: "falha", Toast.LENGTH_LONG).show()
                }
            }
        }
        view.findViewById<MaterialButton>(R.id.btnRestore).setOnClickListener {
            scope.launch {
                try {
                    kotlinx.coroutines.withContext(Dispatchers.IO) { DisplayController.restore() }
                    Toast.makeText(this@OverlayService, R.string.restored, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@OverlayService, e.message ?: "falha", Toast.LENGTH_LONG).show()
                }
            }
        }
        refreshPlan()
        panel.visibility = View.GONE
    }

    private fun expand(view: View) {
        view.findViewById<View>(R.id.panel).visibility = View.VISIBLE
        view.findViewById<View>(R.id.bubble).visibility = View.GONE
        params?.let {
            it.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            it.gravity = Gravity.CENTER
            it.x = 0
            it.y = 0
            windowManager.updateViewLayout(view, it)
        }
    }

    private fun collapse(view: View) {
        view.findViewById<View>(R.id.panel).visibility = View.GONE
        view.findViewById<View>(R.id.bubble).visibility = View.VISIBLE
        params?.let {
            it.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            it.gravity = Gravity.TOP or Gravity.START
            windowManager.updateViewLayout(view, it)
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
                        root?.let { windowManager.updateViewLayout(it, lp) }
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

        fun start(context: Context) {
            val i = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
