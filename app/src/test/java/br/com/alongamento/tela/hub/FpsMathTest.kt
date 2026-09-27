package br.com.alongamento.tela.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FpsMathTest {
    @Test fun frameTime60() {
        assertEquals(16.67f, FpsMath.frameTimeMs(60f), 0.05f)
    }

    @Test fun budget120() {
        assertEquals(8.33f, FpsMath.budgetMs(120f), 0.05f)
    }

    @Test fun jank() {
        assertEquals(10f, FpsMath.jankPercentage(10, 100), 0.01f)
        assertEquals(0f, FpsMath.jankPercentage(1, 0), 0.01f)
    }

    @Test fun plausible() {
        assertTrue(FpsMath.plausibleFps(60f))
        assertFalse(FpsMath.plausibleFps(0f))
        assertFalse(FpsMath.plausibleFps(999f))
    }
}
