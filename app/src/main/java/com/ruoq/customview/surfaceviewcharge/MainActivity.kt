package com.ruoq.customview.surfaceviewcharge

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ruoq.customview.R

class MainActivity : AppCompatActivity() {
    private lateinit var bubbleView:BubbleViscosity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

         bubbleView = findViewById<BubbleViscosity>(R.id.batteryView)
        bubbleView.setBatteryLevel("78")

    }

    override fun onResume() {
        super.onResume()

    }
}