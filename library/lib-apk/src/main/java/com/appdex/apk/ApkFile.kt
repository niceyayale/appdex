package com.appdex.apk

import java.io.Closeable
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.jar.JarFile
import java.util.zip.ZipFile

class ApkFile(private val filePath: String) : Closeable {

    private val zipFile = ZipFile(File(filePath))
    private var manifest: ApkManifest? = null
    private var signatures: List<ApkSignature>? = null
    private var entries: List<ApkEntry>? = null

    fun parse(): ApkInfo {
        return ApkInfo(
            manifest = getManifest(),
            signatures = getSignatures(),
            entries = listEntries(),
            fileSize = File(filePath).length()
        )
    }

    private fun getManifest(): ApkManifest {
        manifest?.let { return it }
        try {
            val entry = zipFile.getEntry("AndroidManifest.xml")
            if (entry != null) {
                val bytes = zipFile.getInputStream(entry).use { it.readBytes() }
                val xml = BinaryXmlDecoder(bytes).decode()
                val parsed = parseManifestXml(xml)
                manifest = parsed
                return parsed
            }
        } catch (_: Exception) { }

        val placeholder = ApkManifest(
            packageName = "",
            versionName = "",
            versionCode = 0,
            minSdk = 0,
            targetSdk = 0,
            permissions = emptyList(),
            activities = emptyList(),
            services = emptyList(),
            receivers = emptyList(),
            providers = emptyList(),
            metaData = emptyMap()
        )
        manifest = placeholder
        return placeholder
    }

    private fun parseManifestXml(xml: String): ApkManifest {
        val packageName = extractAttr(xml, "<manifest", "package") ?: ""
        val versionName = extractAttr(xml, "<manifest", "android:versionName") ?: ""
        val versionCodeStr = extractAttr(xml, "<manifest", "android:versionCode") ?: "0"
        val versionCode = versionCodeStr.toLongOrNull() ?: 0L

        val usesSdk = extractAttr(xml, "<uses-sdk", "android:minSdkVersion")
        val minSdk = usesSdk?.toIntOrNull() ?: 1
        val targetSdkStr = extractAttr(xml, "<uses-sdk", "android:targetSdkVersion")
        val targetSdk = targetSdkStr?.toIntOrNull() ?: minSdk

        val permissions = extractAllAttrs(xml, "<uses-permission", "android:name")
        val activities = extractAllAttrs(xml, "<activity", "android:name")
        val services = extractAllAttrs(xml, "<service", "android:name")
        val receivers = extractAllAttrs(xml, "<receiver", "android:name")
        val providers = extractAllAttrs(xml, "<provider", "android:name")

        return ApkManifest(
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            minSdk = minSdk,
            targetSdk = targetSdk,
            permissions = permissions,
            activities = activities,
            services = services,
            receivers = receivers,
            providers = providers,
            metaData = emptyMap()
        )
    }

    private fun extractAttr(xml: String, tagPrefix: String, attrName: String): String? {
        val tagIdx = xml.indexOf(tagPrefix)
        if (tagIdx < 0) return null
        val tagEnd = xml.indexOf('>', tagIdx)
        if (tagEnd < 0) return null
        val tagContent = xml.substring(tagIdx, tagEnd)

        val attrIdx = tagContent.indexOf(attrName)
        if (attrIdx < 0) return null
        val eqIdx = tagContent.indexOf('=', attrIdx)
        if (eqIdx < 0) return null
        val quoteIdx = tagContent.indexOf('"', eqIdx)
        if (quoteIdx < 0) return null
        val endQuoteIdx = tagContent.indexOf('"', quoteIdx + 1)
        if (endQuoteIdx < 0) return null

        return tagContent.substring(quoteIdx + 1, endQuoteIdx)
    }

    private fun extractAllAttrs(xml: String, tagPrefix: String, attrName: String): List<String> {
        val results = mutableListOf<String>()
        var searchFrom = 0
        while (true) {
            val tagIdx = xml.indexOf(tagPrefix, searchFrom)
            if (tagIdx < 0) break
            val tagEnd = xml.indexOf('>', tagIdx)
            if (tagEnd < 0) break
            val tagContent = xml.substring(tagIdx, tagEnd)

            val attrIdx = tagContent.indexOf(attrName)
            if (attrIdx >= 0) {
                val eqIdx = tagContent.indexOf('=', attrIdx)
                if (eqIdx >= 0) {
                    val quoteIdx = tagContent.indexOf('"', eqIdx)
                    if (quoteIdx >= 0) {
                        val endQuoteIdx = tagContent.indexOf('"', quoteIdx + 1)
                        if (endQuoteIdx >= 0) {
                            results.add(tagContent.substring(quoteIdx + 1, endQuoteIdx))
                        }
                    }
                }
            }
            searchFrom = tagEnd + 1
        }
        return results
    }

    private fun getSignatures(): List<ApkSignature> {
        signatures?.let { return it }
        val result = mutableListOf<ApkSignature>()

        try {
            // ── V1 signature (JAR signing) ──
            val jarFile = JarFile(File(filePath))
            val metaEntries = mutableListOf<String>()
            val manifestEntry = jarFile.getEntry("META-INF/MANIFEST.MF")

            if (manifestEntry != null) {
                jarFile.entries().toList().forEach { entry ->
                    val name = entry.name.uppercase()
                    if (name.startsWith("META-INF/") && (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".EC"))) {
                        metaEntries.add(entry.name)
                    }
                }
            }

            // Read certificates from v1 signature blocks
            val certs = mutableSetOf<X509Certificate>()
            for (metaEntry in metaEntries) {
                try {
                    val entry = jarFile.getEntry(metaEntry)
                    if (entry != null) {
                        jarFile.getInputStream(entry).use { input ->
                            val cf = CertificateFactory.getInstance("X.509")
                            val certCollection = cf.generateCertificates(input)
                            certCollection.filterIsInstance<X509Certificate>().forEach {
                                certs.add(it)
                            }
                        }
                    }
                } catch (_: Exception) { }
            }

            if (certs.isNotEmpty()) {
                for (cert in certs) {
                    result.add(createSignature(
                        version = 1,
                        cert = cert
                    ))
                }
            }

            jarFile.close()
        } catch (_: Exception) { }

        // ── V2/V3 signature (APK Signature Scheme) ──
        try {
            val v2Sigs = parseV2Signature()
            result.addAll(v2Sigs)
        } catch (_: Exception) { }

        signatures = result
        return result
    }

    private fun parseV2Signature(): List<ApkSignature> {
        val result = mutableListOf<ApkSignature>()
        try {
            // Read the APK Signing Block from the end of the ZIP file
            // The APK Signing Block is located before the Central Directory
            val file = File(filePath)
            val raf = java.io.RandomAccessFile(file, "r")
            val fileLength = raf.length()

            // Find End of Central Directory (EOCD)
            val eocdSearchSize = minOf(fileLength, 65557L).toInt()
            val eocdBuffer = ByteArray(eocdSearchSize)
            raf.seek(fileLength - eocdSearchSize)
            raf.readFully(eocdBuffer)

            var eocdOffset = -1
            for (i in eocdBuffer.size - 22 downTo 0) {
                if (eocdBuffer[i] == 0x50.toByte() && eocdBuffer[i + 1] == 0x4b.toByte() &&
                    eocdBuffer[i + 2] == 0x05.toByte() && eocdBuffer[i + 3] == 0x06.toByte()) {
                    eocdOffset = i
                    break
                }
            }

            if (eocdOffset >= 0) {
                // Central Directory offset
                val cdOffset = (eocdBuffer[eocdOffset + 16].toInt() and 0xFF) or
                    ((eocdBuffer[eocdOffset + 17].toInt() and 0xFF) shl 8) or
                    ((eocdBuffer[eocdOffset + 18].toInt() and 0xFF) shl 16) or
                    ((eocdBuffer[eocdOffset + 19].toInt() and 0xFF) shl 24)

                // Check for APK Signing Block magic before CD
                val magicOffset = cdOffset - 16
                if (magicOffset > 0) {
                    raf.seek(magicOffset.toLong())
                    val magic = ByteArray(16)
                    raf.readFully(magic)
                    val expected = "APK Sig Block 42".toByteArray()
                    if (magic.contentEquals(expected)) {
                        // Read block size
                        raf.seek((cdOffset - 24).toLong())
                        val blockSize = raf.readLong()
                        if (blockSize > 0 && cdOffset.toLong() - 8 - blockSize >= 0) {
                            // Parse signers
                            raf.seek(cdOffset.toLong() - 8 - blockSize)
                            // Skip to v2 block (ID = 0x7109871a)
                            // Simplified: just mark that v2 exists
                            // Full parsing requires complex binary reading
                            // For now, return empty - will be enriched by PackageManager
                        }
                    }
                }
            }

            raf.close()
        } catch (_: Exception) { }

        return result
    }

    private fun createSignature(version: Int, cert: X509Certificate): ApkSignature {
        val encoded = cert.encoded
        val sha256 = bytesToHex(MessageDigest.getInstance("SHA-256").digest(encoded))
        val sha1 = bytesToHex(MessageDigest.getInstance("SHA-1").digest(encoded))
        val md5 = bytesToHex(MessageDigest.getInstance("MD5").digest(encoded))

        return ApkSignature(
            version = version,
            algorithm = cert.sigAlgName ?: "unknown",
            certificateSubject = cert.subjectX500Principal?.name ?: "",
            certificateIssuer = cert.issuerX500Principal?.name ?: "",
            serialNumber = cert.serialNumber?.toString(16) ?: "",
            sha256 = sha256,
            sha1 = sha1,
            md5 = md5,
            validFrom = cert.notBefore?.time ?: 0L,
            validTo = cert.notAfter?.time ?: 0L
        )
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }

    private fun listEntries(): List<ApkEntry> {
        entries?.let { return it }
        val result = zipFile.entries().toList().map { entry ->
            ApkEntry(
                name = entry.name,
                size = entry.size,
                compressedSize = entry.compressedSize,
                isDirectory = entry.isDirectory
            )
        }
        entries = result
        return result
    }

    fun getEntryInputStream(name: String): java.io.InputStream? {
        return zipFile.getEntry(name)?.let { zipFile.getInputStream(it) }
    }

    override fun close() {
        zipFile.close()
    }
}
