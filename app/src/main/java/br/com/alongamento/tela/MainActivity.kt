package br.com.alongamento.tela

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import br.com.alongamento.tela.hub.HubFragment
import br.com.alongamento.tela.ui.ActivationFragment
import br.com.alongamento.tela.ui.GamesFragment
import br.com.alongamento.tela.ui.SettingsFragment
import br.com.alongamento.tela.ui.StretchFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        supportFragmentManager.fragments.forEach { it.onResume() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        Shizuku.addRequestPermissionResultListener(shizukuListener)
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setOnItemSelectedListener {
            swap(
                when (it.itemId) {
                    R.id.nav_hub -> HubFragment()
                    R.id.nav_games -> GamesFragment()
                    R.id.nav_activation -> ActivationFragment()
                    R.id.nav_settings -> SettingsFragment()
                    else -> StretchFragment()
                }
            )
            true
        }
        if (savedInstanceState == null) {
            swap(StretchFragment())
        }
    }

    private fun swap(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuListener)
        super.onDestroy()
    }
}
