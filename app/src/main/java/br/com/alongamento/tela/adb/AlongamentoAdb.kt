package br.com.alongamento.tela.adb

import android.content.Context
import android.os.Build
import br.com.alongamento.tela.data.AppPrefs
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import io.github.muntashirakon.adb.AdbStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

class AlongamentoAdbManager private constructor(context: Context) : AbsAdbConnectionManager() {
    private val privateKey: PrivateKey
    private val certificate: Certificate

    init {
        setApi(Build.VERSION.SDK_INT)
        val keys = KeyStoreHelper.loadOrCreate(context)
        privateKey = keys.first
        certificate = keys.second
    }

    override fun getPrivateKey(): PrivateKey = privateKey
    override fun getCertificate(): Certificate = certificate
    override fun getDeviceName(): String = "AlongamentoDeTela"

    companion object {
        @Volatile private var instance: AlongamentoAdbManager? = null
        fun get(context: Context): AlongamentoAdbManager {
            return instance ?: synchronized(this) {
                instance ?: AlongamentoAdbManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

private object KeyStoreHelper {
    fun loadOrCreate(context: Context): Pair<PrivateKey, Certificate> {
        val keyFile = File(context.filesDir, "adb_private.key")
        val certFile = File(context.filesDir, "adb_cert.pem")
        if (keyFile.exists() && certFile.exists()) {
            val key = KeyFactory.getInstance("RSA")
                .generatePrivate(PKCS8EncodedKeySpec(keyFile.readBytes()))
            val cert = certFile.inputStream().use {
                CertificateFactory.getInstance("X.509").generateCertificate(it)
            }
            return key to cert
        }
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val pair = kpg.generateKeyPair()
        val now = Date()
        val until = Date(now.time + 10L * 365 * 24 * 3600 * 1000)
        val name = X500Name("CN=AlongamentoDeTela")
        val builder = JcaX509v3CertificateBuilder(
            name,
            BigInteger.valueOf(System.currentTimeMillis()),
            now,
            until,
            name,
            pair.public
        )
        val signer = JcaContentSignerBuilder("SHA256WithRSA").build(pair.private)
        val holder = builder.build(signer)
        val cert = JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider())
            .getCertificate(holder)
        keyFile.writeBytes(pair.private.encoded)
        certFile.writeBytes(cert.encoded)
        return pair.private to cert
    }
}

object AlongamentoAdb {
    private val connected = AtomicBoolean(false)

    fun isConnected(): Boolean = connected.get()

    suspend fun pair(context: Context, host: String, port: Int, code: String) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            error("Pareamento Wi‑Fi exige Android 11 ou superior.")
        }
        val mgr = AlongamentoAdbManager.get(context)
        val ok = mgr.pair(host, port, code)
        if (!ok) error("Pareamento recusado. Confira IP, porta e o código de 6 dígitos.")
        AppPrefs.adbHost = host
        AppPrefs.adbPairPort = port
    }

    suspend fun connect(context: Context, host: String, port: Int) = withContext(Dispatchers.IO) {
        val mgr = AlongamentoAdbManager.get(context)
        val ok = mgr.connect(host, port)
        if (!ok) error("Não conectou no ADB $host:$port. Pareie primeiro e use a porta de conexão (não a de pareamento).")
        connected.set(true)
        AppPrefs.adbHost = host
        AppPrefs.adbConnectPort = port
    }

    suspend fun autoConnect(context: Context) = withContext(Dispatchers.IO) {
        val mgr = AlongamentoAdbManager.get(context)
        val ok = mgr.autoConnect(context, 8_000)
        if (!ok) error("Não achei a depuração sem fio. Abra as opções de desenvolvedor e ative Depuração sem fio.")
        connected.set(true)
    }

    suspend fun shell(command: String): String = withContext(Dispatchers.IO) {
        val mgr = AlongamentoAdbManager.get(br.com.alongamento.tela.AlongamentoApp.instance)
        val stream: AdbStream = mgr.openStream("shell:$command")
        stream.openInputStream().bufferedReader().use { it.readText() }.trim()
    }

    fun disconnect() {
        connected.set(false)
        try {
            AlongamentoAdbManager.get(br.com.alongamento.tela.AlongamentoApp.instance).disconnect()
        } catch (_: Throwable) {
        }
    }
}
