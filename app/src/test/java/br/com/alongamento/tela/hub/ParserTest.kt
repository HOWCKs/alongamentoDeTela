package br.com.alongamento.tela.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserTest {
    @Test fun gfxinfo() {
        val s = GfxInfoParser.parse(
            "Total frames rendered: 200\nJanky frames: 20 (10.00%)"
        )
        assertTrue(s.ok)
        assertEquals(200, s.totalFrames)
        assertEquals(20, s.jankyFrames)
        assertEquals(10f, s.jankPercent, 0.01f)
    }

    @Test fun gfxEmpty() {
        assertFalse(GfxInfoParser.parse("").ok)
    }

    @Test fun sfFps() {
        val p = SurfaceFlingerParser.parse("refresh-rate 90.0 fps")
        assertTrue(p.ok)
        assertEquals(90f, p.fps, 0.01f)
    }

    @Test fun sfBad() {
        assertFalse(SurfaceFlingerParser.parse("hello").ok)
    }

    @Test fun pkg() {
        assertTrue(PackageValidator.isSafe("com.supercell.brawlstars"))
        assertFalse(PackageValidator.isSafe("rm -rf /"))
        assertFalse(PackageValidator.isSafe("a;reboot"))
        assertFalse(PackageValidator.isSafe(""))
    }

    @Test fun diagUnknown() {
        val d = DiagnosticEngine.evaluate(
            PerformanceSnapshot(
                0, "", 0f, 0f, 0f, 0f, 0f, 60f, 0f, 0, 1000,
                30f, 0, ThermalBand.COOL, "", 0f, FpsSource.UNKNOWN, "baixa", "GL"
            )
        )
        assertEquals(DiagKind.UNKNOWN, d.kind)
    }
}
