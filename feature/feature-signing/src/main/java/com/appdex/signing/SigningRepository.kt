package com.appdex.signing

import android.util.Log

import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 签名方案配置。
 */
data class SigningSchemeConfig(
    val v1Enabled: Boolean = true,
    val v2Enabled: Boolean = true,
    val v3Enabled: Boolean = true,
)

/**
 * 签名结果。
 */
data class SigningResult(
    val success: Boolean,
    val outputFilePath: String,
    val message: String,
    val verificationResult: VerificationResult? = null,
)

/**
 * 验证结果。
 */
data class VerificationResult(
    val verified: Boolean,
    val errors: List<String> = emptyList(),
    val v1Verified: Boolean = false,
    val v2Verified: Boolean = false,
    val v3Verified: Boolean = false,
)

/**
 * Keystore 条目信息。
 */
data class KeystoreEntryInfo(
    val alias: String,
    val subject: String,
    val notBefore: Date,
    val notAfter: Date,
    val algorithm: String,
)

/**
 * APK 签名仓库。
 *
 * 使用 Google apksig 库实现 V1/V2/V3 签名。
 * 使用 BouncyCastle 创建 Keystore 和自签名证书。
 */
@Singleton
class SigningRepository @Inject constructor() {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    /**
     * 签名 APK (V1/V2/V3)。
     */
    fun signApk(
        inputApkPath: String,
        outputApkPath: String,
        keystorePath: String,
        keystorePassword: String,
        keyAlias: String,
        keyPassword: String,
        schemeConfig: SigningSchemeConfig,
    ): SigningResult {
        // 1. 加载 Keystore
        val keystore = loadKeystore(keystorePath, keystorePassword)
        val protectionParam = KeyStore.PasswordProtection(keyPassword.toCharArray())
        val entry = keystore.getEntry(keyAlias, protectionParam) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalArgumentException("Alias '$keyAlias' not found or not a private key entry")

        val privateKey: PrivateKey = entry.privateKey
        val certChain = entry.certificateChain
        val certificates: List<java.security.cert.X509Certificate> = certChain.mapNotNull { it as? X509Certificate }

        // 2. 配置签名器
        val signerConfig = ApkSigner.SignerConfig.Builder(
            keyAlias,
            privateKey,
            certificates
        ).build()

        // 3. 执行签名
        val signer = ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(File(inputApkPath))
            .setOutputApk(File(outputApkPath))
            .setV1SigningEnabled(schemeConfig.v1Enabled)
            .setV2SigningEnabled(schemeConfig.v2Enabled)
            .setV3SigningEnabled(schemeConfig.v3Enabled)
            .setCreatedBy("APPDEX")
            .build()

        signer.sign()

        // 4. 验证签名
        val verification = verifyApk(outputApkPath)

        return SigningResult(
            success = verification.verified,
            outputFilePath = outputApkPath,
            message = if (verification.verified) "签名成功" else "签名完成但验证失败",
            verificationResult = verification,
        )
    }

    /**
     * 验证 APK 签名。
     */
    fun verifyApk(apkPath: String): VerificationResult {
        val errors = mutableListOf<String>()
        var v1Verified = false
        var v2Verified = false
        var v3Verified = false

        try {
            val verifier = ApkVerifier.Builder(File(apkPath)).build()
            val result = verifier.verify()

            // 检查各签名方案
            v1Verified = result.isVerifiedUsingV1Scheme
            v2Verified = result.isVerifiedUsingV2Scheme
            v3Verified = result.isVerifiedUsingV3Scheme

            // 收集错误
            for (issue in result.errors) {
                errors.add(issue.toString())
            }
        } catch (e: Exception) {
            Log.w("AppDex", "Suppressed exception", e)
            errors.add(e.message ?: "验证失败")
        }

        return VerificationResult(
            verified = errors.isEmpty() && (v1Verified || v2Verified || v3Verified),
            errors = errors,
            v1Verified = v1Verified,
            v2Verified = v2Verified,
            v3Verified = v3Verified,
        )
    }

    /**
     * 创建新的 PKCS12 Keystore + 自签名证书。
     */
    fun createKeystore(
        keystorePath: String,
        keystorePassword: String,
        keyAlias: String,
        keyPassword: String,
        subject: String,
        validityYears: Int = 25,
    ): KeystoreEntryInfo {
        // 1. 生成 RSA 2048 密钥对
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair: KeyPair = keyPairGenerator.generateKeyPair()

        // 2. 创建自签名 X.509 证书
        val now = Date()
        val notAfter = Date(now.time + validityYears * 365L * 24 * 60 * 60 * 1000)
        val x500Name = X500Name(subject)

        val certBuilder = JcaX509v3CertificateBuilder(
            x500Name,
            BigInteger.valueOf(System.currentTimeMillis()),
            now,
            notAfter,
            x500Name,
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .setProvider("BC")
            .build(keyPair.private)

        val certHolder = certBuilder.build(signer)
        val cert: X509Certificate = JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certHolder)

        // 3. 创建 PKCS12 Keystore
        val keystore = KeyStore.getInstance("PKCS12", "BC")
        keystore.load(null, null)
        keystore.setKeyEntry(
            keyAlias,
            keyPair.private,
            keyPassword.toCharArray(),
            arrayOf<Certificate>(cert)
        )

        // 4. 保存到文件
        FileOutputStream(keystorePath).use { fos ->
            keystore.store(fos, keystorePassword.toCharArray())
        }

        return KeystoreEntryInfo(
            alias = keyAlias,
            subject = subject,
            notBefore = now,
            notAfter = notAfter,
            algorithm = "SHA256withRSA",
        )
    }

    /**
     * 加载 Keystore。
     */
    fun loadKeystore(keystorePath: String, password: String): KeyStore {
        val keystore = KeyStore.getInstance("PKCS12", "BC")
        File(keystorePath).inputStream().use { fis ->
            keystore.load(fis, password.toCharArray())
        }
        return keystore
    }

    /**
     * 列出 Keystore 中的所有条目。
     */
    fun listKeystoreEntries(keystorePath: String, password: String): List<KeystoreEntryInfo> {
        val keystore = loadKeystore(keystorePath, password)
        val entries = mutableListOf<KeystoreEntryInfo>()

        val aliases = keystore.aliases()
        while (aliases.hasMoreElements()) {
            val alias = aliases.nextElement()
            val protectionParam = KeyStore.PasswordProtection(password.toCharArray())
            val entry = keystore.getEntry(alias, protectionParam) as? KeyStore.PrivateKeyEntry
            if (entry != null) {
                val cert = entry.certificate as? X509Certificate
                if (cert != null) {
                    entries.add(
                        KeystoreEntryInfo(
                            alias = alias,
                            subject = cert.subjectX500Principal.name,
                            notBefore = cert.notBefore,
                            notAfter = cert.notAfter,
                            algorithm = cert.sigAlgName,
                        )
                    )
                }
            }
        }

        return entries
    }

    /**
     * 获取 APK 签名信息。
     */
    fun getApkSigningInfo(apkPath: String): String {
        return try {
            val verifier = ApkVerifier.Builder(File(apkPath)).build()
            val result = verifier.verify()
            buildString {
                append("V1: ${if (result.isVerifiedUsingV1Scheme) "✓" else "✗"}\n")
                append("V2: ${if (result.isVerifiedUsingV2Scheme) "✓" else "✗"}\n")
                append("V3: ${if (result.isVerifiedUsingV3Scheme) "✓" else "✗"}\n")
            }
        } catch (e: Exception) {
            Log.w("AppDex", "Suppressed exception", e)
            "验证失败: ${e.message}"
        }
    }
}
