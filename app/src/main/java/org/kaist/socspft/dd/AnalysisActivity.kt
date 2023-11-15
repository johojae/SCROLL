package org.kaist.socspft.dd

import android.Manifest.permission.CAMERA
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import org.kaist.socspft.dd.databinding.ActivityAnalysisBinding
import org.kaist.socspft.dd.opencv.Detector
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class AnalysisActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private lateinit var cameraView: CameraBridgeViewBase
    private lateinit var faceDetector: Detector
    private lateinit var eyeDetector: Detector
    private lateinit var binding: ActivityAnalysisBinding
    private var cameraIndex: Int = 0

    private val loaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                cameraView.enableView()
                faceDetector = Detector(R.raw.haarcascade_frontalface_alt2, mAppContext)
                eyeDetector = Detector(R.raw.haarcascade_eye_tree_eyeglasses, mAppContext)
            } else {
                super.onManagerConnected(status)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_analysis)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraView = findViewById(R.id.activity_surface_view)
        cameraView.visibility = SurfaceView.VISIBLE
        cameraView.setCvCameraViewListener(this)
        cameraView.setCameraIndex(cameraIndex)    // back:0 front:1

        binding.fabBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.fabAlert.setOnClickListener {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone.play()
        }

        binding.fabCamChange.setOnClickListener {
            cameraView.disableView()
            cameraIndex = if (cameraIndex == 0) 1 else 0
            cameraView.setCameraIndex(cameraIndex)
            cameraView.enableView()
        }
    }

    override fun onStart() {
        super.onStart()
        if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(CAMERA), 200)
        } else {
            cameraView.setCameraPermissionGranted()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, loaderCallback)
        } else {
            loaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS)
        }
    }

    override fun onPause() {
        super.onPause()
        cameraView.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.disableView()
    }

    private lateinit var rgba: Mat
    private lateinit var gray: Mat
    override fun onCameraViewStarted(width: Int, height: Int) {
        rgba = Mat(height, width, CvType.CV_8UC4)
        gray = Mat(height, width, CvType.CV_8UC1)
    }

    override fun onCameraViewStopped() {
        rgba.release()
        gray.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        rgba = inputFrame.rgba()
        gray = inputFrame.gray()
        val faces = faceDetector.detect(gray)
        for (face in faces) {
            Imgproc.rectangle(rgba, face, Scalar(0.0, 0.0, 255.0), 3)
            val eyes = eyeDetector.detect(gray.submat(face))
            for (eye in eyes) {
                Imgproc.rectangle(
                    rgba,
                    Rect(face.x + eye.x, face.y + eye.y, eye.width, eye.height),
                    Scalar(0.0, 255.0, 0.0),
                    3
                )
            }
        }
        return rgba
    }
}