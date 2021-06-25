package com.example.kitforfit

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel:  ViewModel() {

    private var fitnessDataFromRepository: DataRepository? = DataRepository()

    val fitnessData: MutableLiveData<FitnessData>
        get() = fitnessDataFromRepository!!.fitnessData

    fun getStepsData(context: Context) {
        fitnessDataFromRepository?.getStepsData(context)
    }

    fun getCaloriesData(context: Context) {
        fitnessDataFromRepository?.getCaloriesData(context)
    }

    fun getDistanceData(context: Context) {
        fitnessDataFromRepository?.getDistanceData(context)
    }

    fun getMinutesData(context: Context) {
        fitnessDataFromRepository?.getMinutesData(context)
    }

    fun getHeartPointsData(context: Context) {
        fitnessDataFromRepository?.getHeartPointsData(context)
    }

    fun getWeightData(context: Context) {
        fitnessDataFromRepository?.getWeightData(context)
    }
}