/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kaist.dd.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.kaist.dd.AlertMediaHelper
import com.kaist.dd.DatabaseHelper
import com.kaist.dd.FaceLandmarkerHelper
import com.kaist.dd.MainViewModel
import com.kaist.dd.R
import com.kaist.dd.databinding.FragmentCameraBinding
import com.kaist.dd.judgement.DrowsinessComputer
import com.kaist.dd.judgement.FaceDetection
import com.kaist.dd.judgement.Prediction
import com.kaist.dd.judgement.DrowsinessStatus
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment(),
    FaceLandmarkerHelper.LandmarkerListener,
    FaceDetection.FaceDetectListener,
    DrowsinessStatus.DrowsinessStatusListener {

    // 추가 variable
    var avgEAR: Double = 0.0
    private var earCounter: Int = 0
    private var isShowFaceUndetectedAlert: Boolean = false
    private var cameraStartTime: Long = 0

    private lateinit var prediction: Prediction
    private lateinit var faceDetection: FaceDetection
    private lateinit var drowsinessStatus: DrowsinessStatus

    companion object {
        private const val TAG = "Face Landmarker"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private lateinit var alertMediaHelper: AlertMediaHelper
    private lateinit var databaseHelper: DatabaseHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService

    override fun onResume() {
        super.onResume()
        // Start the FaceLandmarkerHelper again when users come back
        // to the foreground.
        backgroundExecutor.execute {
            if (faceLandmarkerHelper.isClose()) {
                faceLandmarkerHelper.setupFaceLandmarker()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if(this::faceLandmarkerHelper.isInitialized) {
            viewModel.setMaxFaces(faceLandmarkerHelper.maxNumFaces)
            viewModel.setMinFaceDetectionConfidence(faceLandmarkerHelper.minFaceDetectionConfidence)
            viewModel.setMinFaceTrackingConfidence(faceLandmarkerHelper.minFaceTrackingConfidence)
            viewModel.setMinFacePresenceConfidence(faceLandmarkerHelper.minFacePresenceConfidence)
            viewModel.setDelegate(faceLandmarkerHelper.currentDelegate)

            // Close the FaceLandmarkerHelper and release resources
            backgroundExecutor.execute { faceLandmarkerHelper.clearFaceLandmarker() }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        cameraStartTime = System.currentTimeMillis()

        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)

        _fragmentCameraBinding!!.fabCamera.setOnClickListener {
            cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUseCases()
        }

        _fragmentCameraBinding!!.fabPredict.setOnClickListener {
            var currentTime = System.currentTimeMillis()

            if (currentTime - cameraStartTime < 30 * 1000) {
                createNotEnoughLoggingTimeAlert()
            } else {
                var ears: ArrayList<Double> = databaseHelper.getLastRangeEars()
                var probability = this.prediction.predict(ears)
                createDrowsyPredictionAlert(probability)
            }
        }

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        // Create the FaceLandmarkerHelper that will handle the inference
        backgroundExecutor.execute {
            faceLandmarkerHelper = FaceLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minFaceDetectionConfidence = viewModel.currentMinFaceDetectionConfidence,
                minFaceTrackingConfidence = viewModel.currentMinFaceTrackingConfidence,
                minFacePresenceConfidence = viewModel.currentMinFacePresenceConfidence,
                maxNumFaces = viewModel.currentMaxFaces,
                currentDelegate = viewModel.currentDelegate,
                faceLandmarkerHelperListener = this
            )
            alertMediaHelper = AlertMediaHelper(
                context = requireContext()
            )
            databaseHelper = DatabaseHelper(
                context = requireContext()
            )
            prediction = Prediction(
                context = requireContext(),
                "model.ptl"
            )
            faceDetection = FaceDetection(
                faceDetectListener = this
            )
            drowsinessStatus = DrowsinessStatus(
                drowsinessStatusListener = this
            )
        }
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        detectFace(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectFace(imageProxy: ImageProxy) {
        faceLandmarkerHelper.detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after face have been detected. Extracts original
    // image height/width to scale and place the landmarks properly through
    // OverlayView
    override fun onResults(
        resultBundle: FaceLandmarkerHelper.ResultBundle
    ) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {
                // Pass necessary information to OverlayView for drawing on the canvas
                fragmentCameraBinding.overlay.setResults(
                    resultBundle.result,
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                // Force a redraw
                fragmentCameraBinding.overlay.invalidate()

                // Average EAR value of left and right eyes
                avgEAR = resultBundle.avgEAR

                // Update and determine current drowsy driving status with avgEAR value
                drowsinessStatus.updateStatus(avgEAR)

                // Update status display on screen
                fragmentCameraBinding.statusTextView.text = drowsinessStatus.getStatusText()

                // update face detection status
                faceDetection.setDetectedFace()

                // add ear value log to database
                earCounter++
                if (earCounter == 15) {
                    earCounter = 0
                    databaseHelper.addEarLog(avgEAR, drowsinessStatus.getStatus().value + 1)
                }
            }
        }
    }

    override fun onEmpty() {
        fragmentCameraBinding.overlay.clear()

        // update face detection status
        faceDetection.setNotDetectedFace()
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createDrowsyPredictionAlert(probability: Double) {
        // 최근 30sec 구간 ear 기준으로 졸음운전 확률을 Alert Dialog 로 사용자에게 알림 (AI REQ)
        val builder:AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val alertMessage = when {
            probability < 0.3 -> R.string.drowsy_prediction_message_alert_1
            probability < 0.5 -> R.string.drowsy_prediction_message_alert_2
            else -> R.string.drowsy_prediction_message_alert_3
        }
        val fullMessage = getString(R.string.drowsy_prediction_message_first) +
                "\n\n%3.0f%%".format(probability*100) + "\n\n" + getString(alertMessage)

        builder.setTitle(R.string.drowsy_prediction_title)
        builder.setMessage(fullMessage)
        builder.setNeutralButton(R.string.drowsy_prediction_button, DialogInterface.OnClickListener { dialog, which ->
            dialog.dismiss()
        })
        builder.setOnDismissListener {
        }
        builder.show()
    }

    private fun createNotEnoughLoggingTimeAlert() {
        val builder:AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.enough_time_title)
        builder.setMessage(R.string.enough_time_message)
        builder.setNeutralButton(R.string.enough_time_button, DialogInterface.OnClickListener { dialog, which ->
            dialog.dismiss()
        })
        builder.setOnDismissListener {
        }
        builder.show()
    }

    private fun playRingtone() {
        val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(requireContext(), notification)
        ringtone.play()
    }

    override fun showFaceNotDetectedAlert() {
        activity?.runOnUiThread {
            if (!isShowFaceUndetectedAlert) {
                // Face 미 인식에 대한 Time-out 발생 시, Alert Dialog 로 사용자에게 알림 (REQ-DD-004)
                val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                builder.setTitle(R.string.face_undetected_title)
                builder.setMessage(R.string.face_undetected_message)
                builder.setNeutralButton(
                    R.string.face_undetected_button,
                    DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                    })
                builder.setOnDismissListener {
                    isShowFaceUndetectedAlert = false
                    faceDetection.setActiveFaceDetect(true)
                }
                builder.show()
                playRingtone()

                isShowFaceUndetectedAlert = true
                faceDetection.setActiveFaceDetect(false)
            }
        }
    }

    override fun updateMoreDangerousStatus(status: DrowsinessComputer.Status) {
        alertMediaHelper.playMedia(status.value)
        databaseHelper.addAlertLog(status.value + 1, cameraStartTime)
    }
}
