package com.example.jm

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class Classifier(private val context1: Context, private val modelName: String) {
    private lateinit var interpreter: Interpreter
    private lateinit var inputImage: TensorImage
    private lateinit var outputBuffer: TensorBuffer
    private var modelInputWidth = 0
    private var modelInputHeight = 0
    private val labels = mutableListOf<String>()

    fun init() {
        interpreter = Interpreter(FileUtil.loadMappedFile(context1, modelName))
        initModelShape()
        labels.addAll(FileUtil.loadLabels(context1, LABEL_FILE))
    }

    fun initForCamera() {
        interpreter = Interpreter(FileUtil.loadMappedFile(context1, modelName))
        initModelShape()
        labels.addAll(FileUtil.loadLabels(context1, CLABEL_FILE))
    }

    private fun initModelShape() {
        val inputTensor = interpreter.getInputTensor(0)
        modelInputWidth = inputTensor.shape()[1]
        modelInputHeight = inputTensor.shape()[2]
        inputImage = TensorImage(inputTensor.dataType())

        val outputTensor = interpreter.getOutputTensor(0)
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType())
    }

    fun classify(image: Bitmap): Pair<String, Float> {
        inputImage = loadImage(image)

        interpreter.run(inputImage.buffer, outputBuffer.buffer.rewind())
        val output = TensorLabel(labels, outputBuffer).mapWithFloatValue

        return argmax(output)
    }

    fun classifySecond(image: Bitmap): Pair<String, Float> {
        inputImage = loadImage(image)

        interpreter.run(inputImage.buffer, outputBuffer.buffer.rewind())
        val output = TensorLabel(labels, outputBuffer).mapWithFloatValue

        return getSecondMax(output)
    }

    private fun loadImage(bitmap: Bitmap): TensorImage {
        if (bitmap.config != Bitmap.Config.ARGB_8888) {
            inputImage.load(convertBitmapToARGB8888(bitmap))
        } else {
            inputImage.load(bitmap)
        }
        val imageProcessor = ImageProcessor.Builder()
            .add(
                ResizeOp(
                    modelInputWidth,
                    modelInputHeight,
                    ResizeOp.ResizeMethod.NEAREST_NEIGHBOR
                )
            )
            .build()

        return imageProcessor.process(inputImage)
    }

    private fun convertBitmapToARGB8888(bitmap: Bitmap) = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    private fun argmax(map: Map<String, Float>) =
        map.entries.maxByOrNull { it.value }?.let {
            it.key to it.value
        } ?: "" to 0f

    private fun getSecondMax(map: Map<String, Float>): Pair<String, Float> {
        val sortedEntries = map.entries.sortedByDescending { it.value }

        return if (sortedEntries.size > 1) {
            sortedEntries[1].let { it.key to it.value }
        } else {
            sortedEntries[1].let { it.key to it.value }
        }
    }

    fun finish() {
        interpreter.close()
    }

    companion object {
        const val MODEL_NAME = "model.tflite"
        const val LABEL_FILE = "labels.txt"
        const val CMODEL_NAME = "cModel.tflite"
        const val CLABEL_FILE = "cLabels.txt"
    }
}
