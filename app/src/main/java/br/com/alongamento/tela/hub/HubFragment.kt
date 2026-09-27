package br.com.alongamento.tela.hub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.com.alongamento.tela.R
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.overlay.GameSession
import br.com.alongamento.tela.overlay.OverlayKind
import br.com.alongamento.tela.overlay.OverlayMutex
import br.com.alongamento.tela.overlay.SessionStart
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HubFragment : Fragment() {
    private var loop: Job? = null
    private var sessionStart = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_hub, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<MaterialButton>(R.id.btnHubOverlay).setOnClickListener { startHub(false) }
        view.findViewById<MaterialButton>(R.id.btnHubGame).setOnClickListener { startHub(true) }
        view.findViewById<MaterialButton>(R.id.btnHubStop).setOnClickListener {
            HubOverlayService.stop(requireContext())
            saveHistory()
            toast(getString(R.string.hub_stopped))
            bind(view)
        }
        view.findViewById<MaterialButton>(R.id.btnModePerf).setOnClickListener { applyProfile(HubProfile.PERFORMANCE) }
        view.findViewById<MaterialButton>(R.id.btnModeBal).setOnClickListener { applyProfile(HubProfile.BALANCED) }
        view.findViewById<MaterialButton>(R.id.btnModeBat).setOnClickListener { applyProfile(HubProfile.BATTERY) }
        view.findViewById<MaterialButton>(R.id.btnQuick).setOnClickListener { quick() }
        view.findViewById<MaterialButton>(R.id.btnRollback).setOnClickListener { rollback() }
        view.findViewById<MaterialButton>(R.id.btnBattery).setOnClickListener {
            HubProfiles.openBatteryUnrestricted(requireContext())
        }
        view.findViewById<MaterialButton>(R.id.btnHistoryClear).setOnClickListener {
            HistoryStore.clear(requireContext())
            bind(view)
        }
        bind(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { bind(it) }
        loop?.cancel()
        loop = viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.Default) { PerformanceSampler.warmupGpu() }
            while (isActive) {
                view?.let { bindLive(it) }
                delay(1000)
            }
        }
    }

    override fun onPause() {
        loop?.cancel()
        super.onPause()
    }

    private fun bind(view: View) {
        val d = PerformanceSampler.device(requireContext())
        view.findViewById<TextView>(R.id.txtHubDevice).text =
            "${d.manufacturer} ${d.model}\nAndroid ${d.androidVersion} (API ${d.sdkInt})\n${d.abi}\n${d.soc}"
        view.findViewById<TextView>(R.id.txtHubGpu).text = d.gpu
        view.findViewById<TextView>(R.id.txtHubShizuku).text =
            if (d.shizukuReady) getString(R.string.hub_shizuku_on) else getString(R.string.hub_shizuku_off)
        view.findViewById<TextView>(R.id.txtHubMutex).text =
            when (OverlayMutex.kind) {
                OverlayKind.STRETCH -> getString(R.string.hub_mutex_stretch)
                OverlayKind.HUB -> getString(R.string.hub_mutex_hub)
                OverlayKind.NONE -> getString(R.string.hub_mutex_none)
            }
        val hist = HistoryStore.list(requireContext()).take(5).joinToString("\n") {
            val t = android.text.format.DateFormat.format("HH:mm", it.timestamp)
            "$t · ${it.profile} · avg ${"%.0f".format(it.avgFps)} · ${it.durationSec}s"
        }.ifBlank { getString(R.string.hub_history_empty) }
        view.findViewById<TextView>(R.id.txtHistory).text = hist
        bindLive(view)
    }

    private fun bindLive(view: View) {
        val s = PerformanceSampler.snapshot(requireContext())
        val d = PerformanceSampler.device(requireContext())
        view.findViewById<TextView>(R.id.txtHubMeters).text =
            "FPS ${if (s.fps > 0) "%.0f".format(s.fps) else "—"}  ·  ${"%.1f".format(s.frameTimeMs)} ms\n" +
                "CPU ${"%.0f".format(s.cpuUsage)}%  ·  RAM ${s.memoryUsedMb}/${s.memoryUsedMb + s.memoryAvailMb} MB\n" +
                "${"%.0f".format(s.refreshRate)} Hz  ·  ${"%.1f".format(s.batteryTemperature)} °C  ·  ${s.thermalBand}\n" +
                "${d.displayWidth}×${d.displayHeight}  ·  bat ${d.batteryLevel}%"
        view.findViewById<TextView>(R.id.txtHubFpsNote).text = getString(R.string.hub_fps_source_overlay)
        val diag = DiagnosticEngine.evaluate(s)
        view.findViewById<TextView>(R.id.txtDiag).text =
            "${diag.kind}\n${diag.reason}\n${diag.evidence}\n${diag.advice}"
        val p = AppPrefs.hubProfile
        view.findViewById<MaterialButton>(R.id.btnModePerf).alpha = if (p == HubProfile.PERFORMANCE.name) 1f else 0.45f
        view.findViewById<MaterialButton>(R.id.btnModeBal).alpha = if (p == HubProfile.BALANCED.name) 1f else 0.45f
        view.findViewById<MaterialButton>(R.id.btnModeBat).alpha = if (p == HubProfile.BATTERY.name) 1f else 0.45f
        view.findViewById<TextView>(R.id.txtGameHub).text =
            if (AppPrefs.selectedPackage.isBlank()) getString(R.string.hub_no_game)
            else getString(R.string.hub_game, AppPrefs.selectedLabel)
    }

    private fun startHub(withGame: Boolean) {
        when (val r = HubSession.prepare(requireContext(), withGame)) {
            SessionStart.Ok -> {
                sessionStart = System.currentTimeMillis()
                PerformanceSampler.resetSession()
                toast(getString(R.string.hub_started))
            }
            SessionStart.NeedOverlay -> {
                toast(getString(R.string.need_overlay))
                GameSession.requestOverlayPermission(requireActivity())
            }
            SessionStart.NeedGame -> toast(getString(R.string.need_game))
            SessionStart.NeedShell -> toast(getString(R.string.need_shell))
            is SessionStart.Error -> toast(r.message)
        }
        view?.let { bind(it) }
    }

    private fun applyProfile(p: HubProfile) {
        viewLifecycleOwner.lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) { HubProfiles.apply(requireContext(), p) }
            val msg = results.joinToString("\n") { "${it.state}: ${it.message}" }
            toast(msg)
            view?.let { bind(it) }
        }
    }

    private fun quick() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.hub_quick)
            .setMessage(R.string.hub_quick_body)
            .setPositiveButton(R.string.apply) { _, _ -> applyProfile(HubProfile.PERFORMANCE) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun rollback() {
        viewLifecycleOwner.lifecycleScope.launch {
            val pkg = AppPrefs.selectedPackage
            val r = withContext(Dispatchers.IO) { HubProfiles.rollback(pkg.ifBlank { "x.x" }) }
            toast("${r.state}: ${r.message}")
        }
    }

    private fun saveHistory() {
        val s = PerformanceSampler.snapshot(requireContext())
        val dur = if (sessionStart == 0L) 0 else ((System.currentTimeMillis() - sessionStart) / 1000).toInt()
        HistoryStore.add(
            requireContext(),
            HistoryEntry(
                System.currentTimeMillis(), s.packageName, s.averageFps, s.minFps, s.maxFps,
                s.batteryTemperature, AppPrefs.hubProfile, dur
            )
        )
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }
}
