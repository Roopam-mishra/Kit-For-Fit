package com.example.kitforfit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.DataSourcesRequest
import com.google.android.gms.fitness.request.SensorRequest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit


const val TAG = "StepCounter"

/**
 * This enum is used to define actions that can be performed after a successful sign in to Fit.
 * One of these values is passed to the Fit sign-in, and returned in a successful callback, allowing
 * subsequent execution of the desired action.
 */
enum class FitActionRequestCode {
    SUBSCRIBE,
    READ_DATA
}

class MainActivity : AppCompatActivity() {

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA)  //TYPE_STEP_COUNT_DELTA
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
        .addDataType(DataType.TYPE_HEART_POINTS)  //FIELD_INTENSITY
        .addDataType(DataType.AGGREGATE_HEART_POINTS) //FIELD_INTENSITY FIELD_DURATION
        .addDataType(DataType.AGGREGATE_DISTANCE_DELTA)
        .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED)  // FIELD_CALORIES
        .addDataType(DataType.TYPE_MOVE_MINUTES)  // FIELD_DURATION
        .addDataType(DataType.AGGREGATE_MOVE_MINUTES)
        .addDataType(DataType.TYPE_STEP_COUNT_CADENCE) //FIELD_RPM
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT) // FIELD_ACTIVITY
        .addDataType(DataType.TYPE_BASAL_METABOLIC_RATE) // FIELD_CALORIES
        .addDataType(DataType.TYPE_CYCLING_PEDALING_CADENCE) //FIELD_RPM
        .addDataType(DataType.TYPE_CYCLING_PEDALING_CUMULATIVE) //FIELD_REVOLUTIONS
        .addDataType(DataType.TYPE_BODY_FAT_PERCENTAGE) //FIELD_PERCENTAGE
        .addDataType(DataType.TYPE_WEIGHT) //FIELD_WEIGHT
        .addDataType(DataType.TYPE_DISTANCE_DELTA) //FIELD_DISTANCE
        .addDataType(DataType.TYPE_LOCATION_SAMPLE) //FIELD_LATITUDE FIELD_LONGITUDE FIELD_ACCURACY FIELD_ALTITUDE
        .addDataType(DataType.TYPE_SPEED) //speed
        .addDataType(DataType.TYPE_HYDRATION) //FIELD_VOLUME
        .addDataType(DataType.TYPE_SLEEP_SEGMENT) //FIELD_SLEEP_SEGMENT_TYPE
        .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY) //FIELD_ACTIVITY FIELD_DURATION FIELD_NUM_SEGMENTS
        .addDataType(DataType.AGGREGATE_BASAL_METABOLIC_RATE_SUMMARY) //FIELD_AVERAGE FIELD_MAX FIELD_MIN
        .addDataType(DataType.AGGREGATE_BODY_FAT_PERCENTAGE_SUMMARY)//FIELD_AVERAGE FIELD_MAX FIELD_MIN
        .addDataType(DataType.AGGREGATE_HEART_RATE_SUMMARY)//FIELD_AVERAGE FIELD_MAX FIELD_MIN
        .addDataType(DataType.AGGREGATE_WEIGHT_SUMMARY)//FIELD_AVERAGE FIELD_MAX FIELD_MIN
        .addDataType(DataType.AGGREGATE_SPEED_SUMMARY)//FIELD_AVERAGE FIELD_MAX FIELD_MIN
        .build()

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)
    }

    private fun checkPermissionsAndRun(fitActionRequestCode: FitActionRequestCode) {
        if (permissionApproved()) {
            fitSignIn(fitActionRequestCode)
        } else {
//            requestRuntimePermissions(fitActionRequestCode)
        }
    }

    private fun fitSignIn(requestCode: FitActionRequestCode) {
        if (oAuthPermissionsApproved()) {
            performActionForRequestCode(requestCode)
        } else {
            requestCode.let {
                GoogleSignIn.requestPermissions(
                    this,
                    requestCode.ordinal,
                    getGoogleAccount(), fitnessOptions)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (resultCode) {
            RESULT_OK -> {
                val postSignInAction = FitActionRequestCode.values()[requestCode]
                postSignInAction.let {
                    performActionForRequestCode(postSignInAction)
                }
            }
            else -> oAuthErrorMsg(requestCode, resultCode)
        }
    }
    private fun performActionForRequestCode(requestCode: FitActionRequestCode) = when (requestCode) {
        FitActionRequestCode.READ_DATA -> readData()
        FitActionRequestCode.SUBSCRIBE -> subscribe()
    }

    private fun oAuthErrorMsg(requestCode: Int, resultCode: Int) {
        val message = """
            There was an error signing into Fit. Check the troubleshooting section of the README
            for potential issues.
            Request code was: $requestCode
            Result code was: $resultCode
        """.trimIndent()
        Log.e(TAG, message)
    }

    private fun oAuthPermissionsApproved() = GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions)
    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

    private fun subscribe() {
        Fitness.getRecordingClient(this, getGoogleAccount())
            .subscribe(DataType.TYPE_LOCATION_SAMPLE)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "Successfully subscribed for weight!")
                } else {
                    Log.w(TAG, "There was a problem subscribing.", task.exception)
                }
            }
    }

    private fun readData(){
        var endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
        var startTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
        Log.i(TAG, "Range Start: $startTime")
        Log.i(TAG, "Range End: $endTime")

        // TYPE_ACTIVITY_SEGMENT

        var readRequest =
            DataReadRequest.Builder()
                .read(DataType.TYPE_ACTIVITY_SEGMENT)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                for (dataSet in response.buckets.flatMap { it.dataSets }) {
                    if(dataSet.isEmpty) {
                        Log.i(TAG, "Dataset is empty")
                        val textView: TextView = findViewById(R.id.type_activity_segment)
                        textView.text = "TYPE_ACTIVITY_SEGMENT = 0"
                    } else {
                        for (dp in dataSet.dataPoints) {
                            Log.i(TAG, "Data point:")
                            Log.i(TAG, "\tType: ${dp.dataType.name}")
                            Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                            Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                            for (field in dp.dataType.fields) {
                                Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                val textView: TextView = findViewById(R.id.type_activity_segment)
                                textView.text = "TYPE_ACTIVITY_SEGMENT = ${dp.getValue(field)}"
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }

        // TYPE_BASAL_METABOLIC_RATE

        readRequest =
            DataReadRequest.Builder()
                .aggregate(DataType.TYPE_BASAL_METABOLIC_RATE)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                for (dataSet in response.buckets.flatMap { it.dataSets }) {
                    if(dataSet.isEmpty) {
                        Log.i(TAG, "Dataset is empty")
                        val textView: TextView = findViewById(R.id.type_basal_metabolic_rate)
                        textView.text = "TYPE_BASAL_METABOLIC_RATE = 0"
                    } else {
                        for (dp in dataSet.dataPoints) {
                            Log.i(TAG, "Data point:")
                            Log.i(TAG, "\tType: ${dp.dataType.name}")
                            Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                            Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                            for (field in dp.dataType.fields) {
                                Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                val textView: TextView = findViewById(R.id.type_basal_metabolic_rate)
                                textView.text = "TYPE_BASAL_METABOLIC_RATE = ${dp.getValue(field)}"
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }

        //TYPE_CALORIES_EXPENDED

        readRequest =
            DataReadRequest.Builder()
                .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
                .bucketByActivityType(1, TimeUnit.HOURS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                for (dataSet in response.buckets.flatMap { it.dataSets }) {
                    if(dataSet.isEmpty) {
                        Log.i(TAG, "Dataset is empty")
                        val textView: TextView = findViewById(R.id.type_calories_expended)
                        textView.text = "TYPE_CALORIES_EXPENDED = 0"
                    } else {
                        for (dp in dataSet.dataPoints) {
                            Log.i(TAG, "Data point:")
                            Log.i(TAG, "\tType: ${dp.dataType.name}")
                            Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                            Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                            for (field in dp.dataType.fields) {
                                Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                val textView: TextView = findViewById(R.id.type_calories_expended)
                                textView.text = "TYPE_CALORIES_EXPENDED = ${dp.getValue(field)}"
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }

        // TYPE_CYCLING_PEDALING_CADENCE

        readRequest =
            DataReadRequest.Builder()
                .read(DataType.TYPE_CYCLING_PEDALING_CADENCE)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                for (dataSet in response.buckets.flatMap { it.dataSets }) {
                    if(dataSet.isEmpty) {
                        Log.i(TAG, "Dataset is empty")
                        val textView: TextView = findViewById(R.id.type_cycling_pedaling_cadence)
                        textView.text = "TYPE_CYCLING_PEDALING_CADENCE = 0"
                    } else {
                        for (dp in dataSet.dataPoints) {
                            Log.i(TAG, "Data point:")
                            Log.i(TAG, "\tType: ${dp.dataType.name}")
                            Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                            Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                            for (field in dp.dataType.fields) {
                                Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                val textView: TextView = findViewById(R.id.type_cycling_pedaling_cadence)
                                textView.text = "TYPE_CYCLING_PEDALING_CADENCE = ${dp.getValue(field)}"
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }

        // TYPE_CYCLING_PEDALING_CUMULATIVE

        readRequest =
            DataReadRequest.Builder()
                .read(DataType.TYPE_CYCLING_PEDALING_CUMULATIVE)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                for (dataSet in response.buckets.flatMap { it.dataSets }) {
                    if(dataSet.isEmpty) {
                        Log.i(TAG, "Dataset is empty")
                        val textView: TextView = findViewById(R.id.type_cycling_pedaling_cumulative)
                        textView.text = "TYPE_CYCLING_PEDALING_CUMULATIVE = 0"
                    } else {
                        for (dp in dataSet.dataPoints) {
                            Log.i(TAG, "Data point:")
                            Log.i(TAG, "\tType: ${dp.dataType.name}")
                            Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                            Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                            for (field in dp.dataType.fields) {
                                Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                val textView: TextView = findViewById(R.id.type_cycling_pedaling_cumulative)
                                textView.text = "TYPE_CYCLING_PEDALING_CUMULATIVE = ${dp.getValue(field)}"
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }

        // TYPE_HEART_POINTS

        Fitness.getHistoryClient(this, getGoogleAccount())
            .readDailyTotal(DataType.TYPE_HEART_POINTS)
            .addOnSuccessListener { dataSet ->
                val total = when {
                    dataSet.isEmpty -> 0
                    else -> DataType.AGGREGATE_HEART_POINTS.toString()
                }
                Log.i(TAG, "Heart Points: $total")
                val textView: TextView = findViewById(R.id.type_heart_points)
                textView.text = "TYPE_HEART_POINTS = ${total}"

            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was a problem getting the step count.", e)
            }

        // TYPE_MOVE_MINUTES

        readRequest =
            DataReadRequest.Builder()
                .read(DataType.TYPE_MOVE_MINUTES)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                for (dataSet in response.buckets.flatMap { it.dataSets }) {
                    if(dataSet.isEmpty) {
                        Log.i(TAG, "Dataset is empty")
                        val textView: TextView = findViewById(R.id.type_move_minutes)
                        textView.text = "TYPE_MOVE_MINUTES = 0"
                    } else {
                        for (dp in dataSet.dataPoints) {
                            Log.i(TAG, "Data point:")
                            Log.i(TAG, "\tType: ${dp.dataType.name}")
                            Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                            Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                            for (field in dp.dataType.fields) {
                                Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                val textView: TextView = findViewById(R.id.type_move_minutes)
                                textView.text = "TYPE_MOVE_MINUTES = ${dp.getValue(field)}"
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }

        // TYPE_STEP_COUNT_CADENCE

        readRequest =
            DataReadRequest.Builder()
                .read(DataType.TYPE_STEP_COUNT_CADENCE)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                for (dataSet in response.buckets.flatMap { it.dataSets }) {
                    if(dataSet.isEmpty) {
                        Log.i(TAG, "Dataset is empty")
                        val textView: TextView = findViewById(R.id.type_step_count_cadence)
                        textView.text = "TYPE_STEP_COUNT_CADENCE = 0"
                    } else {
                        for (dp in dataSet.dataPoints) {
                            Log.i(TAG, "Data point:")
                            Log.i(TAG, "\tType: ${dp.dataType.name}")
                            Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                            Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                            for (field in dp.dataType.fields) {
                                Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                val textView: TextView = findViewById(R.id.type_step_count_cadence)
                                textView.text = "TYPE_STEP_COUNT_CADENCE = ${dp.getValue(field)}"
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }

        //TYPE_STEP_COUNT_DELTA

        Fitness.getHistoryClient(this, getGoogleAccount())
            .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener { dataSet ->
                val total = when {
                    dataSet.isEmpty -> 0
                    else -> dataSet.dataPoints.first().getValue(Field.FIELD_STEPS).asInt()
                }
                Log.i(TAG, "Total steps: $total")
                val textView: TextView = findViewById(R.id.type_step_count_delta)
                textView.text= "TYPE_STEP_COUNT_DELTA = $total"
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was a problem getting the step count.", e)
            }

        // TYPE_BODY_FAT_PERCENTAGE

        readRequest =
            DataReadRequest.Builder()
                .read(DataType.TYPE_BODY_FAT_PERCENTAGE)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                for (dataSet in response.buckets.flatMap { it.dataSets }) {
                    if(dataSet.isEmpty) {
                        Log.i(TAG, "Dataset is empty")
                        val textView: TextView = findViewById(R.id.type_body_fat_percentage)
                        textView.text = "TYPE_BODY_FAT_PERCENTAGE = 0"
                    } else {
                        for (dp in dataSet.dataPoints) {
                            Log.i(TAG, "Data point:")
                            Log.i(TAG, "\tType: ${dp.dataType.name}")
                            Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                            Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                            for (field in dp.dataType.fields) {
                                Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                val textView: TextView = findViewById(R.id.type_body_fat_percentage)
                                textView.text = "TYPE_BODY_FAT_PERCENTAGE = ${dp.getValue(field)}"
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }

        // TYPE_WEIGHT

        val calendar = Calendar.getInstance()
        readRequest =
            DataReadRequest.Builder()
                .read(DataType.TYPE_WEIGHT)
                .setTimeRange(1, calendar.timeInMillis, TimeUnit.MILLISECONDS)
                .setLimit(1)
                .build()
        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { dataReadResult ->
                if (dataReadResult.buckets.isNotEmpty()) {
                    Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                    for (bucket in dataReadResult.buckets) {
                        bucket.dataSets.forEach { dataSet ->
                            if(dataSet.isEmpty) {
                                Log.i(TAG, "Dataset is empty")
                                val textView: TextView = findViewById(R.id.type_weight)
                                textView.text = "TYPE_WEIGHT = 0"
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    Log.i(TAG, "Data point:")
                                    Log.i(TAG, "\tType: ${dp.dataType.name}")
                                    Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                    Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                    for (field in dp.dataType.fields) {
                                        Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                        val textView: TextView = findViewById(R.id.type_weight)
                                        textView.text = "TYPE_WEIGHT = ${dp.getValue(field)}"
                                    }
                                }
                            }
                        }
                    }
                } else if (dataReadResult.dataSets.isNotEmpty()) {
                    Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.dataSets.size)
                    dataReadResult.dataSets.forEach { dataSet ->
                        if(dataSet.isEmpty) {
                            Log.i(TAG, "Dataset is empty")
                            val textView: TextView = findViewById(R.id.type_weight)
                            textView.text = "TYPE_WEIGHT = 0"
                        } else {
                            for (dp in dataSet.dataPoints) {
                                Log.i(TAG, "Data point:")
                                Log.i(TAG, "\tType: ${dp.dataType.name}")
                                Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                for (field in dp.dataType.fields) {
                                    Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                    val textView: TextView = findViewById(R.id.type_weight)
                                    textView.text = "TYPE_WEIGHT = ${dp.getValue(field)}"
                                }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }

        // AGGREGATE_CALORIES_EXPENDED

        readRequest =
            DataReadRequest.Builder()
                .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
                .bucketByActivityType(1, TimeUnit.HOURS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                for (dataSet in response.buckets.flatMap { it.dataSets }) {
                    if(dataSet.isEmpty) {
                        Log.i(TAG, "Dataset is empty")
                        val textView: TextView = findViewById(R.id.aggregate_calories_expended)
                        textView.text = "AGGREGATE_CALORIES_EXPENDED = 0"
                    } else {
                        for (dp in dataSet.dataPoints) {
                            Log.i(TAG, "Data point:")
                            Log.i(TAG, "\tType: ${dp.dataType.name}")
                            Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                            Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                            for (field in dp.dataType.fields) {
                                Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                val textView: TextView = findViewById(R.id.aggregate_calories_expended)
                                textView.text = "AGGREGATE_CALORIES_EXPENDED = ${dp.getValue(field)}"
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }
    }

//    private fun dumpDataSet(dataSet: DataSet) {
//        Log.i(TAG, "Data returned for Data type: ${dataSet.dataType.name}")
//        if(dataSet.isEmpty){
//            Log.i(TAG, "Dataset is empty")
//        }
//        for (dp in dataSet.dataPoints) {
//            Log.i(TAG,"Data point:")
//            Log.i(TAG,"\tType: ${dp.dataType.name}")
//            Log.i(TAG,"\tStart: ${dp.getStartTimeString()}")
//            Log.i(TAG,"\tEnd: ${dp.getEndTimeString()}")
//            for (field in dp.dataType.fields) {
//                Log.i(TAG,"\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
//            }
//        }
//    }

    private fun DataPoint.getStartTimeString() = Instant.ofEpochSecond(this.getStartTime(TimeUnit.SECONDS))
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime().toString()

    private fun DataPoint.getEndTimeString() = Instant.ofEpochSecond(this.getEndTime(TimeUnit.SECONDS))
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime().toString()

    private fun permissionApproved(): Boolean {
        return if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the main; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_read_data) {
            fitSignIn(FitActionRequestCode.READ_DATA)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
