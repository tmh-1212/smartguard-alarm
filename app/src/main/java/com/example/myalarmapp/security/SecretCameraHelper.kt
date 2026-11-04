package com.example.myalarmapp.security

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Helper class to silently capture real front-camera photos using native Android Camera2 API.
 */
class SecretCameraHelper(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun captureFrontCameraPhoto(): String? = suspendCoroutine { continuation ->
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (manager == null) {
            continuation.resume(null)
            return@suspendCoroutine
        }

        try {
            var frontCameraId: String? = null
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = cameraId
                    break
                }
            }

            if (frontCameraId == null) {
                continuation.resume(null)
                return@suspendCoroutine
            }

            val photoFile = File(
                context.filesDir,
                "intruder_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())}.jpg"
            )

            val handler = Handler(Looper.getMainLooper())
            val imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)

            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    FileOutputStream(photoFile).use { fos ->
                        fos.write(bytes)
                    }
                    Log.d("SecretCameraHelper", "Real front camera photo captured successfully at ${photoFile.absolutePath}")
                    continuation.resume(photoFile.absolutePath)
                } catch (e: Exception) {
                    Log.e("SecretCameraHelper", "Error saving captured image", e)
                    continuation.resume(null)
                } finally {
                    image.close()
                    imageReader.close()
                }
            }, handler)

            manager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    try {
                        val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        captureBuilder.addTarget(imageReader.surface)

                        camera.createCaptureSession(listOf(imageReader.surface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                try {
                                    session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {}, handler)
                                } catch (e: Exception) {
                                    logCameraError("Failed to capture request", e)
                                    camera.close()
                                    continuation.resume(null)
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                camera.close()
                                continuation.resume(null)
                            }
                        }, handler)
                    } catch (e: Exception) {
                        camera.close()
                        continuation.resume(null)
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    continuation.resume(null)
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    continuation.resume(null)
                }
            }, handler)

        } catch (e: Exception) {
            Log.e("SecretCameraHelper", "Camera2 open error", e)
            continuation.resume(null)
        }
    }

    private fun logCameraError(message: String, e: Exception) {
        Log.e("SecretCameraHelper", message, e)
    }
}
