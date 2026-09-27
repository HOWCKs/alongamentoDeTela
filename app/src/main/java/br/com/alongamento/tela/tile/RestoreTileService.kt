package br.com.alongamento.tela.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import br.com.alongamento.tela.display.DisplayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RestoreTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        qsTile?.state = Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }

    override fun onClick() {
        scope.launch {
            runCatching { DisplayController.restore() }
        }
    }
}
