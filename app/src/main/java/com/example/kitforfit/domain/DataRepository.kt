package com.example.kitforfit.domain

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.kitforfit.data.model.FitnessData
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
const val TAG = "KitForFit"

class DataRepository {

    var fitnessData = MutableLiveData<FitnessData>()

    fun getData(dataType1: DataType, dataType2: DataType, context: Context) {
        val name = dataType1.name.toString().split(".")[2].replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.getDefault()
            ) else it.toString()
        }
        Log.i(TAG,name)
        val data = FitnessData(name, "Not able to find", "Total no. of $name this week", "Not able to find")
        val fitnessOptions: FitnessOptions by lazy {
            FitnessOptions.builder()
                .addDataType(dataType1, FitnessOptions.ACCESS_WRITE)
                .addDataType(dataType2, FitnessOptions.ACCESS_WRITE)
                .build()
        }

        val endTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(-1)
        val startTime = endTime.minusWeeks(1)
        val readRequest =
            DataReadRequest.Builder()
                .aggregate(dataType1, dataType2)
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
                            data.dailyCount = dataPoint.getValue(dataPoint.dataType.fields[0]).toString()
                        }
                        x += dataPoint.getValue(dataPoint.dataType.fields[0]).toString().toFloat()
                        count++
                    }
                    else if(i == 0){
                        if(dataType1 == DataType.TYPE_WEIGHT){
                            data.dailyCount = "Not found any weight details"
                        }
                        else data.dailyCount = 0.0.toString()
                    }
                }
                if(dataType1 == DataType.TYPE_WEIGHT){
                    if(count == 0){
                        data.weekCount = "Not found any weight details"

                    } else {
                        data.weekCount = (x/count).toString()
                    }
                    data.weekDescription = "Your average weight last week."
                } else if(dataType1 == DataType.TYPE_CALORIES_EXPENDED){
                    data.weekDescription = "Total calories expended this week."
                } else if(dataType1 == DataType.TYPE_DISTANCE_DELTA) {
                    data.weekDescription = "Total Distance travelled this week."
                } else {
                    data.weekCount = x.toString()
                }
                fitnessData.value = data
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"There was an error reading data from Google Fit", e)
            }
    }
}