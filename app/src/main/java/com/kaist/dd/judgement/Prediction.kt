package com.kaist.dd.judgement

import android.content.Context
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream

class Prediction (private val context: Context, private val asset: String) {
    private lateinit var model: Module
    init {
        val inputStream = context.assets.open(asset)
        val outFile = File(context.getDir("assets", Context.MODE_PRIVATE), asset)
        val outStream = FileOutputStream(outFile)
        val data = ByteArray(8192)
        var readBytes: Int

        while (inputStream.read(data).also { readBytes = it } != -1) {
            outStream.write(data, 0, readBytes)
        }

        model = LiteModuleLoader.load(outFile.absolutePath)

        inputStream.close()
        outStream.close()
        outFile.delete()
    }

    fun predict(ears: ArrayList<Double>): Double {
        val output = this.model.forward(convertToIValue(ears)).toTensor()
        return output.dataAsFloatArray[0].toDouble()
    }

    private fun convertToIValue(ears: ArrayList<Double>): IValue {
        val floatArray = ears.map { v -> v.toFloat() }.toFloatArray()
        val buffer = Tensor.allocateFloatBuffer(60).put(floatArray)

        var shape = LongArray(2)
        shape[0] = 1
        shape[1] = 60

        return IValue.from(Tensor.fromBlob(buffer, shape))
    }
}