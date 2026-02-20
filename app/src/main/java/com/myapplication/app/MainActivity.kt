package com.myapplication.app // KEEP YOUR EXACT PACKAGE NAME

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.SmsManager
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var acceleration by mutableFloatStateOf(0f)
    private var isCountingDown by mutableStateOf(false)
    private var countdownTimer by mutableIntStateOf(10)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()

                // RUNTIME PERMISSIONS LAUNCHER
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val allGranted = permissions.values.all { it }
                    if (!allGranted) {
                        Toast.makeText(this, "Permissions required for SOS to function!", Toast.LENGTH_LONG).show()
                    }
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(arrayOf(
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ))
                }

                NavHost(navController = navController, startDestination = "auth") {
                    composable("auth") { AuthScreen(navController) }
                    composable("contacts") { ContactsScreen(navController, getSharedPreferences("AppPrefs", MODE_PRIVATE)) }
                    composable("alert") {
                        AlertScreen(
                            navController = navController,
                            currentGForce = acceleration,
                            isCountingDown = isCountingDown,
                            timeLeft = countdownTimer,
                            onExecuteSos = { message -> executeSOS(message) },
                            onCancel = { cancelCountdown() }
                        )

                        LaunchedEffect(isCountingDown) {
                            if (isCountingDown) {
                                while (countdownTimer > 0) {
                                    triggerShortVibration()
                                    delay(1000L)
                                    countdownTimer--
                                }
                                if (countdownTimer == 0) {
                                    executeSOS("Automated Fall Detection Alert!")
                                    isCountingDown = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            acceleration = gForce

            if (gForce > 25.0f && !isCountingDown) {
                isCountingDown = true
                countdownTimer = 10
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun cancelCountdown() {
        isCountingDown = false
        countdownTimer = 10
        Toast.makeText(this, "False Alarm Cancelled", Toast.LENGTH_SHORT).show()
    }

    // --- THE REAL SOS ENGINE ---
    private fun executeSOS(emergencyType: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val contacts = listOf(
            prefs.getString("c1Num", "") ?: "",
            prefs.getString("c2Num", "") ?: "",
            prefs.getString("c3Num", "") ?: ""
        ).filter { it.isNotBlank() }

        if (contacts.isEmpty()) {
            Toast.makeText(this, "No contacts saved! Cannot dispatch.", Toast.LENGTH_LONG).show()
            return
        }

        // 1. Get Location
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->

                val locationString = if (location != null) {
                    "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                } else {
                    "Location unavailable."
                }

                val finalMessage = "SOS ALERT: $emergencyType\nLoc: $locationString"

                // 2. Send SMS to all valid contacts
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    for (number in contacts) {
                        try {
                            smsManager.sendTextMessage(number, null, finalMessage, null, null)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    Toast.makeText(this, "SMS Sent to ${contacts.size} contacts.", Toast.LENGTH_SHORT).show()
                }

                // 3. Initiate Phone Call to the Primary Contact (c1Num)
                val primaryNumber = contacts.first()
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$primaryNumber"))
                    startActivity(callIntent)
                }
            }
        } else {
            Toast.makeText(this, "Location permission denied. Cannot send GPS.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun triggerShortVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }
}

// --- SCREENS (Auth, Contacts, Alert) ---
@Composable
fun AuthScreen(navController: NavController) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Two-Way Alert", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Red, modifier = Modifier.padding(bottom = 32.dp))
        Text(if (isLoginMode) "Sign In" else "Create Account", fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = "" },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = errorMessage.contains("email", ignoreCase = true)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = "" },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = errorMessage.contains("Password", ignoreCase = true),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle Password Visibility", tint = Color.Gray)
                }
            }
        )

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
                val isPasswordValid = password.length >= 8 && password.any { !it.isLetterOrDigit() }

                if (!isEmailValid) {
                    errorMessage = "Invalid email. Must contain '@'."
                } else if (!isPasswordValid) {
                    errorMessage = "Password must be 8+ chars & include a special character."
                } else {
                    navController.navigate("contacts")
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (isLoginMode) "Login" else "Register")
        }

        TextButton(onClick = { isLoginMode = !isLoginMode; errorMessage = "" }, modifier = Modifier.padding(top = 16.dp)) {
            Text(if (isLoginMode) "Don't have an account? Register" else "Already have an account? Login", color = Color.Gray)
        }
    }
}

@Composable
fun ContactsScreen(navController: NavController, prefs: android.content.SharedPreferences) {
    val scrollState = rememberScrollState()

    var c1Name by remember { mutableStateOf(prefs.getString("c1Name", "") ?: "") }
    var c1Num by remember { mutableStateOf(prefs.getString("c1Num", "") ?: "") }

    var c2Name by remember { mutableStateOf(prefs.getString("c2Name", "") ?: "") }
    var c2Num by remember { mutableStateOf(prefs.getString("c2Num", "") ?: "") }

    var c3Name by remember { mutableStateOf(prefs.getString("c3Name", "") ?: "") }
    var c3Num by remember { mutableStateOf(prefs.getString("c3Num", "") ?: "") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("Emergency Contacts", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))

        ContactCard("Primary Contact (Will be called)", c1Name, c1Num, { c1Name = it }, { c1Num = it })
        ContactCard("Secondary Contact", c2Name, c2Num, { c2Name = it }, { c2Num = it })
        ContactCard("Tertiary Contact", c3Name, c3Num, { c3Name = it }, { c3Num = it })

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                prefs.edit()
                    .putString("c1Name", c1Name).putString("c1Num", c1Num)
                    .putString("c2Name", c2Name).putString("c2Num", c2Num)
                    .putString("c3Name", c3Name).putString("c3Num", c3Num).apply()
                navController.navigate("alert") { popUpTo("contacts") { inclusive = true } }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Save & Proceed to Dashboard")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ContactCard(title: String, name: String, num: String, onNameChange: (String) -> Unit, onNumChange: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.DarkGray)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.LightGray, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = num, onValueChange = onNumChange, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        }
    }
}

@Composable
fun AlertScreen(navController: NavController, currentGForce: Float, isCountingDown: Boolean, timeLeft: Int, onExecuteSos: (String) -> Unit, onCancel: () -> Unit) {
    var showPresetsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {

        // UPDATED CONTACTS ICON & PADDING
        IconButton(
            onClick = { navController.navigate("contacts") },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 24.dp)
        ) {
            Icon(Icons.Filled.Contacts, contentDescription = "Edit Contacts", tint = Color.Gray, modifier = Modifier.size(32.dp))
        }

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = "Sensor Active", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            Text(text = "G-Force: ${String.format("%.1f", currentGForce)}", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(bottom = 64.dp))

            if (isCountingDown) {
                Text("FALL DETECTED!", color = Color.Red, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                Text("Dispatching SOS in $timeLeft", color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(bottom = 48.dp))
                Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(0.6f).height(60.dp)) {
                    Text("I'M OKAY (CANCEL)", fontSize = 18.sp, color = Color.White)
                }
            } else {
                Button(
                    onClick = { showPresetsDialog = true },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.size(220.dp).shadow(12.dp, CircleShape)
                ) {
                    Text("SOS", fontSize = 56.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        if (showPresetsDialog) {
            AlertDialog(
                onDismissRequest = { showPresetsDialog = false },
                title = { Text("Select Emergency Type") },
                text = {
                    Column {
                        PresetButton("Medical Emergency", onExecuteSos) { showPresetsDialog = false }
                        PresetButton("Theft / Robbery", onExecuteSos) { showPresetsDialog = false }
                        PresetButton("Being Followed / Unsafe", onExecuteSos) { showPresetsDialog = false }
                        PresetButton("General SOS", onExecuteSos) { showPresetsDialog = false }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPresetsDialog = false }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }
    }
}

@Composable
fun PresetButton(message: String, onExecuteSos: (String) -> Unit, closeDialog: () -> Unit) {
    Button(
        onClick = { onExecuteSos(message); closeDialog() },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
    ) {
        Text(message)
    }
}