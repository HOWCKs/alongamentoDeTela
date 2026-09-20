package br.com.alongamento.tela.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.com.alongamento.tela.R
import br.com.alongamento.tela.SafetyCountdownActivity
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.data.AspectMath
import br.com.alongamento.tela.data.AspectPreset
import br.com.alongamento.tela.display.DisplayController
import br.com.alongamento.tela.shell.ShellBackend
import br.com.alongamento.tela.shell.ShellExecutor
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class StretchFragment : Fragment() {
    private var presets: List<AspectPreset> = emptyList()
    private var nativeW = 0
    private var nativeH = 0
    private var nativeDpi = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_stretch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val status = view.findViewById<TextView>(R.id.txtStatus)
        val size = view.findViewById<TextView>(R.id.txtSize)
        val chips = view.findViewById<ChipGroup>(R.id.chipPresets)
        val w = view.findViewById<TextInputEditText>(R.id.edtW)
        val h = view.findViewById<TextInputEditText>(R.id.edtH)
        val dpi = view.findViewById<TextInputEditText>(R.id.edtDpi)
        val autoDpi = view.findViewById<SwitchMaterial>(R.id.swAutoDpi)
        autoDpi.isChecked = AppPrefs.autoDpi

        fun fillCustom(pw: Int, ph: Int, pd: Int) {
            w.setText(pw.toString())
            h.setText(ph.toString())
            dpi.setText(pd.toString())
        }

        fun rebuildChips(physW: Int, physH: Int) {
            presets = AspectMath.presets(physW, physH)
            chips.removeAllViews()
            presets.forEach { p ->
                val chip = Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle)
                chip.text = p.title
                chip.isCheckable = true
                chip.isChecked = p.id == AppPrefs.lastRatioId
                chip.setOnClickListener {
                    AppPrefs.lastRatioId = p.id
                    fillCustom(p.w, p.h, nativeDpi)
                    if (autoDpi.isChecked && p.id != "nativo") {
                        val shortN = minOf(nativeW, nativeH)
                        val shortP = minOf(p.w, p.h)
                        dpi.setText(DisplayController.scaledDpi(nativeDpi, shortN, shortP).toString())
                    }
                }
                chips.addView(chip)
            }
        }

        fun refresh() {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val st = DisplayController.snapshot(requireContext())
                    nativeW = st.physicalW
                    nativeH = st.physicalH
                    nativeDpi = st.physicalDpi
                    val backend = when (ShellExecutor.backend()) {
                        ShellBackend.SHIZUKU -> getString(R.string.backend_shizuku)
                        ShellBackend.ADB -> getString(R.string.backend_adb)
                        ShellBackend.NONE -> getString(R.string.backend_none)
                    }
                    status.text = backend
                    size.text = getString(R.string.current_size, st.prettySize(), st.prettyDpi())
                    rebuildChips(st.physicalW, st.physicalH)
                    if (w.text.isNullOrBlank()) fillCustom(st.currentW, st.currentH, st.currentDpi)
                } catch (e: Exception) {
                    status.text = e.message
                }
            }
        }

        view.findViewById<MaterialButton>(R.id.btnApply).setOnClickListener {
            val width = w.text.toString().toIntOrNull()
            val height = h.text.toString().toIntOrNull()
            val density = dpi.text.toString().toIntOrNull()
            if (width == null || height == null) {
                toast("Informe largura e altura")
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val useDpi = if (autoDpi.isChecked) density else density
                    DisplayController.apply(width, height, useDpi)
                    AppPrefs.autoDpi = autoDpi.isChecked
                    if (AppPrefs.safetyEnabled) {
                        startActivity(
                            Intent(requireContext(), SafetyCountdownActivity::class.java)
                                .putExtra("seconds", AppPrefs.safetySeconds)
                        )
                    } else {
                        toast(getString(R.string.applied))
                    }
                    if (AppPrefs.launchAfter && AppPrefs.selectedPackage.isNotBlank()) {
                        val launch = requireContext().packageManager.getLaunchIntentForPackage(AppPrefs.selectedPackage)
                        if (launch != null) startActivity(launch)
                    }
                    refresh()
                } catch (e: Exception) {
                    toast(e.message ?: "falha")
                }
            }
        }

        view.findViewById<MaterialButton>(R.id.btnRestore).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    DisplayController.restore()
                    toast(getString(R.string.restored))
                    refresh()
                } catch (e: Exception) {
                    toast(e.message ?: "falha")
                }
            }
        }

        autoDpi.setOnCheckedChangeListener { _, c -> AppPrefs.autoDpi = c }
        refresh()
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }
}
