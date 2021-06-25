package com.example.kitforfit

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel:  ViewModel() {

//    private var stepsData: DataRepository? = DataRepository()

    var fitnessData = MutableLiveData<FitnessData>()

    fun getStepsData(context: Context) {
        DataRepository().getStepsData(context, fitnessData)
    }

}