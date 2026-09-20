package br.com.alongamento.tela.ui

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
import br.com.alongamento.tela.data.Projection
import br.com.alongamento.tela.display.DisplayController
import br.com.alongamento.tela.overlay.GameSession
import br.com.alongamento.tela.overlay.SessionStart
import br.com.alongamento.tela.shell.ShellBackend
import br.com.alongamento.tela.shell.ShellExecutor
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch

class StretchFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_stretch, container, false)
    }

    override fun onResume() {
        super.onResume()
        view?.let { bind(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val slider = view.findViewById<Slider>(R.id.sliderMult)
        slider.valueFrom = 1.01f
        slider.valueTo = 1.99f
        slider.stepSize = 0.01f
        slider.value = AppPrefs.multiplier
        slider.addOnChangeListener { _, value, _ ->
            AppPrefs.multiplierCents = (value * 100).toInt()
            bind(view)
        }
        view.findViewById<MaterialButton>(R.id.btnModeAlongar).setOnClickListener {
            AppPrefs.corteLateral = false
            bind(view)
        }
        view.findViewById<MaterialButton>(R.id.btnModeCorte).setOnClickListener {
            AppPrefs.corteLateral = true
            bind(view)
        }
        view.findViewById<MaterialButton>(R.id.btnPlay).setOnClickListener { startSession() }
        view.findViewById<MaterialButton>(R.id.btnRestore).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    DisplayController.restore()
                    toast(getString(R.string.restored))
                    bind(view)
                } catch (e: Exception) {
                    toast(e.message ?: "falha")
                }
            }
        }
        bind(view)
    }

    private fun bind(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { DisplayController.snapshot(requireContext()) }
            val plan = DisplayController.currentPlan(requireContext())
            view.findViewById<TextView>(R.id.txtNative).text = plan.nativeLabel
            view.findViewById<TextView>(R.id.txtProj).text = plan.projLabel
            view.findViewById<TextView>(R.id.txtMult).text = Projection.multiplierLabel(plan.multiplier)
            val status = view.findViewById<TextView>(R.id.txtStatus)
            status.text = when (ShellExecutor.backend()) {
                ShellBackend.SHIZUKU -> getString(R.string.backend_shizuku)
                ShellBackend.ADB -> getString(R.string.backend_adb)
                ShellBackend.NONE -> getString(R.string.backend_none)
            }
            val game = view.findViewById<TextView>(R.id.txtGame)
            game.text = if (AppPrefs.selectedPackage.isBlank()) {
                getString(R.string.no_game_home)
            } else {
                getString(R.string.home_game, AppPrefs.selectedLabel)
            }
            val corte = AppPrefs.corteLateral
            view.findViewById<MaterialButton>(R.id.btnModeCorte).alpha = if (corte) 1f else 0.45f
            view.findViewById<MaterialButton>(R.id.btnModeAlongar).alpha = if (corte) 0.45f else 1f
            view.findViewById<TextView>(R.id.txtModeHint).text =
                if (corte) getString(R.string.hint_corte) else getString(R.string.hint_alongar)
        }
    }

    private fun startSession() {
        when (val r = GameSession.prepare(requireContext())) {
            SessionStart.Ok -> toast(getString(R.string.session_started))
            SessionStart.NeedShell -> toast(getString(R.string.need_shell))
            SessionStart.NeedGame -> toast(getString(R.string.need_game))
            SessionStart.NeedOverlay -> {
                toast(getString(R.string.need_overlay))
                GameSession.requestOverlayPermission(requireActivity())
            }
            is SessionStart.Error -> toast(r.message)
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }
}
