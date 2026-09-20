package br.com.alongamento.tela.overlay

import android.content.Context
import br.com.alongamento.tela.hub.HubOverlayService

enum class OverlayKind { NONE, STRETCH, HUB }

/**
 * Only one floating panel at a time: stretch bubble or Performance Hub.
 */
object OverlayMutex {
    @Volatile
    var kind: OverlayKind = OverlayKind.NONE
        private set

    fun claimStretch(context: Context) {
        if (kind == OverlayKind.HUB) {
            HubOverlayService.stop(context.applicationContext)
        }
        kind = OverlayKind.STRETCH
    }

    fun claimHub(context: Context) {
        if (kind == OverlayKind.STRETCH) {
            OverlayService.stop(context.applicationContext)
        }
        kind = OverlayKind.HUB
    }

    fun release(owner: OverlayKind) {
        if (kind == owner) kind = OverlayKind.NONE
    }
}
