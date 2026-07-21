package com.appdex.security

import android.util.Log
import com.appdex.apk.ApkFile
import java.io.File
import java.util.regex.Pattern
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 安全问题严重级别。
 */
enum class Severity {
    CRITICAL,   // 严重
    HIGH,       // 高危
    MEDIUM,     // 中危
    LOW,        // 低危
    INFO,       // 信息
}

/**
 * 安全问题类型。
 */
enum class SecurityIssueType {
    // 清单问题
    DEBUGGABLE,
    ALLOW_BACKUP,
    CLEARTEXT_TRAFFIC,
    EXPORTED_COMPONENT,
    DANGEROUS_PERMISSION,
    MISSING_PROTECTION,

    // 硬编码
    HARDCODED_API_KEY,
    HARDCODED_SECRET,
    HARDCODED_TOKEN,
    HARDCODED_PASSWORD,
    HARDCODED_URL,

    // 签名问题
    NO_SIGNATURE,
    V1_ONLY_SIGNATURE,
    WEAK_SIGNATURE_ALGORITHM,

    // 其他
    TRACKING_SDK,
    INSECURE_RANDOM,
    WEAK_CRYPTO,
}

/**
 * 安全问题。
 */
data class SecurityIssue(
    val type: SecurityIssueType,
    val severity: Severity,
    val title: String,
    val description: String,
    val location: String = "",
    val recommendation: String = "",
)

/**
 * 安全扫描结果。
 */
data class SecurityScanResult(
    val apkName: String,
    val apkPath: String,
    val issues: List<SecurityIssue>,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val infoCount: Int,
    val securityScore: Int, // 0-100
) {
    val passed: Boolean get() = criticalCount == 0 && highCount == 0
}

/**
 * APK 安全扫描仓库。
 *
 * 深度扫描 APK 安全漏洞、硬编码密钥、不安全配置。
 */
@Singleton
class SecurityScannerRepository @Inject constructor() {

    // 危险权限列表
    private val dangerousPermissions = setOf(
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",
        "android.permission.CAMERA",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_PHONE_STATE",
        "android.permission.CALL_PHONE",
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.BODY_SENSORS",
        "android.permission.ACTIVITY_RECOGNITION",
    )

    // 硬编码密钥检测正则
    private val apiKeyPatterns = listOf(
        Pattern.compile("(?i)(?:api[_-]?key|apikey)[\"'\\s]*[:=][\"'\\s]*([a-zA-Z0-9]{32,})"),
        Pattern.compile("(?i)(?:secret[_-]?key|secretkey)[\"'\\s]*[:=][\"'\\s]*([a-zA-Z0-9]{32,})"),
        Pattern.compile("(?i)(?:access[_-]?token|accesstoken)[\"'\\s]*[:=][\"'\\s]*([a-zA-Z0-9._-]{32,})"),
        Pattern.compile("(?i)(?:aws[_-]?secret|aws[_-]?key)[\"'\\s]*[:=][\"'\\s]*([a-zA-Z0-9/+=]{40})"),
        Pattern.compile("(?i)(?:google[_-]?api[_-]?key|google_maps_key)[\"'\\s]*[:=][\"'\\s]*([a-zA-Z0-9_-]{39})"),
        Pattern.compile("(?i)(?:firebase[_-]?url|database[_-]?url)[\"'\\s]*[:=][\"'\\s]*(https?://[a-zA-Z0-9.-]+\\.firebaseio\\.com)"),
        Pattern.compile("(?i)(?:private[_-]?key|privatekey)[\"'\\s]*[:=][\"'\\s]*(-----BEGIN [A-Z ]+-----)"),
        Pattern.compile("(?i)sk_live_[a-zA-Z0-9]{24,}"),  // Stripe live key
        Pattern.compile("(?i)ghp_[a-zA-Z0-9]{36}"),       // GitHub PAT
        Pattern.compile("(?i)xox[baprs]-[a-zA-Z0-9-]{10,}"), // Slack token
    )

    // 硬编码密码检测正则
    private val passwordPatterns = listOf(
        Pattern.compile("(?i)(?:password|passwd|pwd)[\"'\\s]*[:=][\"'\\s]*([a-zA-Z0-9!@#\$%^&*]{6,})"),
    )

    // 追踪 SDK 包名
    private val trackingSdkPatterns = listOf(
        "com/google/android/gms/ads" to "Google AdMob",
        "com/facebook/ads" to "Facebook Ads",
        "com/applovin" to "AppLovin",
        "com/mintegral" to "Mintegral",
        "com/unity3d/ads" to "Unity Ads",
        "com/bytedance/sdk" to "ByteDance SDK",
        "com/tencent/bugly" to "Tencent Bugly",
        "cn/jpush" to "JPush",
        "com/umeng" to "Umeng Analytics",
        "com/google/firebase/analytics" to "Firebase Analytics",
        "com/google/android/gms/analytics" to "Google Analytics",
        "com/flurry" to "Flurry Analytics",
    )

    // 弱加密算法
    private val weakCryptoPatterns = listOf(
        "DES/" to "DES",
        "DESede" to "3DES",
        "RC4" to "RC4",
        "AES/ECB" to "AES-ECB",
    )

    /**
     * 扫描 APK 安全问题。
     */
    fun scan(apkPath: String): SecurityScanResult {
        val issues = mutableListOf<SecurityIssue>()
        val apkFile = ApkFile(apkPath)
        val apkInfo = apkFile.use { it.parse() }

        // 1. 清单安全检查
        scanManifest(apkInfo, issues)

        // 2. 签名安全检查
        scanSignatures(apkInfo, issues)

        // 3. 硬编码密钥检查 (扫描 DEX 中的字符串)
        scanHardcodedSecrets(apkPath, issues)

        // 4. 追踪 SDK 检查
        scanTrackingSdk(apkInfo, issues)

        // 5. 弱加密检查
        scanWeakCrypto(apkPath, issues)

        // 计算统计
        val critical = issues.count { it.severity == Severity.CRITICAL }
        val high = issues.count { it.severity == Severity.HIGH }
        val medium = issues.count { it.severity == Severity.MEDIUM }
        val low = issues.count { it.severity == Severity.LOW }
        val info = issues.count { it.severity == Severity.INFO }

        // 统一计算安全评分 — 使用 RiskScoreCalculator（唯一来源）
        val score = com.appdex.data.session.RiskScoreCalculator.calculate(critical, high, medium, low)

        return SecurityScanResult(
            apkName = File(apkPath).name,
            apkPath = apkPath,
            issues = issues.sortedBy { it.severity.ordinal },
            criticalCount = critical,
            highCount = high,
            mediumCount = medium,
            lowCount = low,
            infoCount = info,
            securityScore = score,
        )
    }

    private fun scanManifest(apkInfo: com.appdex.apk.ApkInfo, issues: MutableList<SecurityIssue>) {
        val manifest = apkInfo.manifest

        // debuggable
        // (从 XML 中检测，这里通过 entries 间接判断)
        // 实际应从解析的 manifest XML 中检测 debuggable 属性
        // 这里通过签名和权限来检测

        // 危险权限
        manifest.permissions.forEach { perm ->
            if (perm in dangerousPermissions) {
                issues.add(SecurityIssue(
                    type = SecurityIssueType.DANGEROUS_PERMISSION,
                    severity = Severity.MEDIUM,
                    title = "危险权限: $perm",
                    description = "此权限可能涉及用户隐私或设备安全",
                    location = "AndroidManifest.xml",
                    recommendation = "评估是否真正需要此权限，考虑使用替代方案"
                ))
            }
        }

        // 导出组件检查
        manifest.activities.forEach { activity ->
            issues.add(SecurityIssue(
                type = SecurityIssueType.EXPORTED_COMPONENT,
                severity = Severity.LOW,
                title = "Activity: $activity",
                description = "检查是否设置了 android:exported 且未受权限保护",
                location = "AndroidManifest.xml",
                recommendation = "确保导出组件有适当的权限保护"
            ))
        }
    }

    private fun scanSignatures(apkInfo: com.appdex.apk.ApkInfo, issues: MutableList<SecurityIssue>) {
        val sigs = apkInfo.signatures

        if (sigs.isEmpty()) {
            issues.add(SecurityIssue(
                type = SecurityIssueType.NO_SIGNATURE,
                severity = Severity.CRITICAL,
                title = "未签名 APK",
                description = "此 APK 没有任何签名，无法验证来源",
                recommendation = "使用 V2/V3 签名方案对 APK 进行签名"
            ))
            return
        }

        val hasV2 = sigs.any { it.version >= 2 }
        if (!hasV2) {
            issues.add(SecurityIssue(
                type = SecurityIssueType.V1_ONLY_SIGNATURE,
                severity = Severity.HIGH,
                title = "仅使用 V1 签名",
                description = "V1 (JAR) 签名容易受到 Janus 漏洞攻击",
                recommendation = "升级到 V2/V3 签名方案"
            ))
        }

        // 检查弱签名算法
        sigs.forEach { sig ->
            if (sig.algorithm.contains("MD5", ignoreCase = true) ||
                sig.algorithm.contains("SHA1", ignoreCase = true)) {
                issues.add(SecurityIssue(
                    type = SecurityIssueType.WEAK_SIGNATURE_ALGORITHM,
                    severity = Severity.MEDIUM,
                    title = "弱签名算法: ${sig.algorithm}",
                    description = "使用弱哈希算法的签名可能被伪造",
                    recommendation = "使用 SHA256withRSA 或更强的签名算法"
                ))
            }
        }
    }

    private fun scanHardcodedSecrets(apkPath: String, issues: MutableList<SecurityIssue>) {
        try {
            ZipFile(apkPath).use { zip ->
                val dexEntries = zip.entries().asSequence()
                    .filter { it.name.endsWith(".dex") }
                    .toList()

                for (dexEntry in dexEntries) {
                    val dexBytes = zip.getInputStream(dexEntry).use { it.readBytes() }
                    // 将 DEX 中的字符串提取为 ASCII 字符串
                    val strings = extractStringsFromDex(dexBytes)

                    for (str in strings) {
                        // API Key 检测
                        for (pattern in apiKeyPatterns) {
                            val matcher = pattern.matcher(str)
                            if (matcher.find()) {
                                issues.add(SecurityIssue(
                                    type = SecurityIssueType.HARDCODED_API_KEY,
                                    severity = Severity.CRITICAL,
                                    title = "硬编码 API 密钥",
                                    description = "在 DEX 中发现疑似 API 密钥: ${maskSecret(matcher.group())}",
                                    location = dexEntry.name,
                                    recommendation = "将密钥移至服务端或使用 Android Keystore"
                                ))
                                break
                            }
                        }

                        // 密码检测
                        for (pattern in passwordPatterns) {
                            val matcher = pattern.matcher(str)
                            if (matcher.find()) {
                                issues.add(SecurityIssue(
                                    type = SecurityIssueType.HARDCODED_PASSWORD,
                                    severity = Severity.HIGH,
                                    title = "硬编码密码",
                                    description = "在 DEX 中发现疑似密码: ${maskSecret(matcher.group(1))}",
                                    location = dexEntry.name,
                                    recommendation = "不要在代码中硬编码密码"
                                ))
                                break
                            }
                        }

                        // 弱随机数
                        if (str.contains("java/util/Random") && !str.contains("SecureRandom")) {
                            // 只报告一次
                            if (issues.none { it.type == SecurityIssueType.INSECURE_RANDOM && it.location == dexEntry.name }) {
                                issues.add(SecurityIssue(
                                    type = SecurityIssueType.INSECURE_RANDOM,
                                    severity = Severity.LOW,
                                    title = "使用不安全的随机数生成器",
                                    description = "发现 java.util.Random，可能不适用于安全场景",
                                    location = dexEntry.name,
                                    recommendation = "安全场景请使用 SecureRandom"
                                ))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { Log.w("AppX", "Suppressed exception", e) }
    }

    private fun scanTrackingSdk(apkInfo: com.appdex.apk.ApkInfo, issues: MutableList<SecurityIssue>) {
        val entries = apkInfo.entries.map { it.name }

        for ((pattern, name) in trackingSdkPatterns) {
            if (entries.any { it.contains(pattern) }) {
                issues.add(SecurityIssue(
                    type = SecurityIssueType.TRACKING_SDK,
                    severity = Severity.INFO,
                    title = "检测到 $name",
                    description = "此 APK 集成了 $name SDK",
                    location = pattern,
                    recommendation = "了解此 SDK 的数据收集政策"
                ))
            }
        }
    }

    private fun scanWeakCrypto(apkPath: String, issues: MutableList<SecurityIssue>) {
        try {
            ZipFile(apkPath).use { zip ->
                val dexEntries = zip.entries().asSequence()
                    .filter { it.name.endsWith(".dex") }
                    .toList()

                for (dexEntry in dexEntries) {
                    val dexBytes = zip.getInputStream(dexEntry).use { it.readBytes() }
                    val strings = extractStringsFromDex(dexBytes)

                    for (str in strings) {
                        for ((pattern, name) in weakCryptoPatterns) {
                            if (str.contains(pattern)) {
                                if (issues.none { it.type == SecurityIssueType.WEAK_CRYPTO && it.location == dexEntry.name }) {
                                    issues.add(SecurityIssue(
                                        type = SecurityIssueType.WEAK_CRYPTO,
                                        severity = Severity.MEDIUM,
                                        title = "弱加密算法: $name",
                                        description = "检测到使用 $name 加密算法，可能不安全",
                                        location = dexEntry.name,
                                        recommendation = "使用 AES-GCM 或 ChaCha20-Poly1305"
                                    ))
                                }
                                break
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { Log.w("AppX", "Suppressed exception", e) }
    }

    /**
     * 从 DEX 文件中提取字符串 (简化版：提取 MUTF-8 字符串)。
     */
    private fun extractStringsFromDex(dexBytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        try {
            // DEX header: string_ids_size at offset 0x38, string_ids_off at 0x3C
            if (dexBytes.size < 0x40) return strings

            val stringIdsSize = readUInt(dexBytes, 0x38)
            val stringIdsOff = readUInt(dexBytes, 0x3C)

            if (stringIdsSize == 0L || stringIdsOff == 0L) return strings
            if (stringIdsOff + stringIdsSize * 4 > dexBytes.size) return strings

            for (i in 0 until minOf(stringIdsSize.toInt(), 5000)) {
                val stringDataOff = readUInt(dexBytes, stringIdsOff + i * 4)
                if (stringDataOff >= dexBytes.size) continue

                // 读取 ULEB128 长度
                var pos = stringDataOff.toInt()
                var strLen = 0
                var shift = 0
                while (pos < dexBytes.size) {
                    val b = dexBytes[pos].toInt() and 0xFF
                    strLen = strLen or ((b and 0x7F) shl shift)
                    pos++
                    if ((b and 0x80) == 0) break
                    shift += 7
                }

                // 读取 MUTF-8 字符串
                val end = minOf(pos + strLen, dexBytes.size)
                val str = String(dexBytes, pos, end - pos, Charsets.UTF_8).trim()
                if (str.length >= 4) {
                    strings.add(str)
                }
            }
        } catch (e: Exception) { Log.w("AppX", "Suppressed exception", e) }

        return strings
    }

    private fun readUInt(bytes: ByteArray, offset: Long): Long {
        val off = offset.toInt()
        if (off + 4 > bytes.size) return 0
        return ((bytes[off].toLong() and 0xFF) or
                ((bytes[off + 1].toLong() and 0xFF) shl 8) or
                ((bytes[off + 2].toLong() and 0xFF) shl 16) or
                ((bytes[off + 3].toLong() and 0xFF) shl 24))
    }

    private fun maskSecret(secret: String): String {
        if (secret.length <= 8) return "***"
        return secret.take(4) + "..." + secret.takeLast(4)
    }
}
