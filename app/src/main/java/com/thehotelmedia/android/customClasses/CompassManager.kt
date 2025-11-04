package com.thehotelmedia.android.customClasses

import android.content.Context
import android.hardware.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.*

class CompassManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _degrees = MutableLiveData<Float>()
    val degrees: LiveData<Float> get() = _degrees

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)

    private var lastAzimuth: Float = 0f

    init {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer != null && magneticField != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    normalizeSensorValues(event.values, gravity)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    normalizeSensorValues(event.values, geomagnetic)
                }
            }

            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)

            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                SensorManager.getOrientation(rotationMatrix, orientation)
                var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()

                // Normalize to [0, 360)
                azimuth = (azimuth + 360) % 360

                val smoothedAzimuth = lowPassFilter(azimuth, lastAzimuth)

                // Optional: Only update if significant change
                if (abs(smoothedAzimuth - lastAzimuth) > 1f) {
                    _degrees.postValue(smoothedAzimuth)
                    lastAzimuth = smoothedAzimuth
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    private fun lowPassFilter(newValue: Float, oldValue: Float, alpha: Float = 0.2f): Float {
        // Handle wrap-around at 360 degrees
        var delta = (newValue - oldValue + 360) % 360
        if (delta > 180) delta -= 360
        return (oldValue + alpha * delta + 360) % 360
    }

    private fun normalizeSensorValues(input: FloatArray, output: FloatArray) {
        val magnitude = sqrt(input[0].pow(2) + input[1].pow(2) + input[2].pow(2))
        if (magnitude != 0f) {
            output[0] = input[0] / magnitude
            output[1] = input[1] / magnitude
            output[2] = input[2] / magnitude
        }
    }
}
