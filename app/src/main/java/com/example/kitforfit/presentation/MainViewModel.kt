package com.example.kitforfit.presentation

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kitforfit.domain.DataRepository
import com.example.kitforfit.data.model.FitnessData
import com.google.android.gms.fitness.data.DataType

class MainViewModel:  ViewModel() {

    private var fitnessDataFromRepository: DataRepository? = DataRepository()

    val fitnessData: MutableLiveData<FitnessData>
        get() = fitnessDataFromRepository!!.fitnessData

    fun getData(dataType1: DataType, dataType2: DataType, context: Context) {
        fitnessDataFromRepository?.getData(dataType1, dataType2, context)
    }
}