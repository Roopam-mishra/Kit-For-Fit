package com.example.kitforfit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessActivities
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.*
import com.google.android.material.snackbar.Snackbar
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit


const val TAG = "KitForFit"

/**
 * This enum is used to define actions that can be performed after a successful sign in to Fit.
 * One of these values is passed to the Fit sign-in, and returned in a successful callback, allowing
 * subsequent execution of the desired action.
 */
enum class FitActionRequestCode {
    SUBSCRIBE,
    READ_DATA,
    INSERT_DATA,
    UPDATE_DATA,
    DELETE_DATA
}

class MainActivity : AppCompatActivity() {

    private val fitnessOptions: FitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)  //TYPE_STEP_COUNT_DELTA
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_HEART_POINTS, FitnessOptions.ACCESS_WRITE)  //FIELD_INTENSITY
            .addDataType(DataType.AGGREGATE_HEART_POINTS, FitnessOptions.ACCESS_WRITE) //FIELD_INTENSITY FIELD_DURATION
            .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE)  // FIELD_CALORIES
            .addDataType(DataType.TYPE_MOVE_MINUTES, FitnessOptions.ACCESS_WRITE)  // FIELD_DURATION
            .addDataType(DataType.AGGREGATE_MOVE_MINUTES, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_STEP_COUNT_CADENCE, FitnessOptions.ACCESS_WRITE) //FIELD_RPM
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_WRITE) // FIELD_ACTIVITY
            .addDataType(DataType.TYPE_BASAL_METABOLIC_RATE, FitnessOptions.ACCESS_WRITE) // FIELD_CALORIES
            .addDataType(DataType.TYPE_CYCLING_PEDALING_CADENCE, FitnessOptions.ACCESS_WRITE) //FIELD_RPM
            .addDataType(DataType.TYPE_CYCLING_PEDALING_CUMULATIVE, FitnessOptions.ACCESS_WRITE) //FIELD_REVOLUTIONS
            .addDataType(DataType.TYPE_BODY_FAT_PERCENTAGE, FitnessOptions.ACCESS_WRITE) //FIELD_PERCENTAGE
            .addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_WRITE) //FIELD_WEIGHT
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE) //FIELD_DISTANCE
            .addDataType(DataType.TYPE_LOCATION_SAMPLE, FitnessOptions.ACCESS_WRITE) //FIELD_LATITUDE FIELD_LONGITUDE FIELD_ACCURACY FIELD_ALTITUDE
            .addDataType(DataType.TYPE_SPEED, FitnessOptions.ACCESS_WRITE) //speed
            .addDataType(DataType.TYPE_HYDRATION, FitnessOptions.ACCESS_WRITE) //FIELD_VOLUME
            .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_WRITE) //FIELD_SLEEP_SEGMENT_TYPE
            .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY, FitnessOptions.ACCESS_WRITE) //FIELD_ACTIVITY FIELD_DURATION FIELD_NUM_SEGMENTS
            .addDataType(DataType.AGGREGATE_BASAL_METABOLIC_RATE_SUMMARY, FitnessOptions.ACCESS_WRITE) //FIELD_AVERAGE FIELD_MAX FIELD_MIN
            .addDataType(DataType.AGGREGATE_BODY_FAT_PERCENTAGE_SUMMARY, FitnessOptions.ACCESS_WRITE)//FIELD_AVERAGE FIELD_MAX FIELD_MIN
            .addDataType(DataType.AGGREGATE_HEART_RATE_SUMMARY, FitnessOptions.ACCESS_WRITE)//FIELD_AVERAGE FIELD_MAX FIELD_MIN
            .addDataType(DataType.AGGREGATE_WEIGHT_SUMMARY, FitnessOptions.ACCESS_WRITE)//FIELD_AVERAGE FIELD_MAX FIELD_MIN
            .addDataType(DataType.AGGREGATE_SPEED_SUMMARY, FitnessOptions.ACCESS_WRITE)//FIELD_AVERAGE FIELD_MAX FIELD_MIN
            .build()
    }

        private val runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)
//            val stepButton :Button = findViewById(R.id.button1)
//            stepButton.setOnClickListener {
//                Fitness.getHistoryClient(this, getGoogleAccount())
//                    .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
//                    .addOnSuccessListener { dataSet ->
//                        val total = when {
//                            dataSet.isEmpty -> 0
//                            else -> dataSet.dataPoints.first().getValue(Field.FIELD_STEPS).asInt()
//                        }
//                        Log.i(TAG, "Total steps: $total")
//                        var textView: TextView = findViewById(R.id.text_view1)
//                        textView.text= "Steps"
//                        textView = findViewById(R.id.text_view2)
//                        textView.text= "$total"
//                        textView = findViewById(R.id.text_view3)
//                        textView.text= "Weekly steps detail:"
//                    }
//                    .addOnFailureListener { e ->
//                        Log.w(TAG, "There was a problem getting the step count.", e)
//                    }
//            }
        }

        private fun checkPermissionsAndRun(fitActionRequestCode: FitActionRequestCode) {
            if (permissionApproved()) {
                fitSignIn(fitActionRequestCode)
            } else {
                requestRuntimePermissions(fitActionRequestCode)
            }
        }

        private fun fitSignIn(requestCode:FitActionRequestCode ) {
            if (oAuthPermissionsApproved()) {
                performActionForRequestCode(requestCode)
                Log.i(TAG, "$requestCode")
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
            FitActionRequestCode.INSERT_DATA -> insertData()
            FitActionRequestCode.UPDATE_DATA -> updateData()
            FitActionRequestCode.DELETE_DATA -> deleteData()
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
                .subscribe(DataType.AGGREGATE_ACTIVITY_SUMMARY)
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
                    var x=0;
                    for (dataSet in response.buckets.flatMap { it.dataSets }) {
                        if(dataSet.isEmpty) {
                            Log.i(TAG, "Dataset is empty")
                        } else {
                            for (dp in dataSet.dataPoints) {
                                Log.i(TAG, "Data point:")
                                Log.i(TAG, "\tType: ${dp.dataType.name}")
                                Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                for (field in dp.dataType.fields) {
                                    Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                    x += dp.getValue(field).asInt()
                                }
                                Log.i(TAG, "${x}")
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.type_activity_segment)
                    textView.text = "TYPE_ACTIVITY_SEGMENT = ${x}"
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
                                    if(field.name.toString() == "average") {
                                        val textView: TextView = findViewById(R.id.type_basal_metabolic_rate)
                                        textView.text = "TYPE_BASAL_METABOLIC_RATE = ${dp.getValue(field)}"
                                    }
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
                    .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                    .bucketByActivityType(1, TimeUnit.HOURS)
                    .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    if (dataReadResult.buckets.isNotEmpty()) {
                        var x = 0f
                        Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
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
                                            x += dp.getValue(field).asFloat()
                                        }
                                    }
                                }
                            }
                        }
                        var textView: TextView = findViewById(R.id.type_calories_expended)
                        textView.text = "TYPE_CALORIES_EXPENDED = ${x}"
                        textView = findViewById(R.id.aggregate_calories_expended)
                        textView.text = "TYPE_CALORIES_EXPENDED = ${x}"
                    } else if (dataReadResult.dataSets.isNotEmpty()) {
                        var x = 0f
                        Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.dataSets.size)
                        dataReadResult.dataSets.forEach { dataSet ->
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
                                        x += dp.getValue(field).asFloat()
                                    }
                                }
                            }
                        }
                        var textView: TextView = findViewById(R.id.type_calories_expended)
                        textView.text = "TYPE_CALORIES_EXPENDED = ${x}"
                        textView = findViewById(R.id.aggregate_calories_expended)
                        textView.text = "TYPE_CALORIES_EXPENDED = ${x}"
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
                    var x = 0f
                    var count = 0
                    for (dataSet in response.buckets.flatMap { it.dataSets }) {
                        if(dataSet.isEmpty) {
                            Log.i(TAG, "Dataset is empty")
                            x += 0
                        } else {
                            for (dp in dataSet.dataPoints) {
                                Log.i(TAG, "Data point:")
                                Log.i(TAG, "\tType: ${dp.dataType.name}")
                                Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                for (field in dp.dataType.fields) {
                                    Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                    x+=dp.getValue(field).asFloat()
                                    count++
                                }
                            }
                        }
                    }
                    Log.i(TAG, "$x and $count")
                    val textView: TextView = findViewById(R.id.type_cycling_pedaling_cadence)
                    textView.text = "TYPE_CYCLING_PEDALING_CADENCE = ${x/count}"
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
                    var x = 0
                    for (dataSet in response.buckets.flatMap { it.dataSets }) {
                        if(dataSet.isEmpty) {
                            Log.i(TAG, "Dataset is empty")
                            x += 0
                        } else {
                            for (dp in dataSet.dataPoints) {
                                Log.i(TAG, "Data point:")
                                Log.i(TAG, "\tType: ${dp.dataType.name}")
                                Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                for (field in dp.dataType.fields) {
                                    Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                    x += dp.getValue(field).asInt()
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.type_cycling_pedaling_cumulative)
                    textView.text = "TYPE_CYCLING_PEDALING_CUMULATIVE = ${x}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            // TYPE_HEART_POINTS
            readRequest =
                DataReadRequest.Builder()
                    .read(DataType.TYPE_HEART_POINTS)
                    .bucketByActivityType(1, TimeUnit.SECONDS)
                    .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    if (dataReadResult.buckets.isNotEmpty()) {
                        var x = 0f
                        Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    Log.i(TAG, "Dataset is empty")
                                    x += 0
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        Log.i(TAG, "Data point:")
                                        Log.i(TAG, "\tType: ${dp.dataType.name}")
                                        Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                        Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                        for (field in dp.dataType.fields) {
                                            Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                            if(field.name.toString() == "intensity") {
                                                x += dp.getValue(field).asFloat()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        val textView: TextView = findViewById(R.id.type_heart_points)
                        textView.text = "TYPE_HEART_POINTS = ${x}"
                    } else if (dataReadResult.dataSets.isNotEmpty()) {
                        var x = 0f
                        Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.dataSets.size)
                        dataReadResult.dataSets.forEach { dataSet ->
                            if(dataSet.isEmpty) {
                                Log.i(TAG, "Dataset is empty")
                                x += 0
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    Log.i(TAG, "Data point:")
                                    Log.i(TAG, "\tType: ${dp.dataType.name}")
                                    Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                    Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                    for (field in dp.dataType.fields) {
                                        Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                        if(field.name.toString() == "intensity") {
                                            x += dp.getValue(field).asFloat()
                                        }
                                    }
                                }
                            }
                        }
                        val textView: TextView = findViewById(R.id.type_heart_points)
                        textView.text = "TYPE_HEART_POINTS = ${x}"
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
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
                    var x = 0
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
                                    x += dp.getValue(field).asInt()
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.type_move_minutes)
                    textView.text = "TYPE_MOVE_MINUTES = ${x}"
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
                    var x = 0f
                    var count = 0
                    for (dataSet in response.buckets.flatMap { it.dataSets }) {
                        if(dataSet.isEmpty) {
                            Log.i(TAG, "Dataset is empty")
                            x += 0
                        } else {
                            for (dp in dataSet.dataPoints) {
                                Log.i(TAG, "Data point:")
                                Log.i(TAG, "\tType: ${dp.dataType.name}")
                                Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                for (field in dp.dataType.fields) {
                                    Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                    x += dp.getValue(field).asFloat()
                                    count++
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.type_step_count_cadence)
                    textView.text = "TYPE_STEP_COUNT_CADENCE = ${x/count}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            // TYPE_STEP_COUNT_DELTA

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
                    var fat = 0f
                    for (dataSet in response.buckets.flatMap { it.dataSets }) {
                        if(dataSet.isEmpty) {
                            Log.i(TAG, "Dataset is empty")
                        } else {
                            for (dp in dataSet.dataPoints) {
                                Log.i(TAG, "Data point:")
                                Log.i(TAG, "\tType: ${dp.dataType.name}")
                                Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                for (field in dp.dataType.fields) {
                                    Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                    fat += dp.getValue(field).asFloat()
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.type_body_fat_percentage)
                    textView.text = "TYPE_BODY_FAT_PERCENTAGE = ${fat}"
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

            // TYPE_DISTANCE_DELTA

            readRequest =
                DataReadRequest.Builder()
                    .read(DataType.TYPE_DISTANCE_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0f
                    if (dataReadResult.buckets.isNotEmpty()) {
                        Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    Log.i(TAG, "Dataset is empty")
                                    x += 0f
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        Log.i(TAG, "Data point:")
                                        Log.i(TAG, "\tType: ${dp.dataType.name}")
                                        Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                        Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                        for (field in dp.dataType.fields) {
                                            Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                            x += dp.getValue(field).asFloat()
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
                                x += 0f
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    Log.i(TAG, "Data point:")
                                    Log.i(TAG, "\tType: ${dp.dataType.name}")
                                    Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                    Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                    for (field in dp.dataType.fields) {
                                        Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                        x += dp.getValue(field).asFloat()
                                    }
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.type_distance_delta)
                    textView.text = "TYPE_DISTANCE_DELTA = ${x}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            // TYPE_SPEED

            readRequest =
                DataReadRequest.Builder()
                    .read(DataType.TYPE_SPEED)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0f
                    var count = 0
                    if (dataReadResult.buckets.isNotEmpty()) {
                        Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    Log.i(TAG, "Dataset is empty")
                                    x += 0f
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        Log.i(TAG, "Data point:")
                                        Log.i(TAG, "\tType: ${dp.dataType.name}")
                                        Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                        Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                        for (field in dp.dataType.fields) {
                                            Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                            x += dp.getValue(field).asFloat()
                                            count++
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
                                x += 0f
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    Log.i(TAG, "Data point:")
                                    Log.i(TAG, "\tType: ${dp.dataType.name}")
                                    Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                    Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                    for (field in dp.dataType.fields) {
                                        Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                        x += dp.getValue(field).asFloat()
                                        count++
                                    }
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.type_speed)
                    textView.text = "TYPE_SPEED = ${x/count}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            // TYPE_HYDRATION
            readRequest =
                DataReadRequest.Builder()
                    .read(DataType.TYPE_HYDRATION)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0f
                    if (dataReadResult.buckets.isNotEmpty()) {
                        Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    Log.i(TAG, "Dataset is empty")
                                    x += 0f
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        Log.i(TAG, "Data point:")
                                        Log.i(TAG, "\tType: ${dp.dataType.name}")
                                        Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                        Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                        for (field in dp.dataType.fields) {
                                            Log.i(
                                                TAG,
                                                "\tField: ${field.name.toString()} Value: ${
                                                    dp.getValue(field)
                                                }"
                                            )
                                            x += dp.getValue(field).asFloat()
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
                                x += 0f
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    Log.i(TAG, "Data point:")
                                    Log.i(TAG, "\tType: ${dp.dataType.name}")
                                    Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                    Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                    for (field in dp.dataType.fields) {
                                        Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                        x += dp.getValue(field).asFloat()
                                    }
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.type_hydration)
                    textView.text = "TYPE_HYDRATION = ${x}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            // TYPE_SLEEP_SEGMENT
            readRequest =
                DataReadRequest.Builder()
                    .read(DataType.TYPE_SLEEP_SEGMENT)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0
                    if (dataReadResult.buckets.isNotEmpty()) {
                        Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    Log.i(TAG, "Dataset is empty")
                                    x += 0
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        Log.i(TAG, "Data point:")
                                        Log.i(TAG, "\tType: ${dp.dataType.name}")
                                        Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                        Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                        for (field in dp.dataType.fields) {
                                            Log.i(
                                                TAG,
                                                "\tField: ${field.name.toString()} Value: ${
                                                    dp.getValue(field)
                                                }"
                                            )
                                            x += dp.getValue(field).asInt()
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
                                x += 0
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    Log.i(TAG, "Data point:")
                                    Log.i(TAG, "\tType: ${dp.dataType.name}")
                                    Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                    Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                    for (field in dp.dataType.fields) {
                                        Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                        x += dp.getValue(field).asInt()
                                    }
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.type_sleep_segment)
                    textView.text = "TYPE_SLEEP_SEGMENT = ${x}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            // AGGREGATE_ACTIVITY_SUMMARY
            readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0
                    if (dataReadResult.buckets.isNotEmpty()) {
                        Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    Log.i(TAG, "Dataset is empty")
                                    x += 0
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        Log.i(TAG, "Data point:")
                                        Log.i(TAG, "\tType: ${dp.dataType.name}")
                                        Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                        Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                        for (field in dp.dataType.fields) {
                                            Log.i(
                                                TAG,
                                                "\tField: ${field.name.toString()} Value: ${
                                                    dp.getValue(field)
                                                }"
                                            )
//                                            x += dp.getValue(field).asInt()
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
                                x += 0
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    Log.i(TAG, "Data point:")
                                    Log.i(TAG, "\tType: ${dp.dataType.name}")
                                    Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                    Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                    for (field in dp.dataType.fields) {
                                        Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
//                                        x += dp.getValue(field).asInt()
                                    }
                                }
                            }
                        }
                    }
//                    val textView: TextView = findViewById(R.id.type_speed)
//                    textView.text = "TYPE_SLEEP_SEGMENT = ${x}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            // AGGREGATE_HEART_POINTS
            readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_HEART_POINTS, DataType.AGGREGATE_HEART_POINTS)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0
                    if (dataReadResult.buckets.isNotEmpty()) {
                        Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    Log.i(TAG, "Dataset is empty")
                                    x += 0
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        Log.i(TAG, "Data point:")
                                        Log.i(TAG, "\tType: ${dp.dataType.name}")
                                        Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                        Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                        for (field in dp.dataType.fields) {
                                            Log.i(
                                                TAG,
                                                "\tField: ${field.name.toString()} Value: ${
                                                    dp.getValue(field)
                                                }"
                                            )
//                                            x += dp.getValue(field).asInt()
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
                                x += 0
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    Log.i(TAG, "Data point:")
                                    Log.i(TAG, "\tType: ${dp.dataType.name}")
                                    Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                    Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                    for (field in dp.dataType.fields) {
                                        Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
//                                        x += dp.getValue(field).asInt()
                                    }
                                }
                            }
                        }
                    }
//                    val textView: TextView = findViewById(R.id.type_speed)
//                    textView.text = "TYPE_SLEEP_SEGMENT = ${x}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            // AGGREGATE_BODY_FAT_PERCENTAGE_SUMMARY
            readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_BODY_FAT_PERCENTAGE, DataType.AGGREGATE_BODY_FAT_PERCENTAGE_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0
                    if (dataReadResult.buckets.isNotEmpty()) {
                        Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    Log.i(TAG, "Dataset is empty")
                                    x += 0
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        Log.i(TAG, "Data point:")
                                        Log.i(TAG, "\tType: ${dp.dataType.name}")
                                        Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                        Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                        for (field in dp.dataType.fields) {
                                            Log.i(
                                                TAG,
                                                "\tField: ${field.name.toString()} Value: ${
                                                    dp.getValue(field)
                                                }"
                                            )
//                                            x += dp.getValue(field).asInt()
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
                                x += 0
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    Log.i(TAG, "Data point:")
                                    Log.i(TAG, "\tType: ${dp.dataType.name}")
                                    Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                    Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                    for (field in dp.dataType.fields) {
                                        Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
//                                        x += dp.getValue(field).asInt()
                                    }
                                }
                            }
                        }
                    }
//                    val textView: TextView = findViewById(R.id.type_speed)
//                    textView.text = "TYPE_SLEEP_SEGMENT = ${x}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            // AGGREGATE_WEIGHT_SUMMARY
            readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0
                    if (dataReadResult.buckets.isNotEmpty()) {
                        Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    Log.i(TAG, "Dataset is empty")
                                    x += 0
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        Log.i(TAG, "Data point:")
                                        Log.i(TAG, "\tType: ${dp.dataType.name}")
                                        Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                        Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                        for (field in dp.dataType.fields) {
                                            Log.i(
                                                TAG,
                                                "\tField: ${field.name.toString()} Value: ${
                                                    dp.getValue(field)
                                                }"
                                            )
//                                            x += dp.getValue(field).asInt()
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
                                x += 0
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    Log.i(TAG, "Data point:")
                                    Log.i(TAG, "\tType: ${dp.dataType.name}")
                                    Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                    Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                    for (field in dp.dataType.fields) {
                                        Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
//                                        x += dp.getValue(field).asInt()
                                    }
                                }
                            }
                        }
                    }
//                    val textView: TextView = findViewById(R.id.type_speed)
//                    textView.text = "TYPE_SLEEP_SEGMENT = ${x}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }


            // TYPE_HEART_RATE_BPM

            readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_HEART_RATE_BPM)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0f
                    var count = 0
                    if (dataReadResult.buckets.isNotEmpty()) {
                        Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    Log.i(TAG, "Dataset is empty")
                                    x += 0f
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        Log.i(TAG, "Data point:")
                                        Log.i(TAG, "\tType: ${dp.dataType.name}")
                                        Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                        Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                        for (field in dp.dataType.fields) {
                                            Log.i(
                                                TAG,
                                                "\tField: ${field.name.toString()} Value: ${
                                                    dp.getValue(field)
                                                }"
                                            )
                                            x += dp.getValue(field).asFloat()
                                            count++
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
                                x += 0f
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    Log.i(TAG, "Data point:")
                                    Log.i(TAG, "\tType: ${dp.dataType.name}")
                                    Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                    Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                    for (field in dp.dataType.fields) {
                                        Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
                                        x += dp.getValue(field).asFloat()
                                        count++;
                                    }
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.type_heart_rate_bpm)
                    textView.text = "TYPE_HEART_RATE_BPM = ${x/count}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            // AGGREGATE_HEART_RATE_SUMMARY
            readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_HEART_RATE_BPM, DataType.AGGREGATE_HEART_RATE_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0
                    if (dataReadResult.buckets.isNotEmpty()) {
                        Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    Log.i(TAG, "Dataset is empty")
                                    x += 0
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        Log.i(TAG, "Data point:")
                                        Log.i(TAG, "\tType: ${dp.dataType.name}")
                                        Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                        Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                        for (field in dp.dataType.fields) {
                                            Log.i(
                                                TAG,
                                                "\tField: ${field.name.toString()} Value: ${
                                                    dp.getValue(field)
                                                }"
                                            )
//                                            x += dp.getValue(field).asInt()
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
                                x += 0
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    Log.i(TAG, "Data point:")
                                    Log.i(TAG, "\tType: ${dp.dataType.name}")
                                    Log.i(TAG, "\tStart: ${dp.getStartTimeString()}")
                                    Log.i(TAG, "\tEnd: ${dp.getEndTimeString()}")
                                    for (field in dp.dataType.fields) {
                                        Log.i(TAG, "\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
//                                        x += dp.getValue(field).asInt()
                                    }
                                }
                            }
                        }
                    }
//                    val textView: TextView = findViewById(R.id.type_speed)
//                    textView.text = "TYPE_SLEEP_SEGMENT = ${x}"
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

        // INSERT DATA

        private fun insertData() {
            var endTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).minusDays(1)
            var startTime = endTime.minusHours(2)

            // TYPE_ACTIVITY_SEGMENT

            // Create a data source
            var dataSource = DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_ACTIVITY_SEGMENT)
                .setStreamName("$TAG - activity_segment")
                .setType(DataSource.TYPE_RAW)
                .build()

            // For each data point, specify a start time, end time, and the
            // data value -- in this case, 950 new steps.
            var dataPoint =
                DataPoint.builder(dataSource)
                    .setActivityField(Field.FIELD_ACTIVITY, FitnessActivities.BADMINTON)
                    .setTimeInterval(1, Calendar.getInstance().timeInMillis, TimeUnit.MILLISECONDS)
                    .build()

            var dataSet = DataSet.builder(dataSource)
                .add(dataPoint)
                .build()

            Fitness.getHistoryClient(this, getGoogleAccount())
                .insertData(dataSet)
                .addOnSuccessListener {
                    Log.i(TAG, "DataSet added successfully!")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was an error adding the DataSet", e)
                }

            // TYPE_BASAL_METABOLIC_RATE

            // Create a data source
            val calendar = Calendar.getInstance()
            dataSource = DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_BASAL_METABOLIC_RATE)
                .setStreamName("$TAG - basal_metabolic_rate")
                .setType(DataSource.TYPE_RAW)
                .build()

            // For each data point, specify a start time, end time, and the
            // data value -- in this case, 950 new steps.
            val bmrData = 600f
            dataPoint =
                DataPoint.builder(dataSource)
                    .setField(Field.FIELD_CALORIES,bmrData)
                    .setTimeInterval(1, calendar.timeInMillis, TimeUnit.MILLISECONDS)
                    .build()

            dataSet = DataSet.builder(dataSource)
                .add(dataPoint)
                .build()

            Fitness.getHistoryClient(this, getGoogleAccount())
                .insertData(dataSet)
                .addOnSuccessListener {
                    Log.i(TAG, "DataSet added successfully!")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was an error adding the DataSet", e)
                }

            // TYPE_CALORIES_EXPENDED

            dataSource = DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_CALORIES_EXPENDED)
                .setStreamName("$TAG - calories_expended")
                .setType(DataSource.TYPE_RAW)
                .build()

            // For each data point, specify a start time, end time, and the
            // data value -- in this case, 950 new steps.

            val caloriesBurned = 1000f
            dataPoint =
                DataPoint.builder(dataSource)
                    .setField(Field.FIELD_CALORIES, caloriesBurned)
                    .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()

            dataSet = DataSet.builder(dataSource)
                .add(dataPoint)
                .build()

            Fitness.getHistoryClient(this, getGoogleAccount())
                .insertData(dataSet)
                .addOnSuccessListener {
                    Log.i(TAG, "DataSet added successfully!")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was an error adding the DataSet", e)
                }

            // TYPE_CYCLING_PEDALING_CADENCE

            dataSource = DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_CYCLING_PEDALING_CADENCE)
                .setStreamName("$TAG - type cycling pedaling cadence")
                .setType(DataSource.TYPE_RAW)
                .build()

            // For each data point, specify a start time, end time, and the
            // data value -- in this case, 950 new steps.

            val cyclingPedalingCadence = 1000f
            dataPoint =
                DataPoint.builder(dataSource)
                    .setField(Field.FIELD_RPM, cyclingPedalingCadence)
                    .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()

            dataSet = DataSet.builder(dataSource)
                .add(dataPoint)
                .build()

            Fitness.getHistoryClient(this, getGoogleAccount())
                .insertData(dataSet)
                .addOnSuccessListener {
                    Log.i(TAG, "DataSet added successfully!")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was an error adding the DataSet", e)
                }

            // TYPE_CYCLING_PEDALING_CUMULATIVE

            dataSource = DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_CYCLING_PEDALING_CUMULATIVE)
                .setStreamName("$TAG - type cycling pedaling cumulative")
                .setType(DataSource.TYPE_RAW)
                .build()

//             For each data point, specify a start time, end time, and the
//             data value -- in this case, 950 new steps.

            val cyclingPedalingCumulative = 100
            dataPoint =
                DataPoint.builder(dataSource)
                    .setField(Field.FIELD_REVOLUTIONS, cyclingPedalingCumulative)
                    .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()

            dataSet = DataSet.builder(dataSource)
                .add(dataPoint)
                .build()

            Fitness.getHistoryClient(this, getGoogleAccount())
                .insertData(dataSet)
                .addOnSuccessListener {
                    Log.i(TAG, "DataSet added successfully!")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was an error adding the DataSet", e)
                }


            // TYPE_STEP_COUNT_CADENCE

            dataSource = DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_STEP_COUNT_CADENCE)
                .setStreamName("$TAG - type step count cadence")
                .setType(DataSource.TYPE_RAW)
                .build()

//             For each data point, specify a start time, end time, and the
//             data value -- in this case, 950 new steps.

            val stepCountCadence = 100f
            dataPoint =
                DataPoint.builder(dataSource)
                    .setField(Field.FIELD_RPM, stepCountCadence)
                    .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()

            dataSet = DataSet.builder(dataSource)
                .add(dataPoint)
                .build()

            Fitness.getHistoryClient(this, getGoogleAccount())
                .insertData(dataSet)
                .addOnSuccessListener {
                    Log.i(TAG, "DataSet added successfully!")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was an error adding the DataSet", e)
                }

            // TYPE_BODY_FAT_PERCENTAGE

            dataSource = DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_BODY_FAT_PERCENTAGE)
                .setStreamName("$TAG - type step count cadence")
                .setType(DataSource.TYPE_RAW)
                .build()

//             For each data point, specify a start time, end time, and the
//             data value -- in this case, 950 new steps.

            val fatPercentage = 2f
            dataPoint =
                DataPoint.builder(dataSource)
                    .setField(Field.FIELD_PERCENTAGE, fatPercentage)
                    .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()

            dataSet = DataSet.builder(dataSource)
                .add(dataPoint)
                .build()

            Fitness.getHistoryClient(this, getGoogleAccount())
                .insertData(dataSet)
                .addOnSuccessListener {
                    Log.i(TAG, "DataSet added successfully!")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was an error adding the DataSet", e)
                }

            // TYPE_DISTANCE_DELTA

            dataSource = DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_DISTANCE_DELTA)
                .setStreamName("$TAG - type distance delta")
                .setType(DataSource.TYPE_RAW)
                .build()

//             For each data point, specify a start time, end time, and the
//             data value -- in this case, 950 new steps.

            var distanceDelta = 24f
            dataPoint =
                DataPoint.builder(dataSource)
                    .setField(Field.FIELD_DISTANCE, distanceDelta)
                    .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()

            dataSet = DataSet.builder(dataSource)
                .add(dataPoint)
                .build()

            Fitness.getHistoryClient(this, getGoogleAccount())
                .insertData(dataSet)
                .addOnSuccessListener {
                    Log.i(TAG, "DataSet added successfully!")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was an error adding the DataSet", e)
                }

            // TYPE_HYDRATION

            dataSource = DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_HYDRATION)
                .setStreamName("$TAG - type hydration")
                .setType(DataSource.TYPE_RAW)
                .build()

//             For each data point, specify a start time, end time, and the
//             data value -- in this case, 950 new steps.

            var litres = 12f
            dataPoint =
                DataPoint.builder(dataSource)
                    .setField(Field.FIELD_VOLUME, litres)
                    .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()

            dataSet = DataSet.builder(dataSource)
                .add(dataPoint)
                .build()

            Fitness.getHistoryClient(this, getGoogleAccount())
                .insertData(dataSet)
                .addOnSuccessListener {
                    Log.i(TAG, "DataSet added successfully!")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was an error adding the DataSet", e)
                }

            // TYPE_SLEEP_SEGMENT

            dataSource = DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_SLEEP_SEGMENT)
                .setStreamName("$TAG - type hydration")
                .setType(DataSource.TYPE_RAW)
                .build()

//             For each data point, specify a start time, end time, and the
//             data value -- in this case, 950 new steps.

            var hours = 12
            dataPoint =
                DataPoint.builder(dataSource)
                    .setField(Field.FIELD_SLEEP_SEGMENT_TYPE, hours)
                    .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()

            dataSet = DataSet.builder(dataSource)
                .add(dataPoint)
                .build()

            Fitness.getHistoryClient(this, getGoogleAccount())
                .insertData(dataSet)
                .addOnSuccessListener {
                    Log.i(TAG, "DataSet added successfully!")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was an error adding the DataSet", e)
                }

            // TYPE_HEART_RATE_BPM

            dataSource = DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_HEART_RATE_BPM)
                .setStreamName("$TAG - type hydration")
                .setType(DataSource.TYPE_RAW)
                .build()

//             For each data point, specify a start time, end time, and the
//             data value -- in this case, 950 new steps.

            var bpm = 72f
            dataPoint =
                DataPoint.builder(dataSource)
                    .setField(Field.FIELD_BPM, bpm)
                    .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()

            dataSet = DataSet.builder(dataSource)
                .add(dataPoint)
                .build()

            Fitness.getHistoryClient(this, getGoogleAccount())
                .insertData(dataSet)
                .addOnSuccessListener {
                    Log.i(TAG, "DataSet added successfully!")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was an error adding the DataSet", e)
                }

        }

    // UPDATE_DATA

    private fun updateData() {
        val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val startTime = endTime.minusDays(1)

        // UPDATE WEIGHT

        // Create a data source
        val dataSource  = DataSource.Builder()
            .setAppPackageName(this)
            .setDataType(DataType.TYPE_WEIGHT)
            .setStreamName("$TAG - weight")
            .setType(DataSource.TYPE_RAW)
            .build()

        // Create a data set
        // For each data point, specify a start time, end time, and the
        // data value -- in this case, 1000 new steps.
        val typeWeight = 59f

        val dataPoint = DataPoint.builder(dataSource)
            .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
            .setField(Field.FIELD_WEIGHT, typeWeight)
            .build()

        val dataSet = DataSet.builder(dataSource)
            .add(dataPoint)
            .build()

        val request = DataUpdateRequest.Builder()
            .setDataSet(dataSet)
            .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
            .build()

        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .updateData(request)
            .addOnSuccessListener {
                Log.i(TAG, "DataSet updated successfully!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was an error updating the DataSet", e)
            }
    }

    private fun deleteData(){
        // Declare that this code deletes step count information that was collected
// throughout the past day.
        val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val startTime = endTime.minusDays(1)

// Create a delete request object, providing a data type and a time interval
        val request = DataDeleteRequest.Builder()
            .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .build()

// Invoke the History API with the HistoryClient object and delete request, and
// then specify a callback that will check the result.
        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .deleteData(request)
            .addOnSuccessListener {
                Log.i(TAG, "Data deleted successfully!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was an error with the deletion request", e)
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
            } else if (id == R.id.action_insert_data) {
                fitSignIn(FitActionRequestCode.INSERT_DATA)
                return true
            } else if (id == R.id.action_update_data) {
                fitSignIn(FitActionRequestCode.UPDATE_DATA)
                return  true
            } else if (id == R.id.action_delete_data) {
                fitSignIn(FitActionRequestCode.DELETE_DATA)
                return true
            } else if (id == R.id.action_unsubscribe) {
                unsubscribe()
                return true
            }
            return super.onOptionsItemSelected(item)
        }

    private fun unsubscribe() {
        Fitness.getConfigClient(this,  GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .disableFit()
            .addOnSuccessListener {
                Log.i(TAG,"Disabled Google Fit")
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error disabling Google Fit", e)
            }
    }
        private fun requestRuntimePermissions(requestCode: FitActionRequestCode) {
            val shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACTIVITY_RECOGNITION)

            // Provide an additional rationale to the user. This would happen if the user denied the
            // request previously, but didn't check the "Don't ask again" checkbox.
            requestCode.let {
                if (shouldProvideRationale) {
                    Log.i(TAG, "Displaying permission rationale to provide additional context.")
                    Snackbar.make(
                        findViewById(R.id.main_activity_view),
                        R.string.permission_rationale,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok) {
                            // Request permission
                            ActivityCompat.requestPermissions(this,
                                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                                requestCode.ordinal)
                        }
                        .show()
                } else {
                    Log.i(TAG, "Requesting permission")
                    // Request permission. It's possible this can be auto answered if device policy
                    // sets the permission in a given state or the user denied the permission
                    // previously and checked "Never ask again".
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                        requestCode.ordinal)
                }
            }
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                                grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            when {
                grantResults.isEmpty() -> {
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.i(TAG, "User interaction was cancelled.")
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    // Permission was granted.
                    val fitActionRequestCode = FitActionRequestCode.values()[requestCode]
                    fitActionRequestCode.let {
                        fitSignIn(fitActionRequestCode)
                    }
                }
                else -> {
                    // Permission denied.

                    // In this Activity we've chosen to notify the user that they
                    // have rejected a core permission for the app since it makes the Activity useless.
                    // We're communicating this message in a Snackbar since this is a sample app, but
                    // core permissions would typically be best requested during a welcome-screen flow.

                    // Additionally, it is important to remember that a permission might have been
                    // rejected without asking the user for permission (device policy or "Never ask
                    // again" prompts). Therefore, a user interface affordance is typically implemented
                    // when permissions are denied. Otherwise, your app could appear unresponsive to
                    // touches or interactions which have required permissions.

                    Snackbar.make(
                        findViewById(R.id.main_activity_view),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package",
                                BuildConfig.APPLICATION_ID, null)
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }
    }
