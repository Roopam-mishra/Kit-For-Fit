package com.example.kitforfit

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import kotlinx.coroutines.*
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
import kotlinx.coroutines.launch as launch

class MainActivity2: AppCompatActivity() {

    private val fitnessOptions: FitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)  //TYPE_STEP_COUNT_DELTA
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE)  // FIELD_CALORIES
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE) //FIELD_DISTANCE
            .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_MOVE_MINUTES, FitnessOptions.ACCESS_WRITE)  // FIELD_DURATION
            .addDataType(DataType.AGGREGATE_MOVE_MINUTES, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_HEART_POINTS, FitnessOptions.ACCESS_WRITE)  //FIELD_INTENSITY
            .addDataType(DataType.AGGREGATE_HEART_POINTS, FitnessOptions.ACCESS_WRITE) //FIELD_INTENSITY FIELD_DURATION
            .addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_WRITE) //FIELD_WEIGHT
            .addDataType(DataType.AGGREGATE_WEIGHT_SUMMARY, FitnessOptions.ACCESS_WRITE)//FIELD_AVERAGE FIELD_MAX FIELD_MIN
            .build()
    }

    private val runningQOrLater =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.one)
        checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)
        val stepsButton :Button = findViewById(R.id.button1)
        stepsButton.setOnClickListener {
            readData(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
        }
        val caloriesButton: Button = findViewById(R.id.button2)
        caloriesButton.setOnClickListener {
            readData(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
        }
        val distanceButton :Button = findViewById(R.id.button3)
        distanceButton.setOnClickListener {
            readData(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
        }
        val minutesButton: Button = findViewById(R.id.button4)
        minutesButton.setOnClickListener {
            readData(DataType.TYPE_MOVE_MINUTES, DataType.AGGREGATE_MOVE_MINUTES)
        }
        val heartPtsButton :Button = findViewById(R.id.button5)
        heartPtsButton.setOnClickListener {
            readData(DataType.TYPE_HEART_POINTS, DataType.AGGREGATE_HEART_POINTS)
        }
        val weightButton: Button = findViewById(R.id.button6)
        weightButton.setOnClickListener {
            readData(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
        }
    }


    private fun performActionForRequestCode(requestCode: FitActionRequestCode, dataType: DataType, dataType2: DataType) = when (requestCode) {
        FitActionRequestCode.READ_DATA -> readData(dataType, dataType2)
        FitActionRequestCode.SUBSCRIBE -> subscribe()
        else -> TODO()
    }

    @SuppressLint("SetTextI18n")
    private fun readData(dataType: DataType, dataType2: DataType) {
        if(dataType == DataType.TYPE_STEP_COUNT_DELTA){
            Fitness.getHistoryClient(this, getGoogleAccount())
                .readDailyTotal(dataType)
                .addOnSuccessListener { dataSet ->
                    val total = when {
                        dataSet.isEmpty -> 0
                        else -> dataSet.dataPoints.first().getValue(Field.FIELD_STEPS).asInt()
                    }
                    Log.i(TAG, "Total steps: $total")
                    var textView: TextView = findViewById(R.id.text_view1)
                    textView.text= getString(R.string.steps)
                    textView = findViewById(R.id.text_view2)
                    textView.text= "$total"
                    textView = findViewById(R.id.text_view3)
                    textView.text= getString(R.string.last_week_total_steps)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "There was a problem getting the step count.", e)
                }
            val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
            val startTime = endTime.minusWeeks(1)
            val readRequest =
                DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                    .bucketByActivityType(1, TimeUnit.DAYS)
                    .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0
                    if (dataReadResult.buckets.isNotEmpty()) {
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    x += 0
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        for (field in dp.dataType.fields) {
                                            x += dp.getValue(field).asInt()
                                        }
                                    }
                                }
                            }
                        }
                    } else if (dataReadResult.dataSets.isNotEmpty()) {
                        dataReadResult.dataSets.forEach { dataSet ->
                            if(dataSet.isEmpty) {
                                x += 0
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    for (field in dp.dataType.fields) {
                                        x += dp.getValue(field).asInt()
                                    }
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.text_view4)
                    textView.text = "${x}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

        }
        else if(dataType == DataType.TYPE_CALORIES_EXPENDED){
            var endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
            var startTime = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault())
            var readRequest =
                DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                    .bucketByActivityType(1, TimeUnit.HOURS)
                    .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0f
                    if (dataReadResult.buckets.isNotEmpty()) {
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    x += 0f
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        for (field in dp.dataType.fields) {
                                            x += dp.getValue(field).asFloat()
                                        }
                                    }
                                }
                            }
                        }
                    } else if (dataReadResult.dataSets.isNotEmpty()) {
                        dataReadResult.dataSets.forEach { dataSet ->
                            if(dataSet.isEmpty) {
                                x += 0f
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    for (field in dp.dataType.fields) {
                                        x += dp.getValue(field).asFloat()
                                    }
                                }
                            }
                        }
                    }
                    var textView: TextView = findViewById(R.id.text_view1)
                    textView.text = "Calories"
                    textView = findViewById(R.id.text_view2)
                    textView.text = "${x} kcal"
                    textView = findViewById(R.id.text_view3)
                    textView.text = "Total calories expended this week"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
            startTime = endTime.minusWeeks(1)
            readRequest =
                DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                    .bucketByActivityType(1, TimeUnit.HOURS)
                    .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0f
                    if (dataReadResult.buckets.isNotEmpty()) {
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    x += 0f
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        for (field in dp.dataType.fields) {
                                            x += dp.getValue(field).asFloat()
                                        }
                                    }
                                }
                            }
                        }
                    } else if (dataReadResult.dataSets.isNotEmpty()) {
                        dataReadResult.dataSets.forEach { dataSet ->
                            if(dataSet.isEmpty) {
                                x += 0f
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    for (field in dp.dataType.fields) {
                                        x += dp.getValue(field).asFloat()
                                    }
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.text_view4)
                    textView.text = "${x} kcal"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

        }
        else if(dataType == DataType.TYPE_DISTANCE_DELTA) {
            var endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
            var startTime = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault())
            var readRequest =
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
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    x += 0f
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        for (field in dp.dataType.fields) {
                                            x += dp.getValue(field).asFloat()
                                        }
                                    }
                                }
                            }
                        }
                    } else if (dataReadResult.dataSets.isNotEmpty()) {
                        dataReadResult.dataSets.forEach { dataSet ->
                            if(dataSet.isEmpty) {
                                x += 0f
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    for (field in dp.dataType.fields) {
                                        x += dp.getValue(field).asFloat()
                                    }
                                }
                            }
                        }
                    }
                    var textView: TextView = findViewById(R.id.text_view1)
                    textView.text = "Distance"
                    textView = findViewById(R.id.text_view2)
                    textView.text = "${x} m"
                    textView = findViewById(R.id.text_view3)
                    textView.text = "Total distance travelled this week"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
            startTime = endTime.minusWeeks(1)
            readRequest =
                DataReadRequest.Builder()
                    .aggregate(dataType, dataType2)
                    .bucketByActivityType(1, TimeUnit.HOURS)
                    .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0f
                    if (dataReadResult.buckets.isNotEmpty()) {
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    x += 0f
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        for (field in dp.dataType.fields) {
                                            x += dp.getValue(field).asFloat()
                                        }
                                    }
                                }
                            }
                        }
                    } else if (dataReadResult.dataSets.isNotEmpty()) {
                        dataReadResult.dataSets.forEach { dataSet ->
                            if(dataSet.isEmpty) {
                                x += 0f
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    for (field in dp.dataType.fields) {
                                        x += dp.getValue(field).asFloat()
                                    }
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.text_view4)
                    textView.text = "${x} m"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }
        }
        else if(dataType == DataType.TYPE_MOVE_MINUTES) {
            var endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
            var startTime = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault())
            var readRequest =
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
                            x += 0
                        } else {
                            for (dp in dataSet.dataPoints) {
                                for (field in dp.dataType.fields) {
                                    x += dp.getValue(field).asInt()
                                }
                            }
                        }
                    }
                    var textView: TextView = findViewById(R.id.text_view1)
                    textView.text = "Minutes"
                    textView = findViewById(R.id.text_view2)
                    textView.text = "${x} mins"
                    textView = findViewById(R.id.text_view3)
                    textView.text = "Total minutes you moved this week"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
            startTime = endTime.minusWeeks(1)
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
                            x += 0
                        } else {
                            for (dp in dataSet.dataPoints) {
                                for (field in dp.dataType.fields) {
                                    x += dp.getValue(field).asInt()
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.text_view4)
                    textView.text = "${x}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }
        }
        else if(dataType == DataType.TYPE_HEART_POINTS){
            var endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
            var startTime = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault())
            var readRequest =
                DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_HEART_POINTS)
                    .bucketByActivityType(1, TimeUnit.SECONDS)
                    .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0f
                    if (dataReadResult.buckets.isNotEmpty()) {
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    x += 0f
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        for (field in dp.dataType.fields) {
                                            if(field.name.toString() == "intensity") {
                                                x += dp.getValue(field).asFloat()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (dataReadResult.dataSets.isNotEmpty()) {
                        dataReadResult.dataSets.forEach { dataSet ->
                            if(dataSet.isEmpty) {
                                x += 0
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    for (field in dp.dataType.fields) {
                                        if(field.name.toString() == "intensity") {
                                            x += dp.getValue(field).asFloat()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    var textView: TextView = findViewById(R.id.text_view1)
                    textView.text = "Heart Points"
                    textView = findViewById(R.id.text_view2)
                    textView.text = "${x}"
                    textView = findViewById(R.id.text_view3)
                    textView.text = "You earned heart points this week"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
            startTime = endTime.minusWeeks(1)
            readRequest =
                DataReadRequest.Builder()
                    .aggregate(dataType, dataType2)
                    .bucketByActivityType(1, TimeUnit.SECONDS)
                    .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0f
                    if (dataReadResult.buckets.isNotEmpty()) {
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    x += 0f
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        for (field in dp.dataType.fields) {
                                            if(field.name.toString() == "intensity") {
                                                x += dp.getValue(field).asFloat()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (dataReadResult.dataSets.isNotEmpty()) {
                        dataReadResult.dataSets.forEach { dataSet ->
                            if(dataSet.isEmpty) {
                                x += 0f
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    for (field in dp.dataType.fields) {
                                        if(field.name.toString() == "intensity") {
                                            x += dp.getValue(field).asFloat()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.text_view4)
                    textView.text = "${x}"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }
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

        }
        else if(dataType == DataType.TYPE_WEIGHT) {
            val calendar = Calendar.getInstance()
            var readRequest =
                DataReadRequest.Builder()
                    .read(DataType.TYPE_WEIGHT)
                    .setTimeRange(1, calendar.timeInMillis, TimeUnit.MILLISECONDS)
                    .setLimit(1)
                    .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0f
                    if (dataReadResult.buckets.isNotEmpty()) {
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    x += 0f
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        for (field in dp.dataType.fields) {
                                            x += dp.getValue(field).asFloat()
                                        }
                                    }
                                }
                            }
                        }
                    } else if (dataReadResult.dataSets.isNotEmpty()) {
                        dataReadResult.dataSets.forEach { dataSet ->
                            if(dataSet.isEmpty) {
                                x += 0f
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    for (field in dp.dataType.fields) {
                                        x += dp.getValue(field).asFloat()
                                    }
                                }
                            }
                        }
                    }
                    var textView: TextView = findViewById(R.id.text_view1)
                    textView.text = "Weight"
                    textView = findViewById(R.id.text_view2)
                    if(x == 0f){
                        textView.text = "Not Recorded"
                    } else {
                        textView.text = "${x} kg"
                    }
                    textView = findViewById(R.id.text_view3)
                    textView.text = "Your this week avg weight"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }

            val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
            val startTime = endTime.minusWeeks(1)
            readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
            Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResult ->
                    var x = 0f
                    var count = 0
                    if (dataReadResult.buckets.isNotEmpty()) {
                        for (bucket in dataReadResult.buckets) {
                            bucket.dataSets.forEach { dataSet ->
                                if(dataSet.isEmpty) {
                                    x += 0f
                                } else {
                                    for (dp in dataSet.dataPoints) {
                                        for (field in dp.dataType.fields) {
                                            if(field.name.toString() == "average"){
                                                x += dp.getValue(field).asFloat()
                                                count++
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (dataReadResult.dataSets.isNotEmpty()) {
                        dataReadResult.dataSets.forEach { dataSet ->
                            if(dataSet.isEmpty) {
                                x += 0f
                            } else {
                                for (dp in dataSet.dataPoints) {
                                    for (field in dp.dataType.fields) {
                                        if(field.name.toString() == "average"){
                                            x += dp.getValue(field).asFloat()
                                            count++
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val textView: TextView = findViewById(R.id.text_view4)
                    textView.text = "${x/count} kg"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error reading data from Google Fit", e)
                }
        }
    }
    private fun DataPoint.getStartTimeString() = Instant.ofEpochSecond(this.getStartTime(TimeUnit.SECONDS))
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime().toString()
    private fun DataPoint.getEndTimeString() = Instant.ofEpochSecond(this.getEndTime(TimeUnit.SECONDS))
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime().toString()
    private fun checkPermissionsAndRun(fitActionRequestCode: FitActionRequestCode) {
        if (permissionApproved()) {
            fitSignIn(fitActionRequestCode)
        } else {
            requestRuntimePermissions(fitActionRequestCode)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (resultCode) {
            RESULT_OK -> {
                val postSignInAction = FitActionRequestCode.values()[requestCode]
                postSignInAction.let {
                    performActionForRequestCode(postSignInAction, DataType.TYPE_HEART_POINTS, DataType.AGGREGATE_HEART_POINTS)
                }
            }
            else -> oAuthErrorMsg(requestCode, resultCode)
        }
    }
    private fun fitSignIn(requestCode:FitActionRequestCode ) {
        if (oAuthPermissionsApproved()) {
            performActionForRequestCode(requestCode, DataType.TYPE_HEART_POINTS, DataType.AGGREGATE_HEART_POINTS)
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
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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