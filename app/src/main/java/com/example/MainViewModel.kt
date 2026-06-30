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

        val videoFile = File(context.filesDir, "walhero_active_video.mp4")
        if (!videoFile.exists()) {
            // Check for legacy target files and migrate to active_video
            val homeFile = File(context.filesDir, "walhero_home_video.mp4")
            val lockFile = File(context.filesDir, "walhero_lock_video.mp4")
            if (homeFile.exists() && homeFile.length() > 0) {
                homeFile.renameTo(videoFile)
                if (lockFile.exists()) lockFile.delete()
            } else if (lockFile.exists() && lockFile.length() > 0) {
                lockFile.renameTo(videoFile)
            }
        }

        if (videoFile.exists() && videoFile.length() > 0) {
            val savedName = prefs.getString("pref_video_name", "My Video Wallpaper") ?: "My Video Wallpaper"
            val sizeStr = getFileSizeString(videoFile.length())
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

    fun handleSelectedVideo(context: Context, uri: Uri) {
        _videoState.value = VideoState.Copying
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val originalName = getFileName(context, uri) ?: "Selected Video"
                val destFile = File(context.filesDir, "walhero_active_video.mp4")

                // Copy chosen video directly to walhero_active_video.mp4
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                // Clean up any old target files
                val homeFile = File(context.filesDir, "walhero_home_video.mp4")
                if (homeFile.exists()) homeFile.delete()
                val lockFile = File(context.filesDir, "walhero_lock_video.mp4")
                if (lockFile.exists()) lockFile.delete()

                if (destFile.exists() && destFile.length() > 0) {
                    val sizeStr = getFileSizeString(destFile.length())
                    
                    // Save info to SharedPreferences
                    val prefs = context.getSharedPreferences("walhero_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("pref_video_name", originalName)
                        .putLong("pref_video_updated", System.currentTimeMillis())
                        .apply()

                    _videoState.value = VideoState.Ready(originalName, sizeStr)
                } else {
                    _videoState.value = VideoState.Error("Failed to save video file")
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
