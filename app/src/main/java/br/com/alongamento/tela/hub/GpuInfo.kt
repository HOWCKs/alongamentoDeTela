package br.com.alongamento.tela.hub

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES10
import android.os.Build

object GpuInfo {
    fun abi(): String = Build.SUPPORTED_ABIS.joinToString()

    fun socHint(): String {
        val socM = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MANUFACTURER else ""
        val socN = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else ""
        val bits = listOf(Build.HARDWARE, Build.BOARD, socM, socN)
        return bits.filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "desconhecido" }
    }

    fun glesRenderer(): String = runCatching { queryEgl() }.getOrElse { "GPU: ${it.message ?: "indisponível"}" }

    fun family(renderer: String): String {
        val r = renderer.lowercase()
        return when {
            "adreno" in r -> "Adreno"
            "mali" in r -> "Mali"
            "xclipse" in r -> "Xclipse"
            "powervr" in r || "sgx" in r || "rogue" in r -> "PowerVR"
            "maleoon" in r -> "Maleoon"
            "nvidia" in r || "tegra" in r -> "NVIDIA"
            else -> "GL"
        }
    }

    fun note(renderer: String): String {
        val fam = family(renderer)
        return when (fam) {
            "Adreno" -> "GPU Adreno. Drivers Qualcomm. Não alteramos clock/GPU. WebView usa o Chromium do sistema com aceleração de hardware."
            "Mali" -> "GPU Mali (ARM). Não forçamos OpenGL vs Vulkan. Aceleração HW no app está ligada."
            "Xclipse" -> "GPU Xclipse (Samsung). O compositor One UI decide o alongamento; o Hub só monitora."
            else -> "ABI ${abi()}. Não injetamos OpenGL no jogo. Aceleração de hardware do próprio app está ativa."
        }
    }

    private fun queryEgl(): String {
        val dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val ver = IntArray(2)
        if (!EGL14.eglInitialize(dpy, ver, 0, ver, 1)) return "EGL init falhou"
        val attrib = intArrayOf(EGL14.EGL_RED_SIZE, 8, EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE)
        val configs = arrayOfNulls<EGLConfig>(1)
        val num = IntArray(1)
        EGL14.eglChooseConfig(dpy, attrib, 0, configs, 0, 1, num, 0)
        val cfg = configs[0] ?: return "sem EGLConfig"
        val ctxAttr = intArrayOf(0x3098, 2, EGL14.EGL_NONE)
        val ctx = EGL14.eglCreateContext(dpy, cfg, EGL14.EGL_NO_CONTEXT, ctxAttr, 0)
        val surfAttr = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
        val surf = EGL14.eglCreatePbufferSurface(dpy, cfg, surfAttr, 0)
        EGL14.eglMakeCurrent(dpy, surf, surf, ctx)
        val renderer = GLES10.glGetString(GLES10.GL_RENDERER) ?: "?"
        val vendor = GLES10.glGetString(GLES10.GL_VENDOR) ?: "?"
        val version = GLES10.glGetString(GLES10.GL_VERSION) ?: "?"
        EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(dpy, surf)
        EGL14.eglDestroyContext(dpy, ctx)
        EGL14.eglTerminate(dpy)
        return "$vendor · $renderer · $version"
    }
}
