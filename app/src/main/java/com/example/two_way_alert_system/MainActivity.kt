package com.example.two_way_alert_system

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.two_way_alert_system.databinding.ActivityMainBinding // 1. Make sure you're using View Binding

class MainActivity : AppCompatActivity() {

    // 2. Declare FallDetector and ViewBinding variables
    private lateinit var fallDetector: FallDetector
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 3. Set up View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 4. Initialize the FallDetector
        fallDetector = FallDetector(this) {
            // This is the code that runs when a fall is detected
            Log.d("FallDetection", "FALL DETECTED!")

            // To see the result on screen, show a Toast message.
            // runOnUiThread is important because sensor events can come from a background thread.
            runOnUiThread {
                Toast.makeText(applicationContext, "Fall Detected!", Toast.LENGTH_LONG).show()

                // You can also update a TextView to show the status
                // Make sure you have a TextView with id "statusTextView" in your activity_main.xml
                // binding.statusTextView.text = "Status: Fall Detected!"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 5. Start the detector when the app becomes active
        fallDetector.start()
        Log.d("FallDetection", "Fall detector started.")
    }

    override fun onPause() {
        super.onPause()
        // 6. Stop the detector when the app is paused to save battery
        fallDetector.stop()
        Log.d("FallDetection", "Fall detector stopped.")
    }
}
