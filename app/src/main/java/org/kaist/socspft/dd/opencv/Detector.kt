package org.kaist.socspft.dd.opencv

import android.content.Context
import android.content.Context.MODE_PRIVATE
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Rect
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream

class Detector(resourceId: Int, context: Context) {
    private val classifier: CascadeClassifier

    init {
        val inputStream = context.resources.openRawResource(resourceId)
        val outFile = File(context.getDir("cascade", MODE_PRIVATE), resourceId.toString())
        val outStream = FileOutputStream(outFile)
        val data = ByteArray(8192)
        var readBytes: Int

        while (inputStream.read(data).also { readBytes = it } != -1) {
            outStream.write(data, 0, readBytes)
        }

        classifier = CascadeClassifier(outFile.absolutePath)

        inputStream.close()
        outStream.close()
        outFile.delete()
    }

    fun detect(image: Mat, scaleFactor: Double = 1.2, minNeighbors: Int = 5): Array<Rect> {
        val objects = MatOfRect()
        classifier.detectMultiScale(image, objects, scaleFactor, minNeighbors)
        return objects.toArray()
    }
}