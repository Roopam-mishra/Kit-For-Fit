package com.example.kitforfit

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit

class DataRepository {

    var fitnessData = MutableLiveData<FitnessData>()

    fun getStepsData(context: Context) {
        var step = FitnessData("Steps", "Not able to find", "Total no. of steps this week", "Not able to find")
        val fitnessOptions: FitnessOptions by lazy {
            FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                .build()
        }

        val endTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(-1)
        val startTime = endTime.minusWeeks(1)
        val readRequest =
            DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                val buckets = response.buckets
                val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                buckets.reverse()
                var x = 0
                for (i in buckets.indices) {
                    val bucket = buckets[i]
                    val dataSet = bucket.dataSets[0]
                    val dataCount = dataSet.dataPoints.size
                    if (dataCount != 0) {
                        val dataPoint = dataSet.dataPoints[0]
                        val date = formatter.format(
                            Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS)))
                        Log.i(TAG, "$date and $i")
                        if (i == 0) {
                            step.dailyCount = dataPoint.getValue(dataPoint.dataType.fields[0]).asInt().toString()
                        }
                        x += dataPoint.getValue(dataPoint.dataType.fields[0]).asInt()
                    }
                }
                step.weekCount = x.toString()
                fitnessData.value =step
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }
    }

    fun getCaloriesData(context: Context) {
        var calorie = FitnessData("Calories", "Not able to find", "Total Calories expended this week", "Not able to find")
        val fitnessOptions: FitnessOptions by lazy {
            FitnessOptions.builder()
                .addDataType(DataType.TYPE_CALORIES_EXPENDED)
                .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE)
                .build()
        }

        val endTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(-1)
        val startTime = endTime.minusWeeks(1)
        val readRequest =
            DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                val buckets = response.buckets
                val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                buckets.reverse()
                var x = 0f
                for (i in buckets.indices) {
                    val bucket = buckets[i]
                    val dataSet = bucket.dataSets[0]
                    val dataCount = dataSet.dataPoints.size

                    if (dataCount != 0) {
                        val dataPoint = dataSet.dataPoints[0]
                        val date = formatter.format(
                            Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS)))
                        Log.i(TAG, "$date and $i")
                        if (i == 0) {
                            calorie.dailyCount = dataPoint.getValue(dataPoint.dataType.fields[0]).asFloat().toString()
                        }
                        x += dataPoint.getValue(dataPoint.dataType.fields[0]).asFloat()
                    }
                }
                calorie.weekCount = x.toString()
                fitnessData.value = calorie
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }
    }

    fun getDistanceData(context: Context) {
        var distance = FitnessData("Distance", "Not able to find", "Total Distance travelled this week", "Not able to find")
        val fitnessOptions: FitnessOptions by lazy {
            FitnessOptions.builder()
                .addDataType(DataType.TYPE_DISTANCE_DELTA)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
                .build()
        }

        val endTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(-1)
        val startTime = endTime.minusWeeks(1)
        val readRequest =
            DataReadRequest.Builder()
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                val buckets = response.buckets
                val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                buckets.reverse()
                var x = 0f
                for (i in buckets.indices) {
                    val bucket = buckets[i]
                    val dataSet = bucket.dataSets[0]
                    val dataCount = dataSet.dataPoints.size

                    if (dataCount != 0) {
                        val dataPoint = dataSet.dataPoints[0]
                        val date = formatter.format(
                            Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS)))
                        Log.i(TAG, "$date and $i")
                        if (i == 0) {
                            distance.dailyCount = dataPoint.getValue(dataPoint.dataType.fields[0]).asFloat().toString()
                        }
                        x += dataPoint.getValue(dataPoint.dataType.fields[0]).asFloat()
                    }
                }
                distance.weekCount = x.toString()
                fitnessData.value = distance
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }
    }

    fun getMinutesData(context: Context) {
        var minute = FitnessData("Minutes", "Not able to find", "Total Minutes you moved this week", "Not able to find")
        val fitnessOptions: FitnessOptions by lazy {
            FitnessOptions.builder()
                .addDataType(DataType.TYPE_MOVE_MINUTES)
                .addDataType(DataType.AGGREGATE_MOVE_MINUTES, FitnessOptions.ACCESS_WRITE)
                .build()
        }

        val endTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(-1)
        val startTime = endTime.minusWeeks(1)
        val readRequest =
            DataReadRequest.Builder()
                .aggregate(DataType.TYPE_MOVE_MINUTES, DataType.AGGREGATE_MOVE_MINUTES)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                val buckets = response.buckets
                val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                buckets.reverse()
                var x = 0
                for (i in buckets.indices) {
                    val bucket = buckets[i]
                    val dataSet = bucket.dataSets[0]
                    val dataCount = dataSet.dataPoints.size

                    if (dataCount != 0) {
                        val dataPoint = dataSet.dataPoints[0]
                        val date = formatter.format(
                            Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS)))
                        Log.i(TAG, "$date and $i")
                        if (i == 0) {
                            minute.dailyCount = dataPoint.getValue(dataPoint.dataType.fields[0]).asInt().toString()
                        }
                        x += dataPoint.getValue(dataPoint.dataType.fields[0]).asInt()
                    }
                }
                minute.weekCount = x.toString()
                fitnessData.value = minute
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }
    }

    fun getHeartPointsData(context: Context) {
        var heartPoint = FitnessData("Heart Points", "Not able to find", "You earned heart point this week", "Not able to find")
        val fitnessOptions: FitnessOptions by lazy {
            FitnessOptions.builder()
                .addDataType(DataType.TYPE_HEART_POINTS)
                .addDataType(DataType.AGGREGATE_HEART_POINTS, FitnessOptions.ACCESS_WRITE)
                .build()
        }

        val endTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(-1)
        val startTime = endTime.minusWeeks(1)
        val readRequest =
            DataReadRequest.Builder()
                .aggregate(DataType.TYPE_HEART_POINTS, DataType.AGGREGATE_HEART_POINTS)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                val buckets = response.buckets
                val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                buckets.reverse()
                var x = 0f
                for (i in buckets.indices) {
                    val bucket = buckets[i]
                    val dataSet = bucket.dataSets[0]
                    val dataCount = dataSet.dataPoints.size
                    Log.i(TAG, "dataCount = $dataCount")
                    if (dataCount != 0) {
                        val dataPoint = dataSet.dataPoints[0]
                        val date = formatter.format(
                            Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS)))
                        Log.i(TAG, "$date and $i")
                        if (i == 0) {
                            heartPoint.dailyCount = dataPoint.getValue(dataPoint.dataType.fields[0]).asFloat().toString()
                        }
                        x += dataPoint.getValue(dataPoint.dataType.fields[0]).asFloat()
                    }
                    else {
                        heartPoint.dailyCount = 0.0.toString()
                    }
                }
                heartPoint.weekCount = x.toString()
                fitnessData.value = heartPoint
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }
    }

    fun getWeightData(context: Context) {
        var weight = FitnessData("Weight", "Not able to find", "Your this week average weight", "Not able to find")
        val fitnessOptions: FitnessOptions by lazy {
            FitnessOptions.builder()
                .addDataType(DataType.TYPE_WEIGHT)
                .addDataType(DataType.AGGREGATE_WEIGHT_SUMMARY, FitnessOptions.ACCESS_WRITE)
                .build()
        }

        val endTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(-1)
        val startTime = endTime.minusWeeks(1)
        val readRequest =
            DataReadRequest.Builder()
                .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()
        Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                val buckets = response.buckets
                val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                buckets.reverse()
                var x = 0f
                var count = 0
                for (i in buckets.indices) {
                    val bucket = buckets[i]
                    val dataSet = bucket.dataSets[0]
                    val dataCount = dataSet.dataPoints.size

                    if (dataCount != 0) {
                        val dataPoint = dataSet.dataPoints[0]
                        val date = formatter.format(
                            Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS)))
                        Log.i(TAG, "$date and $i")
                        if (i == 0) {
                            weight.dailyCount = dataPoint.getValue(dataPoint.dataType.fields[0]).asFloat().toString()
                        }
                        x += dataPoint.getValue(dataPoint.dataType.fields[0]).asFloat()
                        count ++
                    }
                }
                weight.weekCount = (x/count).toString()
                fitnessData.value = weight
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }
    }

}