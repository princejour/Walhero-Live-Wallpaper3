package com.example

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface VideoState {
    object Idle : VideoState
    object Copying : VideoState
    data class Ready(val fileName: String, val sizeStr: String) : VideoState
    data class Error(val message: String) : VideoState
}

class MainViewModel : ViewModel() {

    private val _videoState = MutableStateFlow<VideoState>(VideoState.Idle)
    val videoState: StateFlow<VideoState> = _videoState.asStateFlow()

    private val _soundEnabled = MutableStateFlow(false)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    fun initPrefs(context: Context) {
        val prefs = context.getSharedPreferences("walhero_prefs", Context.MODE_PRIVATE)
        _soundEnabled.value = prefs.getBoolean("pref_sound_enabled", false)

        val homeFile = File(context.filesDir, "walhero_home_video.mp4")
        val lockFile = File(context.filesDir, "walhero_lock_video.mp4")
        val legacyFile = File(context.filesDir, "walhero_active_video.mp4")

        val exists = homeFile.exists() || lockFile.exists() || legacyFile.exists()
        if (exists) {
            val savedName = prefs.getString("pref_video_name", "My Video Wallpaper") ?: "My Video Wallpaper"
            val size = when {
                homeFile.exists() -> homeFile.length()
                lockFile.exists() -> lockFile.length()
                else -> legacyFile.length()
            }
            val sizeStr = getFileSizeString(size)
            _videoState.value = VideoState.Ready(savedName, sizeStr)
        } else {
            _videoState.value = VideoState.Idle
        }
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        _soundEnabled.value = enabled
        val prefs = context.getSharedPreferences("walhero_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("pref_sound_enabled", enabled).apply()
    }

    fun handleSelectedVideo(context: Context, uri: Uri, target: String = "both") {
        _videoState.value = VideoState.Copying
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val originalName = getFileName(context, uri) ?: "Selected Video"
                
                val targets = when (target) {
                    "home" -> listOf("walhero_home_video.mp4")
                    "lock" -> listOf("walhero_lock_video.mp4")
                    else -> listOf("walhero_home_video.mp4", "walhero_lock_video.mp4")
                }

                // Copy to a temporary file in cache first to prevent any lockups during concurrent copying
                val tempFile = File(context.cacheDir, "temp_selected_video.mp4")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    // Copy to each target file
                    for (targetName in targets) {
                        val destFile = File(context.filesDir, targetName)
                        tempFile.copyTo(destFile, overwrite = true)
                    }
                    
                    // If they chose specific targets, clean up the other ones or legacy files as appropriate
                    if (target == "both") {
                        val legacyFile = File(context.filesDir, "walhero_active_video.mp4")
                        if (legacyFile.exists()) {
                            legacyFile.delete()
                        }
                    } else if (target == "home") {
                        // If they set specifically home, we can delete the legacy general file
                        val legacyFile = File(context.filesDir, "walhero_active_video.mp4")
                        if (legacyFile.exists()) {
                            legacyFile.delete()
                        }
                    } else if (target == "lock") {
                        val legacyFile = File(context.filesDir, "walhero_active_video.mp4")
                        if (legacyFile.exists()) {
                            legacyFile.delete()
                        }
                    }

                    tempFile.delete()

                    val sizeStr = getFileSizeString(File(context.filesDir, targets[0]).length())
                    
                    // Save info to SharedPreferences
                    val prefs = context.getSharedPreferences("walhero_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("pref_video_name", originalName)
                        .putLong("pref_video_updated", System.currentTimeMillis())
                        .apply()

                    _videoState.value = VideoState.Ready(originalName, sizeStr)
                } else {
                    _videoState.value = VideoState.Error("Failed to save video file: file is empty")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _videoState.value = VideoState.Error(e.localizedMessage ?: "Unknown error while copying video")
            }
        }
    }

    fun clearVideo(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val homeFile = File(context.filesDir, "walhero_home_video.mp4")
            if (homeFile.exists()) homeFile.delete()
            val lockFile = File(context.filesDir, "walhero_lock_video.mp4")
            if (lockFile.exists()) lockFile.delete()
            val legacyFile = File(context.filesDir, "walhero_active_video.mp4")
            if (legacyFile.exists()) legacyFile.delete()

            val prefs = context.getSharedPreferences("walhero_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .remove("pref_video_name")
                .putLong("pref_video_updated", System.currentTimeMillis())
                .apply()

            _videoState.value = VideoState.Idle
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun getFileSizeString(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
