package com.appdex.apk

import kotlinx.serialization.Serializable

@Serializable
data class ApkInfo(
    val manifest: ApkManifest,
    val signatures: List<ApkSignature>,
    val entries: List<ApkEntry>,
    val fileSize: Long
)

@Serializable
data class ApkManifest(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val compileSdk: Int? = null,
    val permissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>,
    val metaData: Map<String, String>
)

@Serializable
data class ApkSignature(
    val version: Int,  // 1, 2, or 3
    val algorithm: String,
    val certificateSubject: String,
    val certificateIssuer: String,
    val serialNumber: String,
    val sha256: String,
    val sha1: String,
    val md5: String,
    val validFrom: Long,
    val validTo: Long
)

@Serializable
data class ApkEntry(
    val name: String,
    val size: Long,
    val compressedSize: Long,
    val isDirectory: Boolean
)
