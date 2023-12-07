package com.kaist.dd.judgement

class FaceDetection(
    val faceDetectListener: FaceDetectListener? = null
) {

    private var isUndetected = false
    private var undetectedStartTime: Long = 0
    private var undetectedTimeCounter: Int = 0
    private var activeFaceDetect:Boolean = true

    fun setActiveFaceDetect(active: Boolean) {
        activeFaceDetect = active
    }

    fun setDetectedFace() {
        // Face 인식이 된 경우에 대한 처리
        isUndetected = false
        undetectedStartTime = 0
    }

    fun setNotDetectedFace() {
        if (!activeFaceDetect) {
            // Face 미인식에 대한 Alert Dialog 가 출력된 경우 시간 계산 처리를 하지 않음
            isUndetected = false
            return;
        }
        if (!isUndetected) {
            // Face 인식이 처음 안된 경우
            isUndetected = true
            undetectedStartTime = System.currentTimeMillis()
            undetectedTimeCounter = 0
        } else {
            val currentTime = System.currentTimeMillis()
            val timeDifference = currentTime - undetectedStartTime

            // Face 인식에 대한 Time-out 처리 (20 sec)
            undetectedTimeCounter++
            if (undetectedTimeCounter % 30 === 0 && timeDifference >= 1000 * 10) {
                undetectedStartTime = System.currentTimeMillis()
                // Alert
                faceDetectListener?.showFaceNotDetectedAlert()
            }
        }
    }

    interface FaceDetectListener {
        fun showFaceNotDetectedAlert()
    }
}