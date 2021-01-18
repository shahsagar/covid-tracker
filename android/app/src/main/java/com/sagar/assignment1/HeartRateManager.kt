package com.sagar.assignment1

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import android.widget.TextView
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio
import java.io.File


class HeartRateManager(private val activity: Activity) {
    private val videoName = "myvideo"
    private val heartRateText: TextView = activity.findViewById(R.id.heart_rate)

    companion object {
        const val VIDEO_CAPTURE = 101
        private val TAG = HeartRateManager::class.simpleName
    }

    fun getLocation() =
        "${activity.getExternalFilesDir(null)?.absolutePath}/${videoName}"

    fun startRecording() {
        Log.d(TAG, "startRecording: started")
        val mediaFile = File(getLocation() + ".mp4")
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        val fileUri = Uri.fromFile(mediaFile)
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_DURATION_LIMIT, 45)
            putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
        }
        activity.startActivityForResult(intent, VIDEO_CAPTURE)
        Log.d(TAG, "startRecording: completed")
    }

    fun analyzeRecording(): Double {
        heartRateText.text = "Analyzing recording"
        convertToAVI()

        val cap = VideoCapture()
        cap.open("${getLocation()}.avi")
        if (cap.isOpened) {
            Log.i(TAG, "analyzeRecording: opened video")
            heartRateText.text = "opened video."

            val videoLength = cap.get(Videoio.CAP_PROP_FRAME_COUNT).toInt()
            val fps = cap.get(Videoio.CV_CAP_PROP_FPS).toInt()
            val totalSeconds = videoLength.toDouble() / fps

            Log.i(TAG, "analyzeRecording: lengths $videoLength, $fps, $totalSeconds")

            val reds = getReds(videoLength, cap)
            val redsAveraged = calculateMovingAverage(reds)
            val numPeaks = getNumPeaks(redsAveraged)

            return 60.0 * numPeaks / totalSeconds
        } else {
            Log.i(TAG, "analyzeRecording: unable to open video")
            return 0.0
        }
    }

    private fun convertToAVI() {
        Log.i(TAG, "analyzeRecording: converting to avi")
        val command = "-y -i ${getLocation()}.mp4 -vcodec mjpeg -qscale 1 -an ${getLocation()}.avi"
        when (val rc = FFmpeg.execute(command)) {
            RETURN_CODE_SUCCESS -> Log.i(Config.TAG, "Command execution completed successfully.")
            RETURN_CODE_CANCEL -> Log.i(Config.TAG, "Command execution cancelled by user.")
            else -> {
                Log.i(Config.TAG, "Command execution failed with rc=$rc and the output below.")
                Config.printLastCommandOutput(Log.INFO)
            }
        }
    }

    private fun getNumPeaks(redsAveraged: MutableList<Double>): Int {
        Log.i(TAG, "getNumPeaks: started")
        var numPeaks = 0
        for (i in 1 until redsAveraged.size - 1)
            if (redsAveraged[i - 1] < redsAveraged[i] && redsAveraged[i] > redsAveraged[i + 1])
                numPeaks++
        return numPeaks
    }

    private fun getReds(videoLength: Int, cap: VideoCapture): MutableList<Double> {
        Log.i(TAG, "getReds: started")
        heartRateText.text = "Reading frames..."
        val frame = Mat()
        val reds = mutableListOf<Double>()

        for (i in 0..videoLength) {
            cap.read(frame)
            reds.add(Core.mean(frame).`val`[2])
        }
        return reds
    }

    private fun calculateMovingAverage(
        reds: MutableList<Double>,
        batchLength: Int = 10
    ): MutableList<Double> {
        Log.i(TAG, "calculateMovingAverage: started")
        heartRateText.text = "Averaging values..."

        var sum = 0.0
        for (i in 0..batchLength) sum += reds[i]

        val redsAveraged = mutableListOf<Double>()
        redsAveraged.add(sum / batchLength)

        for (i in batchLength until reds.size) {
            sum += reds[i]
            sum -= reds[i - batchLength]
            redsAveraged.add(sum / batchLength)
        }
        return redsAveraged
    }
}