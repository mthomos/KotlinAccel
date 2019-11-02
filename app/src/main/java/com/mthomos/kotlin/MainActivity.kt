package com.mthomos.kotlin

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.math.MathUtils.clamp
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), SensorEventListener
{
    private var ADAPTIVE_FILTER = true
    val constFilterFactor = 0.1f
    var filterFactor = constFilterFactor
    var buffer = FloatArray(3)
    var accelFilter = FloatArray(3)
    //Variables
    var sensorManager : SensorManager ?= null
    var accelerometer : Sensor ?= null
    var dx = 0.0f
    var dy = 0.0f
    var dz = 0.0f
    var ux = 0.0f
    var uy = 0.0f
    var uz = 0.0f
    var previousTime : Long = 0
    var distance = 0.0f
    var height = 0.0f
    // App mode
    // -1 Start
    // +0 Standby
    // +1 Measure
    var mode = -1
    private var resetButton : Button ?= null
    private var measurementButton : Button ?= null
    private var measurementText : TextView? = null
    //private var valuesText : TextView ?= null
    private var progressBar : ProgressBar ?= null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        resetButton = findViewById(R.id.reset_button)
        resetButton!!.setOnClickListener { reset() }
        measurementButton = findViewById(R.id.measurement_button)
        measurementButton!!.setOnClickListener(object : View.OnClickListener
        {
            override fun onClick(v: View?)
            {
                if (mode == 1)
                    stopMeasureMode()
                else
                    startMeasureMode()
            }
        })
        measurementText = findViewById(R.id.measurement_text)
        //valuesText = findViewById(R.id.accel_values_text)
        progressBar = findViewById(R.id.progressBar)
        previousTime = System.currentTimeMillis()
        setupSensors()
        setStandbyMode()
    }


    private fun reset()
    {
        progressBar!!.visibility = View.GONE
        //resetVariables()
        setStandbyMode()
    }

    private fun resetVariables()
    {
        dx = 0.0f
        dy = 0.0f
        dz = 0.0f
        ux = 0.0f
        uy = 0.0f
        uz = 0.0f
        previousTime = 0
        distance = 0.0f
        height = 0.0f

    }

    private fun  setStandbyMode()
    {
        mode = 0
        measurementText!!.setText("Press Start")
        measurementButton!!.setText("Start")
        progressBar!!.visibility = View.GONE
    }

    private fun startMeasureMode()
    {
        resetVariables()
        measurementText!!.setText("Running")
        measurementButton!!.setText("Stop")
        progressBar!!.visibility = View.VISIBLE
        mode = 1
        previousTime = System.currentTimeMillis()
    }

    private fun stopMeasureMode()
    {
        mode = 0
        measurementText!!.setText("Press Start")
        measurementButton!!.setText("Start")
        progressBar!!.visibility = View.GONE
    }



    private fun setupSensors()
    {
        // get reference of the service
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // focus in accelerometer
        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    }

    override fun onResume()
    {
        super.onResume()
        sensorManager!!.registerListener(this,accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause()
    {
        super.onPause()
        sensorManager!!.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?)
    {
        if (event != null)
        {
            var dt = (System.currentTimeMillis() - previousTime).toFloat()/1000.0f
            //Get Values
            var x_value = event.values[0]
            var y_value = event.values[1]
            var z_value = event.values[2]
           //Filter Values
            if (buffer[0] != 0.0f && buffer[1] != 0.0f && buffer[2] != 0.0f)
            {
                // Adaptive filtering
                if (ADAPTIVE_FILTER)
                {
                    var cutOffFreq = 0.9f
                    var RC = 1 / cutOffFreq
                    var constant = RC / (dt + RC)
                    val kAccelerometerMinStep = 0.033f
                    val kAccelerometerNoiseAttenuation = 3.0f
                    var d = clamp(Math.abs(norm(accelFilter[0], accelFilter[1], accelFilter[2]) - norm(x_value, y_value, z_value)) / kAccelerometerMinStep - 1.0f, 0.0f, 1.0f)
                    filterFactor = d * constant / kAccelerometerNoiseAttenuation + (1.0f - d) * constant;
                }
                else
                    filterFactor = constFilterFactor

                buffer[0] = x_value * filterFactor + buffer[0]*(1.0f - filterFactor)
                buffer[1] = y_value * filterFactor + buffer[1]*(1.0f - filterFactor)
                buffer[2] = z_value * filterFactor + buffer[2]*(1.0f - filterFactor)
                x_value = x_value - buffer[0]
                y_value = y_value - buffer[1]
                z_value = z_value - buffer[2]

            }
                buffer[0] = x_value
                buffer[1] = y_value
                buffer[2] = z_value

            Log.d("dspeed", "X:"+x_value+",Y:"+y_value+",Z:"+z_value)
            if (mode ==1)
            {
                Log.d("time", dt.toString())
                ux = ux + x_value*dt
                uy = uy + y_value*dt
                uz = uz + z_value*dt
                //Log.d("dspeed", "X:"+x_value*dt+",Y:"+y_value*dt+",Z:"+z_value*dt)
                //Log.d("vspeed", "X:"+ux+",Y:"+uy+",Z:"+uz)
                if (ux > -1)
                    Log.d("size", "small")
                else
                    Log.d("size", "big")
                dx = dx + ux*dt
                dy = dy + uy*dt
                dz = dz + uz*dt
                //distance = distance + Math.sqrt(Math.pow(dx.toFloat(), 2.toFloat())+Math.pow(dy.toFloat(),2.toFloat()))
                measurementText!!.setText("X :"+dx.toString()+ "\n"+ "Y :"+dy.toString()+"\n"+"height:"+dz.toString())
                //Log.d("kotlin", distance.toFloat().toString())
                previousTime = System.currentTimeMillis()
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int)
    {
        Log.d("kotlin", "accuracy")
    }

    fun norm(x: Float, y: Float, z: Float): Float
    {
        val Dx = x.toDouble()
        val Dy = y.toDouble()
        val Dz = z.toDouble()
        val result =  sqrt(Dx* Dx + Dy * Dy + Dz * Dz)
        return  result.toFloat()
    }

    fun norm(x: Double, y: Double, z: Double): Double
    {
        return sqrt(x * x + y * y + z * z)
    }
}

