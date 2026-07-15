package com.appdex.apk

import android.util.Log
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
        } catch (e: Exception) { Log.w("AppDex", "Suppressed exception", e) }

        val defaultManifest = ApkManifest(
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
        manifest = defaultManifest
        return defaultManifest
    }

    private fun parseManifestXml(xml: String): ApkManifest {
        val packageName = extractAttr(xml, "<manifest", "package") ?: ""
        val versionName = extractAttr(xml, "<manifest", "android:versionName")
            ?: extractAttr(xml, "<manifest", ":versionName") ?: ""
        val versionCodeStr = extractAttr(xml, "<manifest", "android:versionCode")
            ?: extractAttr(xml, "<manifest", ":versionCode") ?: "0"
        val versionCode = versionCodeStr.toLongOrNull() ?: 0L

        val usesSdk = extractAttr(xml, "<uses-sdk", "android:minSdkVersion")
            ?: extractAttr(xml, "<uses-sdk", ":minSdkVersion")
        val minSdk = usesSdk?.toIntOrNull() ?: 1
        val targetSdkStr = extractAttr(xml, "<uses-sdk", "android:targetSdkVersion")
            ?: extractAttr(xml, "<uses-sdk", ":targetSdkVersion")
        val targetSdk = targetSdkStr?.toIntOrNull() ?: minSdk

        val permissions = extractAllAttrs(xml, "<uses-permission", "android:name")
            .ifEmpty { extractAllAttrs(xml, "<uses-permission", ":name") }
        val activities = extractAllAttrs(xml, "<activity", "android:name")
            .ifEmpty { extractAllAttrs(xml, "<activity", ":name") }
        val services = extractAllAttrs(xml, "<service", "android:name")
            .ifEmpty { extractAllAttrs(xml, "<service", ":name") }
        val receivers = extractAllAttrs(xml, "<receiver", "android:name")
            .ifEmpty { extractAllAttrs(xml, "<receiver", ":name") }
        val providers = extractAllAttrs(xml, "<provider", "android:name")
            .ifEmpty { extractAllAttrs(xml, "<provider", ":name") }

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
                } catch (e: Exception) { Log.w("AppDex", "Suppressed exception", e) }
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
        } catch (e: Exception) { Log.w("AppDex", "Suppressed exception", e) }

        // ── V2/V3 signature (APK Signature Scheme) ──
        try {
            val v2Sigs = parseV2Signature()
            result.addAll(v2Sigs)
        } catch (e: Exception) { Log.w("AppDex", "Suppressed exception", e) }

        signatures = result
        return result
    }

    private fun parseV2Signature(): List<ApkSignature> {
        val result = mutableListOf<ApkSignature>()
        try {
            val file = File(filePath)
            val raf = java.io.RandomAccessFile(file, "r")
            val fileLength = raf.length()

            // Find End of Central Directory (EOCD)
            val eocdSearchSize = minOf(fileLength, 65557L).toInt()
            val eocdBuffer = ByteArray(eocdSearchSize)
            raf.seek(fileLength - eocdSearchSize)
            raf.readFully(eocdBuffer)

            var eocdOffset = -1L
            for (i in eocdBuffer.size - 22 downTo 0) {
                if (eocdBuffer[i] == 0x50.toByte() && eocdBuffer[i + 1] == 0x4b.toByte() &&
                    eocdBuffer[i + 2] == 0x05.toByte() && eocdBuffer[i + 3] == 0x06.toByte()) {
                    eocdOffset = (fileLength - eocdSearchSize + i).toInt().toLong()
                    break
                }
            }

            if (eocdOffset < 0) {
                raf.close()
                return result
            }

            // Central Directory offset from EOCD
            raf.seek(eocdOffset + 16)
            val cdOffset = java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
                raf.readFully(array())
            }.int.toLong() and 0xFFFFFFFFL

            // Check for APK Signing Block magic before CD
            val magicOffset = cdOffset - 16
            if (magicOffset <= 0) {
                raf.close()
                return result
            }

            raf.seek(magicOffset)
            val magic = ByteArray(16)
            raf.readFully(magic)
            if (!magic.contentEquals("APK Sig Block 42".toByteArray())) {
                raf.close()
                return result
            }

            // Read block size
            raf.seek(cdOffset - 24)
            val blockSizeBuf = ByteArray(8)
            raf.readFully(blockSizeBuf)
            val blockSize = java.nio.ByteBuffer.wrap(blockSizeBuf).order(java.nio.ByteOrder.LITTLE_ENDIAN).long

            if (blockSize <= 0 || cdOffset - 8 - blockSize < 0) {
                raf.close()
                return result
            }

            // Read the entire signing block
            val blockStart = cdOffset - 8 - blockSize
            raf.seek(blockStart)
            val blockData = ByteArray(blockSize.toInt() - 8) // minus the trailing size+magic... actually blockSize includes up to the first size field
            // Actually: the block layout is:
            // [size of block (8 bytes)] [ID-value pairs...] [size of block (8 bytes)] [magic (16 bytes)]
            // blockSize is the size from after first 8 bytes to end (including second size + magic)
            // So total block = 8 + blockSize
            // ID-value pairs start at blockStart + 8

            raf.seek(blockStart + 8)
            val pairsData = ByteArray(blockSize.toInt() - 24) // blockSize - 8 (trailing size) - 16 (magic)
            raf.readFully(pairsData)

            raf.close()

            // Parse ID-value pairs
            val bb = java.nio.ByteBuffer.wrap(pairsData).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            while (bb.remaining() >= 8) {
                val pairLength = bb.long.toInt()
                if (pairLength <= 0 || pairLength > bb.remaining()) break

                val pairStart = bb.position()
                val pairId = bb.int

                when (pairId) {
                    0x7109871a -> {
                        // V2 signature
                        val v2Sigs = parseV2SignerBlock(bb, pairStart, pairLength.toInt())
                        result.addAll(v2Sigs)
                    }
                    0xf05368c0.toInt() -> {
                        // V3 signature - same structure for our purposes
                        val v3Sigs = parseV2SignerBlock(bb, pairStart, pairLength.toInt(), version = 3)
                        result.addAll(v3Sigs)
                    }
                }

                // Move to next pair
                bb.position(pairStart + pairLength.toInt())
            }

        } catch (e: Exception) { Log.w("AppDex", "Suppressed exception", e) }

        return result
    }

    private fun parseV2SignerBlock(
        bb: java.nio.ByteBuffer,
        pairStart: Int,
        pairLength: Int,
        version: Int = 2
    ): List<ApkSignature> {
        val result = mutableListOf<ApkSignature>()
        try {
            // The value is a length-prefixed sequence of signers
                val signersSeqLength = readVarInt(bb)
                val signersEnd = bb.position().toInt() + signersSeqLength

            while (bb.position() < signersEnd) {
                val signerLength = readVarInt(bb)
                val signerEnd = bb.position() + signerLength

                // Signed data (length-prefixed)
                val signedDataLength = readVarInt(bb)
                val signedDataStart = bb.position()
                val signedDataEnd = signedDataStart + signedDataLength

                // Skip digests
                val digestsLength = readVarInt(bb)
                bb.position(bb.position() + digestsLength)

                // Certificates (length-prefixed sequence)
                val certsSeqLength = readVarInt(bb)
                val certsEnd = bb.position() + certsSeqLength

                while (bb.position() < certsEnd) {
                    val certLength = readVarInt(bb)
                    val certBytes = ByteArray(certLength)
                    bb.get(certBytes)

                    try {
                        val cf = CertificateFactory.getInstance("X.509")
                        val cert = cf.generateCertificate(java.io.ByteArrayInputStream(certBytes)) as X509Certificate
                        result.add(createSignature(version, cert))
                    } catch (e: Exception) { Log.w("AppDex", "Suppressed exception", e) }
                }

                // Move to end of signer
                bb.position(signerEnd)
            }
        } catch (e: Exception) { Log.w("AppDex", "Suppressed exception", e) }

        return result
    }

    private fun readVarInt(bb: java.nio.ByteBuffer): Int {
        return bb.int and 0x7FFFFFFF
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
