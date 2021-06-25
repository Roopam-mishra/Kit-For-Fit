package com.example.kitforfit

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field

class DataRepository {

//    var stepsData = MutableLiveData<FitnessData>()

    fun getStepsData(context: Context, stepsData: MutableLiveData<FitnessData>) {
        val step = FitnessData()
        val fitnessOptions: FitnessOptions by lazy {
            FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                .build()
        }

        fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(context, fitnessOptions)

        Fitness.getHistoryClient(context, getGoogleAccount())
            .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener { dataSet ->
                val total = when {
                    dataSet.isEmpty -> 0
                    else -> dataSet.dataPoints.first().getValue(Field.FIELD_STEPS).asInt()
                }
                Log.i(TAG, "Total steps: $total")
                step.name = "Steps"
                step.stepsCount = total
                stepsData.value = step
                Log.i(TAG, "name: ${stepsData.value?.name.toString()}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was a problem getting the step count.", e)
            }
    }
}