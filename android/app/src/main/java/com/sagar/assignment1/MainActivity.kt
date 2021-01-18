package com.sagar.assignment1

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.OpenCVLoader
import java.util.*


class MainActivity : AppCompatActivity(), SensorEventListener {
    // text views
    private var respiratoryRateText: TextView? = null
    private var heartRateText: TextView? = null

    // buttons
    private var respiratoryRateButton: Button? = null
    private var heartRateButton: Button? = null
    private var playButton: Button? = null

    // rates
    private var respiratoryRate: Double? = 0.0
    private var heartRate: Double? = 0.0

    // sensors
    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null
    private val accelerometerData = mutableListOf<NTuple3<Float, Float, Float>>()

    private var recordAccelerometerStop = Calendar.getInstance().time

    private var heartRateManager: HeartRateManager? = null
    private var recordingStage = 0

    companion object {
        private const val TAG = "MainActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        OpenCVLoader.initDebug()

        // text views
        respiratoryRateText = findViewById(R.id.respiratory_rate)
        heartRateText = findViewById(R.id.heart_rate)

        // sensor
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)

        respiratoryRateButton = findViewById(R.id.measure_respiratory_rate)
        respiratoryRateButton?.setOnClickListener {
            it.isEnabled = false
            Toast.makeText(this, "Recording accelerometer sensor", Toast.LENGTH_SHORT).show()

            accelerometerData.clear()
            recordingStage = 1
            val calender = Calendar.getInstance()
            calender.add(Calendar.SECOND, 45)
            recordAccelerometerStop = calender.time
        }

        heartRateButton = findViewById(R.id.measure_heart_rate)
        heartRateManager = HeartRateManager(this)
        heartRateButton?.setOnClickListener {
            it.isEnabled = false
            heartRateManager?.startRecording()
//            Handler().post {
//                heartRate = heartRateManager?.analyzeRecording()
//                heartRateText?.text = heartRate.toString()
//                heartRateButton?.isEnabled = true
//            }
        }

        playButton = findViewById(R.id.play_button)
        playButton?.setOnClickListener {
            findViewById<VideoView>(R.id.video_view).apply {
                setVideoPath(heartRateManager?.getLocation() + ".mp4")
                start()
            }
        }

        findViewById<Button>(R.id.symptoms).setOnClickListener {
            Intent(this, SymptomsActivity::class.java).apply {
                putExtra("heartRate", heartRate)
                putExtra("respiratoryRate", respiratoryRate)
                startActivity(this)
            }
        }
    }
    private fun getNumPeaks(list: MutableList<Float>): Int {
        Log.i(TAG, "getNumPeaks: started")
        var numPeaks = 0
        for (i in 1 until list.size - 1)
            if (list[i - 1] < list[i] && list[i] > list[i + 1])
                numPeaks++
        return numPeaks
    }

    private fun calculateMovingAverage(
        list: MutableList<Float>,
        batchLength: Int = 5
    ): MutableList<Float> {
        Log.i(TAG, "calculateMovingAverage: started")

        var sum = 0.0
        for (i in 0..batchLength) sum += list[i]

        val averaged = mutableListOf<Float>()
        averaged.add((sum / batchLength).toFloat())

        for (i in batchLength until list.size) {
            sum += list[i]
            sum -= list[i - batchLength]
            averaged.add((sum / batchLength).toFloat())
        }
        return averaged
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        // TODO Auto-generated method stub
        if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            if (Calendar.getInstance().time < recordAccelerometerStop) {
                val (x, y, z) = sensorEvent.values
                respiratoryRateText?.text = "X:$x Y:$y Z:$z"
                accelerometerData.add(NTuple3(x, y, z))
            } else if (recordingStage == 1) {
                callBreathRecognition()
                recordingStage = 2
            }
            if (recordingStage== 2) {
                respiratoryRateButton?.isEnabled = true
                recordingStage = 0
            }
        }
    }
    private fun callBreathRecognition() {
        val zs = mutableListOf<Float>()
        for (data in accelerometerData)
            zs.add(data.t3)
        val averaged = calculateMovingAverage(zs)
        val numPeaks = getNumPeaks(averaged)

        accelerometerData.clear()
        respiratoryRate = 60.0 * numPeaks / 45.0
        respiratoryRateText?.text = respiratoryRate.toString()
        Log.d(TAG, "Breath Rate: $respiratoryRate")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//        TODO("Not yet implemented")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == HeartRateManager.VIDEO_CAPTURE) {
            val text: String = when (resultCode) {
                RESULT_OK -> {
                    Handler().post {
                        heartRate = heartRateManager?.analyzeRecording()
                        heartRateText?.text = heartRate.toString()
                        heartRateButton?.isEnabled = true
                    }
                    "Video has been saved."
                }
                RESULT_CANCELED -> "Video recording cancelled."
                else -> "Failed to record video"
            }
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }
    }
}

data class NTuple3<T1, T2, T3>(val t1: T1, val t2: T2, val t3: T3)
