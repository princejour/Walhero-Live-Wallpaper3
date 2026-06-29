package com.example

import android.app.WallpaperManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.util.Log
import java.io.File

class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine() {
        private var mediaPlayer: MediaPlayer? = null
        private val prefs by lazy {
            getSharedPreferences("walhero_prefs", Context.MODE_PRIVATE)
        }

        private val prefListener = SharedPreferencesListener()

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            prefs.registerOnSharedPreferenceChangeListener(prefListener)
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
            releasePlayer()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                playVideo()
            } else {
                mediaPlayer?.pause()
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            playVideo()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
        }

        private fun getEngineWallpaperFlags(): Int {
            if (Build.VERSION.SDK_INT >= 34) { // Android 14+
                try {
                    return getWallpaperFlags()
                } catch (e: Throwable) {
                    // Fallback to reflection below
                }
            }
            // Fallback to reflection on private mWhich field (available since Android 8.0 / API 26)
            try {
                val field = WallpaperService.Engine::class.java.getDeclaredField("mWhich")
                field.isAccessible = true
                return field.get(this) as Int
            } catch (e: Throwable) {
                // If reflection fails, return FLAG_SYSTEM as standard default
            }
            return WallpaperManager.FLAG_SYSTEM
        }

        private fun playVideo() {
            val flags = getEngineWallpaperFlags()
            val isLockScreen = (flags and WallpaperManager.FLAG_LOCK) != 0

            val videoFile = if (isLockScreen) {
                val lockFile = File(filesDir, "walhero_lock_video.mp4")
                if (lockFile.exists()) lockFile else File(filesDir, "walhero_active_video.mp4")
            } else {
                val homeFile = File(filesDir, "walhero_home_video.mp4")
                if (homeFile.exists()) homeFile else File(filesDir, "walhero_active_video.mp4")
            }

            if (!videoFile.exists()) {
                Log.w("VideoWallpaperService", "No active video file exists for targets (isLockScreen=$isLockScreen).")
                return
            }

            if (mediaPlayer == null) {
                try {
                    val holder = surfaceHolder ?: return
                    mediaPlayer = MediaPlayer().apply {
                        setSurface(holder.surface)
                        setDataSource(videoFile.absolutePath)
                        isLooping = true
                        
                        // Set volume based on preference
                        val soundEnabled = prefs.getBoolean("pref_sound_enabled", false)
                        val volume = if (soundEnabled) 1.0f else 0.0f
                        setVolume(volume, volume)

                        setOnVideoSizeChangedListener { mp, videoWidth, videoHeight ->
                            if (videoWidth > 0 && videoHeight > 0) {
                                adjustSurfaceSize(holder, videoWidth, videoHeight)
                            }
                        }

                        prepare()
                        start()
                    }
                } catch (e: Exception) {
                    Log.e("VideoWallpaperService", "Error preparing MediaPlayer", e)
                    releasePlayer()
                }
            } else {
                mediaPlayer?.start()
            }
        }

        private fun adjustSurfaceSize(holder: SurfaceHolder, videoWidth: Int, videoHeight: Int) {
            try {
                val surfaceFrame = holder.surfaceFrame
                val screenWidth = surfaceFrame.width()
                val screenHeight = surfaceFrame.height()
                if (screenWidth <= 0 || screenHeight <= 0) return

                val videoRatio = videoWidth.toFloat() / videoHeight
                val screenRatio = screenWidth.toFloat() / screenHeight

                var targetWidth = screenWidth
                var targetHeight = screenHeight

                if (videoRatio > screenRatio) {
                    // Video is wider than screen: match height, crop sides
                    targetWidth = (screenHeight * videoRatio).toInt()
                } else {
                    // Video is taller than screen: match width, crop top/bottom
                    targetHeight = (screenWidth / videoRatio).toInt()
                }

                holder.setFixedSize(targetWidth, targetHeight)
                Log.d("VideoWallpaperService", "Adjusted surface size: ${targetWidth}x${targetHeight} for video ${videoWidth}x${videoHeight}")
            } catch (e: Exception) {
                Log.e("VideoWallpaperService", "Error adjusting surface size", e)
            }
        }

        private fun releasePlayer() {
            try {
                mediaPlayer?.let {
                    it.stop()
                    it.release()
                }
            } catch (e: Exception) {
                Log.e("VideoWallpaperService", "Error releasing MediaPlayer", e)
            } finally {
                mediaPlayer = null
            }
        }

        private fun updateVolume() {
            val soundEnabled = prefs.getBoolean("pref_sound_enabled", false)
            val volume = if (soundEnabled) 1.0f else 0.0f
            mediaPlayer?.setVolume(volume, volume)
            Log.d("VideoWallpaperService", "Volume updated to: $volume")
        }

        inner class SharedPreferencesListener : android.content.SharedPreferences.OnSharedPreferenceChangeListener {
            override fun onSharedPreferenceChanged(
                sharedPreferences: android.content.SharedPreferences?,
                key: String?
            ) {
                if (key == "pref_sound_enabled") {
                    updateVolume()
                } else if (key == "pref_video_updated") {
                    // Force re-creation of player to load new video source
                    releasePlayer()
                    if (isVisible) {
                        playVideo()
                    }
                }
            }
        }
    }
}
