package com.appdex.apk

import java.io.Closeable
import java.io.File
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
        // TODO: Implement binary XML decoding for AndroidManifest.xml
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

    private fun getSignatures(): List<ApkSignature> {
        signatures?.let { return it }
        // TODO: Implement v1/v2/v3 signature parsing
        val result = emptyList<ApkSignature>()
        signatures = result
        return result
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
