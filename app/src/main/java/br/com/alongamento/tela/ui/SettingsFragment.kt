package br.com.alongamento.tela.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import br.com.alongamento.tela.BuildConfig
import br.com.alongamento.tela.R
import br.com.alongamento.tela.data.AppPrefs
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val safety = view.findViewById<SwitchMaterial>(R.id.swSafety)
        val slider = view.findViewById<Slider>(R.id.sliderSafety)
        val label = view.findViewById<TextView>(R.id.txtSafetyValue)
        safety.isChecked = AppPrefs.safetyEnabled
        slider.value = AppPrefs.safetySeconds.toFloat()
        label.text = getString(R.string.safety_seconds, AppPrefs.safetySeconds)
        safety.setOnCheckedChangeListener { _, c -> AppPrefs.safetyEnabled = c }
        slider.addOnChangeListener { _, value, _ ->
            val v = value.toInt()
            AppPrefs.safetySeconds = v
            label.text = getString(R.string.safety_seconds, v)
        }
        view.findViewById<TextView>(R.id.txtVersion).text =
            getString(R.string.version_fmt, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }
}
