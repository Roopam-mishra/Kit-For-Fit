package com.example.kitforfit.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.kitforfit.R

class MainActivity2: AppCompatActivity() {

    lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.one)
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

    }

    fun stepsData() {
        mainViewModel.stepsData()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity2::class.java)
        }
    }

}