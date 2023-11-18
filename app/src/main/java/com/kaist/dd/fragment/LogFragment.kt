/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kaist.dd.fragment

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.kaist.dd.FaceLandmarkerHelper
import com.kaist.dd.MainViewModel
import com.kaist.dd.databinding.FragmentLogBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class LogFragment : Fragment(), FaceLandmarkerHelper.LandmarkerListener {

    enum class MediaType {
        IMAGE,
        VIDEO,
        UNKNOWN
    }

    private var _fragmentLogBinding: FragmentLogBinding? = null
    private val fragmentLogBinding
        get() = _fragmentLogBinding!!
    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ScheduledExecutorService

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            // Handle the returned Uri
            uri?.let { mediaUri ->
                when (val mediaType = loadMediaType(mediaUri)) {
                    MediaType.IMAGE -> runDetectionOnImage(mediaUri)
                    MediaType.VIDEO -> runDetectionOnVideo(mediaUri)
                    MediaType.UNKNOWN -> {
                        updateDisplayView(mediaType)
                        Toast.makeText(
                            requireContext(),
                            "Unsupported data type.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentLogBinding =
            FragmentLogBinding.inflate(inflater, container, false)

        return fragmentLogBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //fragmentLogBinding.fabGetContent.setOnClickListener {
        //    getContent.launch(arrayOf("image/*", "video/*"))
        //}
        /*
        with(fragmentLogBinding.recyclerviewResults) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = faceBlendshapesResultAdapter
        }

        initBottomSheetControls()
         */
    }

    override fun onPause() {
        fragmentLogBinding.overlay.clear()
        if (fragmentLogBinding.videoView.isPlaying) {
            fragmentLogBinding.videoView.stopPlayback()
        }
        fragmentLogBinding.videoView.visibility = View.GONE
        fragmentLogBinding.imageResult.visibility = View.GONE
        fragmentLogBinding.tvPlaceholder.visibility = View.VISIBLE

        /*
        activity?.runOnUiThread {
            faceBlendshapesResultAdapter.updateResults(null)
            faceBlendshapesResultAdapter.notifyDataSetChanged()
        }
         */
        super.onPause()
    }

    /*
    private fun initBottomSheetControls() {
        // init bottom sheet settings
        fragmentLogBinding.bottomSheetLayout.maxFacesValue.text =
            viewModel.currentMaxFaces.toString()
        fragmentLogBinding.bottomSheetLayout.detectionThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinFaceDetectionConfidence
            )
        fragmentLogBinding.bottomSheetLayout.trackingThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinFaceTrackingConfidence
            )
        fragmentLogBinding.bottomSheetLayout.presenceThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinFacePresenceConfidence
            )

        // When clicked, lower detection score threshold floor
        fragmentLogBinding.bottomSheetLayout.detectionThresholdMinus.setOnClickListener {
            if (viewModel.currentMinFaceDetectionConfidence >= 0.2) {
                viewModel.setMinFaceDetectionConfidence(viewModel.currentMinFaceDetectionConfidence - 0.1f)
                updateControlsUi()
            }
        }

        // When clicked, raise detection score threshold floor
        fragmentLogBinding.bottomSheetLayout.detectionThresholdPlus.setOnClickListener {
            if (viewModel.currentMinFaceDetectionConfidence <= 0.8) {
                viewModel.setMinFaceDetectionConfidence(viewModel.currentMinFaceDetectionConfidence + 0.1f)
                updateControlsUi()
            }
        }

        // When clicked, lower face tracking score threshold floor
        fragmentLogBinding.bottomSheetLayout.trackingThresholdMinus.setOnClickListener {
            if (viewModel.currentMinFaceTrackingConfidence >= 0.2) {
                viewModel.setMinFaceTrackingConfidence(
                    viewModel.currentMinFaceTrackingConfidence - 0.1f
                )
                updateControlsUi()
            }
        }

        // When clicked, raise face tracking score threshold floor
        fragmentLogBinding.bottomSheetLayout.trackingThresholdPlus.setOnClickListener {
            if (viewModel.currentMinFaceTrackingConfidence <= 0.8) {
                viewModel.setMinFaceTrackingConfidence(
                    viewModel.currentMinFaceTrackingConfidence + 0.1f
                )
                updateControlsUi()
            }
        }

        // When clicked, lower face presence score threshold floor
        fragmentLogBinding.bottomSheetLayout.presenceThresholdMinus.setOnClickListener {
            if (viewModel.currentMinFacePresenceConfidence >= 0.2) {
                viewModel.setMinFacePresenceConfidence(
                    viewModel.currentMinFacePresenceConfidence - 0.1f
                )
                updateControlsUi()
            }
        }

        // When clicked, raise face presence score threshold floor
        fragmentLogBinding.bottomSheetLayout.presenceThresholdPlus.setOnClickListener {
            if (viewModel.currentMinFacePresenceConfidence <= 0.8) {
                viewModel.setMinFacePresenceConfidence(
                    viewModel.currentMinFacePresenceConfidence + 0.1f
                )
                updateControlsUi()
            }
        }

        // When clicked, reduce the number of objects that can be detected at a time
        fragmentLogBinding.bottomSheetLayout.maxFacesMinus.setOnClickListener {
            if (viewModel.currentMaxFaces > 1) {
                viewModel.setMaxFaces(viewModel.currentMaxFaces - 1)
                updateControlsUi()
            }
        }

        // When clicked, increase the number of objects that can be detected at a time
        fragmentLogBinding.bottomSheetLayout.maxFacesPlus.setOnClickListener {
            if (viewModel.currentMaxFaces < 2) {
                viewModel.setMaxFaces(viewModel.currentMaxFaces + 1)
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU
        // GPU, and NNAPI
        fragmentLogBinding.bottomSheetLayout.spinnerDelegate.setSelection(
            viewModel.currentDelegate,
            false
        )
        fragmentLogBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    p2: Int,
                    p3: Long
                ) {

                    viewModel.setDelegate(p2)
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }
    }
     */

    // Update the values displayed in the bottom sheet. Reset detector.
    private fun updateControlsUi() {
        if (fragmentLogBinding.videoView.isPlaying) {
            fragmentLogBinding.videoView.stopPlayback()
        }
        fragmentLogBinding.videoView.visibility = View.GONE
        fragmentLogBinding.imageResult.visibility = View.GONE
        fragmentLogBinding.overlay.clear()
        /*
        fragmentLogBinding.bottomSheetLayout.maxFacesValue.text =
            viewModel.currentMaxFaces.toString()
        fragmentLogBinding.bottomSheetLayout.detectionThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinFaceDetectionConfidence
            )
        fragmentLogBinding.bottomSheetLayout.trackingThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinFaceTrackingConfidence
            )
        fragmentLogBinding.bottomSheetLayout.presenceThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinFacePresenceConfidence
            )
         */

        fragmentLogBinding.overlay.clear()
        fragmentLogBinding.tvPlaceholder.visibility = View.VISIBLE
    }

    // Load and display the image.
    private fun runDetectionOnImage(uri: Uri) {
        setUiEnabled(false)
        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        updateDisplayView(MediaType.IMAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(
                requireActivity().contentResolver,
                uri
            )
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(
                requireActivity().contentResolver,
                uri
            )
        }
            .copy(Bitmap.Config.ARGB_8888, true)
            ?.let { bitmap ->
                fragmentLogBinding.imageResult.setImageBitmap(bitmap)

                // Run face landmarker on the input image
                backgroundExecutor.execute {

                    faceLandmarkerHelper =
                        FaceLandmarkerHelper(
                            context = requireContext(),
                            runningMode = RunningMode.IMAGE,
                            minFaceDetectionConfidence = viewModel.currentMinFaceDetectionConfidence,
                            minFaceTrackingConfidence = viewModel.currentMinFaceTrackingConfidence,
                            minFacePresenceConfidence = viewModel.currentMinFacePresenceConfidence,
                            maxNumFaces = viewModel.currentMaxFaces,
                            currentDelegate = viewModel.currentDelegate
                        )

                    faceLandmarkerHelper.detectImage(bitmap)?.let { result ->
                        activity?.runOnUiThread {
                            /*
                            if (fragmentLogBinding.recyclerviewResults.scrollState != ViewPager2.SCROLL_STATE_DRAGGING) {
                                faceBlendshapesResultAdapter.updateResults(result.result)
                                faceBlendshapesResultAdapter.notifyDataSetChanged()
                            }
                             */
                            fragmentLogBinding.overlay.setResults(
                                result.result,
                                bitmap.height,
                                bitmap.width,
                                RunningMode.IMAGE
                            )

                            setUiEnabled(true)
                            /*
                            fragmentLogBinding.bottomSheetLayout.inferenceTimeVal.text =
                                String.format("%d ms", result.inferenceTime)

                             */
                        }
                    } ?: run { Log.e(TAG, "Error running face landmarker.") }

                    faceLandmarkerHelper.clearFaceLandmarker()
                }
            }
    }

    private fun runDetectionOnVideo(uri: Uri) {
        setUiEnabled(false)
        updateDisplayView(MediaType.VIDEO)

        with(fragmentLogBinding.videoView) {
            setVideoURI(uri)
            // mute the audio
            setOnPreparedListener { it.setVolume(0f, 0f) }
            requestFocus()
        }

        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        backgroundExecutor.execute {

            faceLandmarkerHelper =
                FaceLandmarkerHelper(
                    context = requireContext(),
                    runningMode = RunningMode.VIDEO,
                    minFaceDetectionConfidence = viewModel.currentMinFaceDetectionConfidence,
                    minFaceTrackingConfidence = viewModel.currentMinFaceTrackingConfidence,
                    minFacePresenceConfidence = viewModel.currentMinFacePresenceConfidence,
                    maxNumFaces = viewModel.currentMaxFaces,
                    currentDelegate = viewModel.currentDelegate
                )

            activity?.runOnUiThread {
                fragmentLogBinding.videoView.visibility = View.GONE
                fragmentLogBinding.progress.visibility = View.VISIBLE
            }

            faceLandmarkerHelper.detectVideoFile(uri, VIDEO_INTERVAL_MS)
                ?.let { resultBundle ->
                    activity?.runOnUiThread { displayVideoResult(resultBundle) }
                }
                ?: run { Log.e(TAG, "Error running face landmarker.") }

            faceLandmarkerHelper.clearFaceLandmarker()
        }
    }

    // Setup and display the video.
    private fun displayVideoResult(result: FaceLandmarkerHelper.VideoResultBundle) {

        fragmentLogBinding.videoView.visibility = View.VISIBLE
        fragmentLogBinding.progress.visibility = View.GONE

        fragmentLogBinding.videoView.start()
        val videoStartTimeMs = SystemClock.uptimeMillis()

        backgroundExecutor.scheduleAtFixedRate(
            {
                activity?.runOnUiThread {
                    val videoElapsedTimeMs =
                        SystemClock.uptimeMillis() - videoStartTimeMs
                    val resultIndex =
                        videoElapsedTimeMs.div(VIDEO_INTERVAL_MS).toInt()

                    if (resultIndex >= result.results.size || fragmentLogBinding.videoView.visibility == View.GONE) {
                        // The video playback has finished so we stop drawing bounding boxes
                        backgroundExecutor.shutdown()
                    } else {
                        fragmentLogBinding.overlay.setResults(
                            result.results[resultIndex],
                            result.inputImageHeight,
                            result.inputImageWidth,
                            RunningMode.VIDEO
                        )

                        /*
                        if (fragmentLogBinding.recyclerviewResults.scrollState != ViewPager2.SCROLL_STATE_DRAGGING) {
                            faceBlendshapesResultAdapter.updateResults(result.results[resultIndex])
                            faceBlendshapesResultAdapter.notifyDataSetChanged()
                        }
                         */

                        setUiEnabled(true)

                        /*
                        fragmentLogBinding.bottomSheetLayout.inferenceTimeVal.text =
                            String.format("%d ms", result.inferenceTime)

                         */
                    }
                }
            },
            0,
            VIDEO_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )
    }

    private fun updateDisplayView(mediaType: MediaType) {
        fragmentLogBinding.imageResult.visibility =
            if (mediaType == MediaType.IMAGE) View.VISIBLE else View.GONE
        fragmentLogBinding.videoView.visibility =
            if (mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE
        fragmentLogBinding.tvPlaceholder.visibility =
            if (mediaType == MediaType.UNKNOWN) View.VISIBLE else View.GONE
    }

    // Check the type of media that user selected.
    private fun loadMediaType(uri: Uri): MediaType {
        val mimeType = context?.contentResolver?.getType(uri)
        mimeType?.let {
            if (mimeType.startsWith("image")) return MediaType.IMAGE
            if (mimeType.startsWith("video")) return MediaType.VIDEO
        }

        return MediaType.UNKNOWN
    }

    private fun setUiEnabled(enabled: Boolean) {
        /*
        fragmentLogBinding.fabGetContent.isEnabled = enabled
        fragmentLogBinding.bottomSheetLayout.detectionThresholdMinus.isEnabled =
            enabled
        fragmentLogBinding.bottomSheetLayout.detectionThresholdPlus.isEnabled =
            enabled
        fragmentLogBinding.bottomSheetLayout.trackingThresholdMinus.isEnabled =
            enabled
        fragmentLogBinding.bottomSheetLayout.trackingThresholdPlus.isEnabled =
            enabled
        fragmentLogBinding.bottomSheetLayout.presenceThresholdMinus.isEnabled =
            enabled
        fragmentLogBinding.bottomSheetLayout.presenceThresholdPlus.isEnabled =
            enabled
        fragmentLogBinding.bottomSheetLayout.maxFacesPlus.isEnabled =
            enabled
        fragmentLogBinding.bottomSheetLayout.maxFacesMinus.isEnabled =
            enabled
        fragmentLogBinding.bottomSheetLayout.spinnerDelegate.isEnabled =
            enabled

         */
    }

    private fun classifyingError() {
        activity?.runOnUiThread {
            fragmentLogBinding.progress.visibility = View.GONE
            setUiEnabled(true)
            updateDisplayView(MediaType.UNKNOWN)
        }
    }

    override fun onError(error: String, errorCode: Int) {
        //classifyingError()
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            /*
            if (errorCode == FaceLandmarkerHelper.GPU_ERROR) {
                fragmentLogBinding.bottomSheetLayout.spinnerDelegate.setSelection(
                    FaceLandmarkerHelper.DELEGATE_CPU,
                    false
                )
            }
             */
        }
    }

    override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        // no-op
    }

    companion object {
        private const val TAG = "LogFragment"

        // Value used to get frames at specific intervals for inference (e.g. every 300ms)
        private const val VIDEO_INTERVAL_MS = 300L
    }

    override fun onEmpty() {
        fragmentLogBinding.overlay.clear()
        /*
        activity?.runOnUiThread {
            faceBlendshapesResultAdapter.updateResults(null)
            faceBlendshapesResultAdapter.notifyDataSetChanged()
        }
         */
    }

    override fun onUndetectedFace() {
        // no-op
    }
}
