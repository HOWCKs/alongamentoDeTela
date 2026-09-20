package br.com.alongamento.tela.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.display.DisplayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StretchTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        qsTile?.state = if (AppPrefs.stretched) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    override fun onClick() {
        scope.launch {
            runCatching {
                if (AppPrefs.stretched) {
                    DisplayController.restore()
                } else if (AppPrefs.lastWidth > 0) {
                    DisplayController.apply(
                        AppPrefs.lastWidth,
                        AppPrefs.lastHeight,
                        AppPrefs.lastDpi.takeIf { it > 0 },
                        safetyNote = false
                    )
                }
            }
            qsTile?.state = if (AppPrefs.stretched) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            qsTile?.updateTile()
        }
    }
}
