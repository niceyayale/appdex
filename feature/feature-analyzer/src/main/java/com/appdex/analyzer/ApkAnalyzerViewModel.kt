package com.appdex.analyzer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdex.apk.ApkFile
import com.appdex.apk.ApkInfo
import com.appdex.apk.ApkManifest
import com.appdex.arch.BaseViewModel
import com.appdex.arch.MviEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ApkAnalyzerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : BaseViewModel<ApkAnalyzerIntent, ApkAnalyzerState, ApkAnalyzerEffect>(
    initialState = ApkAnalyzerState()
) {
    override fun handleIntent(intent: ApkAnalyzerIntent) {
        when (intent) {
            is ApkAnalyzerIntent.OpenApk -> openApk(intent.uri)
            is ApkAnalyzerIntent.Clear -> update { it.copy(apkInfo = null, error = null) }
        }
    }

    private fun openApk(uri: Uri) {
        update { it.copy(isLoading = true, error = null, apkInfo = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Copy APK to cache for parsing
                val tempFile = File(context.cacheDir, "temp_analysis.apk")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Cannot open file")

                val apkFile = ApkFile(tempFile.absolutePath)
                val info = apkFile.use { it.parse() }

                // If manifest is empty (binary XML not yet implemented), try package manager
                val enrichedInfo = if (info.manifest.packageName.isEmpty()) {
                    enrichWithPackageManager(info, tempFile)
                } else {
                    info
                }

                update { it.copy(apkInfo = enrichedInfo, isLoading = false) }
                tempFile.delete()
            } catch (e: Exception) {
                update { it.copy(isLoading = false, error = e.message ?: "Failed to analyze APK") }
            }
        }
    }

    private fun enrichWithPackageManager(info: ApkInfo, apkFile: File): ApkInfo {
        return try {
            val pm = context.packageManager
            val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            val appInfo = pkgInfo?.applicationInfo
            val manifest = info.manifest.copy(
                packageName = pkgInfo?.packageName ?: "",
                versionName = pkgInfo?.versionName ?: "",
                versionCode = pkgInfo?.longVersionCode ?: 0,
                minSdk = appInfo?.minSdkVersion ?: 0,
                targetSdk = appInfo?.targetSdkVersion ?: 0,
                permissions = pkgInfo?.requestedPermissions?.toList() ?: emptyList()
            )
            info.copy(manifest = manifest)
        } catch (e: Exception) {
            info
        }
    }
}

sealed interface ApkAnalyzerEffect : MviEffect {
    data class Error(val message: String) : ApkAnalyzerEffect
}
