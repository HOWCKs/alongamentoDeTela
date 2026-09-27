package br.com.alongamento.tela.apps

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class LaunchApp(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isGame: Boolean
)

object InstalledApps {
    fun list(pm: PackageManager): List<LaunchApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        return resolved.mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
            val info = try { pm.getApplicationInfo(pkg, 0) } catch (_: Exception) { return@mapNotNull null }
            val label = ri.loadLabel(pm).toString()
            val icon = ri.loadIcon(pm)
            val game = (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0 ||
                info.category == ApplicationInfo.CATEGORY_GAME
            LaunchApp(pkg, label, icon, game)
        }.distinctBy { it.packageName }
            .sortedWith(compareByDescending<LaunchApp> { it.isGame }.thenBy { it.label.lowercase() })
    }
}
