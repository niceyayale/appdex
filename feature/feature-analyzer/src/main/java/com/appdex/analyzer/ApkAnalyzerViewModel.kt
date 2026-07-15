package com.appdex.analyzer

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdex.apk.ApkFile
import com.appdex.apk.ApkInfo
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
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<ApkAnalyzerIntent, ApkAnalyzerState, ApkAnalyzerEffect>(
    initialState = ApkAnalyzerState(),
    savedStateHandle = savedStateHandle
) {
    override fun handleIntent(intent: ApkAnalyzerIntent) {
        when (intent) {
            is ApkAnalyzerIntent.OpenApk -> openApk(intent.uri)
            is ApkAnalyzerIntent.OpenApkPath -> openApk(Uri.fromFile(java.io.File(intent.path)))
            is ApkAnalyzerIntent.Clear -> {
                // Clean up temp file
                currentState.apkFilePath?.let { path ->
                    try { File(path).delete() } catch (e: Exception) { Log.w("AppDex", "Suppressed exception", e) }
                }
                update { it.copy(apkInfo = null, appIcon = null, error = null, apkFilePath = null) }
                savedStateHandle?.remove<String?>("apk_file_path")
            }
        }
    }

    private fun openApk(uri: Uri) {
        update { it.copy(isLoading = true, error = null, apkInfo = null, appIcon = null, apkFilePath = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Copy APK to cache for parsing — keep it for DEX browsing
                val tempFile = File(context.cacheDir, "temp_analysis.apk")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: run {
                    update { it.copy(isLoading = false, error = "Cannot open file") }
                    return@launch
                }

                // Verify it's a valid ZIP/APK
                if (!tempFile.exists() || tempFile.length() < 4) {
                    update { it.copy(isLoading = false, error = "文件无效或太小") }
                    return@launch
                }

                val apkFile = ApkFile(tempFile.absolutePath)
                val info = apkFile.use { it.parse() }

                // If manifest is empty (binary XML not yet implemented), try package manager
                val enrichedInfo = if (info.manifest.packageName.isEmpty()) {
                    enrichWithPackageManager(info, tempFile)
                } else {
                    info
                }

                // Load app icon
                val icon = loadAppIcon(tempFile)

                update { it.copy(apkInfo = enrichedInfo, appIcon = icon, isLoading = false, apkFilePath = tempFile.absolutePath) }
                saveState("apk_file_path", tempFile.absolutePath)
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                update { it.copy(isLoading = false, error = e.message ?: "Failed to analyze APK") }
            }
        }
    }

    private fun loadAppIcon(apkFile: File): Bitmap? {
        return try {
            val pm = context.packageManager
            val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            pkgInfo?.applicationInfo?.let { appInfo ->
                appInfo.sourceDir = apkFile.absolutePath
                appInfo.publicSourceDir = apkFile.absolutePath
                val drawable = appInfo.loadIcon(pm)
                drawableToBitmap(drawable)
            }
        } catch (e: Exception) {
            Log.w("AppDex", "Suppressed exception", e)
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun enrichWithPackageManager(info: ApkInfo, apkFile: File): ApkInfo {
        return try {
            val pm = context.packageManager
            val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_PERMISSIONS)
            val appInfo = pkgInfo?.applicationInfo
            appInfo?.sourceDir = apkFile.absolutePath
            appInfo?.publicSourceDir = apkFile.absolutePath
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
            Log.w("AppDex", "Suppressed exception", e)
            info
        }
    }
}

sealed interface ApkAnalyzerEffect : MviEffect {
    data class Error(val message: String) : ApkAnalyzerEffect
}
