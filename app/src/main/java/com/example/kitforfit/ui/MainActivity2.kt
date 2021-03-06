package com.example.kitforfit.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.kitforfit.R
import com.example.kitforfit.databinding.OneBinding
import com.example.kitforfit.presentation.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
const val TAG = "KitForFit"
class MainActivity2: AppCompatActivity() {

    private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1

    private val fitnessOptions: FitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
            .build()
    }

    private lateinit var mainViewModel: MainViewModel
    private lateinit var binding : OneBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        binding = DataBindingUtil.setContentView(this, R.layout.one)
        binding.textView1.text = "KIT FOR FIT"
        binding.textView2.text = "Physical fitness is the first requisite of happiness"
        binding.textView3.text = "So, Why Kit For Fit?"
        binding.textView4.text = "Because, it keeps tracks of your daily and weekly fitness details. Stay fit and safe!."
        checkPermissionsAndRun()
        binding.mainViewModel = mainViewModel
        binding.lifecycleOwner = this
        mainViewModel.fitnessData.observe(this, {
            binding.textView1.text = it.name
            binding.textView2.text = it.dailyCount
            binding.textView3.text = it.weekDescription
            binding.textView4.text = it.weekCount
        })

        binding.button1.setOnClickListener {
            getData(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
        }
        binding.button2.setOnClickListener {
            getData(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
        }
        binding.button3.setOnClickListener {
            getData(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
        }
        binding.button4.setOnClickListener {
            getData(DataType.TYPE_MOVE_MINUTES, DataType.AGGREGATE_MOVE_MINUTES)
        }
        binding.button5.setOnClickListener {
            getData(DataType.TYPE_HEART_POINTS, DataType.AGGREGATE_HEART_POINTS)
        }
        binding.button6.setOnClickListener {
            getData(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
        }
    }

    private fun getData(dataType1: DataType, dataType2: DataType) {
        mainViewModel.getData(dataType1, dataType2, this)
    }

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    private fun checkPermissionsAndRun() {
        if (permissionApproved()) {
            fitSignIn()
        } else {
            Log.i(TAG, "You are not logged in successfully.")
        }
    }

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

    private fun fitSignIn() {
        if (oAuthPermissionsApproved()) {
            Log.i(TAG, "Your oAuth permission is handled successfully.")
        } else {
            GoogleSignIn.requestPermissions(
                this,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                getGoogleAccount(),
                fitnessOptions)
        }
    }

    private fun oAuthPermissionsApproved() = GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions)
    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
}