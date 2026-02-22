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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val allGranted = permissions.values.all { it }
                    if (!allGranted) Toast.makeText(this, "Permissions required for SOS!", Toast.LENGTH_LONG).show()
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE, Manifest.permission.ACCESS_FINE_LOCATION))
                }

                // Verify Firebase Auth State
                val startScreen = if (FirebaseAuth.getInstance().currentUser != null && prefs.getBoolean("isLoggedIn", false)) "alert" else "auth"

                Scaffold(
                    bottomBar = {
                        if (currentRoute in listOf("profile", "alert", "contacts")) {
                            NavigationBar(containerColor = Color(0xFF1E1E1E)) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") }, label = { Text("Profile") }, selected = currentRoute == "profile",
                                    onClick = { navController.navigate("profile") { launchSingleTop = true } }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Red, selectedTextColor = Color.Red, indicatorColor = Color(0xFF333333))
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") }, label = { Text("Home") }, selected = currentRoute == "alert",
                                    onClick = { navController.navigate("alert") { launchSingleTop = true } }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Red, selectedTextColor = Color.Red, indicatorColor = Color(0xFF333333))
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Filled.Contacts, contentDescription = "Contacts") }, label = { Text("Contacts") }, selected = currentRoute == "contacts",
                                    onClick = { navController.navigate("contacts") { launchSingleTop = true } }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Red, selectedTextColor = Color.Red, indicatorColor = Color(0xFF333333))
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFF121212))) {
                        NavHost(navController = navController, startDestination = startScreen) {
                            composable("auth") { AuthScreen(navController, prefs) }
                            composable("contacts") { ContactsScreen(navController, prefs) }
                            composable("profile") { ProfileScreen(navController, prefs) }
                            composable("alert") {
                                AlertScreen(
                                    currentGForce = acceleration, isCountingDown = isCountingDown, timeLeft = countdownTimer,
                                    onExecuteSos = { message -> executeSOS(message, prefs) }, onCancel = { cancelCountdown() }
                                )

                                LaunchedEffect(isCountingDown) {
                                    if (isCountingDown) {
                                        while (countdownTimer > 0) { triggerShortVibration(); delay(1000L); countdownTimer-- }
                                        if (countdownTimer == 0) { executeSOS("Automated Fall Detection Alert!", prefs); isCountingDown = false }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) } }
    override fun onPause() { super.onPause(); sensorManager.unregisterListener(this) }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
            val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            acceleration = gForce
            if (gForce > 25.0f && !isCountingDown) { isCountingDown = true; countdownTimer = 10 }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun cancelCountdown() {
        isCountingDown = false; countdownTimer = 10
        Toast.makeText(this, "False Alarm Cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun executeSOS(emergencyType: String, prefs: android.content.SharedPreferences) {
        val contacts = listOf(prefs.getString("c1Num", "") ?: "", prefs.getString("c2Num", "") ?: "").filter { it.isNotBlank() }
        if (contacts.isEmpty()) { Toast.makeText(this, "No contacts saved!", Toast.LENGTH_LONG).show(); return }

        val call112 = prefs.getBoolean("call112", false)
        val vitalContext = prefs.getString("vitalContext", "None") ?: "None"

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val locationString = if (location != null) "https://maps.google.com/?q=$${location.latitude},${location.longitude}" else "Location unavailable."
                val finalMessage = "SOS ALERT: $emergencyType\nContext: $vitalContext\nLoc: $locationString"

                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) getSystemService(SmsManager::class.java) else SmsManager.getDefault()
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    for (number in contacts) { try { smsManager.sendTextMessage(number, null, finalMessage, null, null) } catch (e: Exception) { e.printStackTrace() } }
                    Toast.makeText(this, "SMS Sent!", Toast.LENGTH_SHORT).show()
                }

                val numberToCall = if (call112) "112" else contacts.first()
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$numberToCall")))
                }
            }
        }
    }

    private fun triggerShortVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator else @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)) else @Suppress("DEPRECATION") vibrator.vibrate(300)
    }
}

// --- SCREENS ---
@Composable
fun AuthScreen(navController: NavController, prefs: android.content.SharedPreferences) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // Added Loading State

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Two-Way Alert", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Red, modifier = Modifier.padding(bottom = 32.dp))
        Text(if (isLoginMode) "Sign In" else "Create Account", fontSize = 20.sp, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(value = email, onValueChange = { email = it; errorMessage = "" }, label = { Text("Email", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it; errorMessage = "" }, label = { Text("Password", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null, tint = Color.Gray) } }
        )

        if (errorMessage.isNotEmpty()) Text(errorMessage, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Color.Red)
        } else {
            Button(
                onClick = {
                    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    val isPasswordValid = password.length >= 8 && password.any { !it.isLetterOrDigit() }

                    if (!isEmailValid) errorMessage = "Invalid email."
                    else if (!isPasswordValid) errorMessage = "Password needs 8+ chars & 1 special."
                    else {
                        isLoading = true
                        if (isLoginMode) {
                            // FIREBASE LOGIN
                            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = auth.currentUser?.uid
                                    if (uid != null) {
                                        // PULL CLOUD DATA TO LOCAL CACHE
                                        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                                            if (doc.exists()) {
                                                prefs.edit()
                                                    .putString("userName", doc.getString("name") ?: "")
                                                    .putString("userAge", doc.getString("age") ?: "")
                                                    .putString("userSex", doc.getString("sex") ?: "")
                                                    .putString("vitalContext", doc.getString("vitalContext") ?: "None")
                                                    .putBoolean("call112", doc.getBoolean("call112") ?: false)
                                                    .putString("c1Num", doc.getString("c1Num") ?: "")
                                                    .putString("c2Num", doc.getString("c2Num") ?: "")
                                                    .putBoolean("isLoggedIn", true)
                                                    .apply()
                                            } else {
                                                prefs.edit().putBoolean("isLoggedIn", true).apply()
                                            }
                                            navController.navigate("alert") { popUpTo("auth") { inclusive = true } }
                                        }.addOnFailureListener {
                                            prefs.edit().putBoolean("isLoggedIn", true).apply()
                                            navController.navigate("alert") { popUpTo("auth") { inclusive = true } }
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    errorMessage = task.exception?.localizedMessage ?: "Login failed"
                                }
                            }
                        } else {
                            // FIREBASE REGISTER
                            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    prefs.edit().putBoolean("isLoggedIn", true).apply()
                                    navController.navigate("profile") { popUpTo("auth") { inclusive = true } }
                                } else {
                                    isLoading = false
                                    errorMessage = task.exception?.localizedMessage ?: "Registration failed"
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text(if (isLoginMode) "Login" else "Register", color = Color.White) }
        }

        TextButton(onClick = { isLoginMode = !isLoginMode; errorMessage = "" }, modifier = Modifier.padding(top = 16.dp)) {
            Text(if (isLoginMode) "Don't have an account? Register" else "Already have an account? Login", color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, prefs: android.content.SharedPreferences) {
    var name by remember { mutableStateOf(prefs.getString("userName", "") ?: "") }
    var age by remember { mutableStateOf(prefs.getString("userAge", "") ?: "") }
    var sex by remember { mutableStateOf(prefs.getString("userSex", "") ?: "") }

    val contextOptions = listOf("None", "Traveling Alone", "Asthma / Breathing Issues", "Heart Condition", "Mobility Impaired", "Deaf / Hard of Hearing", "Severe Allergies")
    var vitalContext by remember { mutableStateOf(prefs.getString("vitalContext", contextOptions[0]) ?: contextOptions[0]) }
    var expanded by remember { mutableStateOf(false) }
    var call112 by remember { mutableStateOf(prefs.getBoolean("call112", false)) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            TextButton(onClick = {
                // FIREBASE LOGOUT
                FirebaseAuth.getInstance().signOut()
                prefs.edit().clear().apply()
                navController.navigate("auth") { popUpTo(0) }
            }) { Text("Logout", color = Color.Red) }
        }
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = sex, onValueChange = { sex = it }, label = { Text("Sex") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(value = vitalContext, onValueChange = {}, readOnly = true, label = { Text("Vital Context") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(), modifier = Modifier.fillMaxWidth().menuAnchor())
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                contextOptions.forEach { selectionOption ->
                    DropdownMenuItem(text = { Text(selectionOption) }, onClick = { vitalContext = selectionOption; expanded = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dial 112 on SOS", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Texts family, but calls emergency services.", color = Color.Gray, fontSize = 12.sp)
                }
                Switch(checked = call112, onCheckedChange = { call112 = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Red, checkedTrackColor = Color(0xFF5C1C1C)))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                prefs.edit().putString("userName", name).putString("userAge", age).putString("userSex", sex).putString("vitalContext", vitalContext).putBoolean("call112", call112).apply()

                // PUSH TO FIREBASE FIRESTORE
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val userData = hashMapOf("name" to name, "age" to age, "sex" to sex, "vitalContext" to vitalContext, "call112" to call112)
                    FirebaseFirestore.getInstance().collection("users").document(uid).set(userData, SetOptions.merge())
                }
                Toast.makeText(navController.context, "Profile Saved to Cloud", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) { Text("Save Changes") }
    }
}

@Composable
fun ContactsScreen(navController: NavController, prefs: android.content.SharedPreferences) {
    var c1Num by remember { mutableStateOf(prefs.getString("c1Num", "") ?: "") }
    var c2Num by remember { mutableStateOf(prefs.getString("c2Num", "") ?: "") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Emergency Contacts", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 24.dp))
        OutlinedTextField(value = c1Num, onValueChange = { c1Num = it }, label = { Text("Primary Phone Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = c2Num, onValueChange = { c2Num = it }, label = { Text("Secondary Phone Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                prefs.edit().putString("c1Num", c1Num).putString("c2Num", c2Num).apply()

                // PUSH TO FIREBASE FIRESTORE
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val contactData = hashMapOf("c1Num" to c1Num, "c2Num" to c2Num)
                    FirebaseFirestore.getInstance().collection("users").document(uid).set(contactData, SetOptions.merge())
                }
                Toast.makeText(navController.context, "Contacts Synced", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) { Text("Save Contacts") }
    }
}

@Composable
fun AlertScreen(currentGForce: Float, isCountingDown: Boolean, timeLeft: Int, onExecuteSos: (String) -> Unit, onCancel: () -> Unit) {
    var showPresetsDialog by remember { mutableStateOf(false) }
    val progressTarget = (currentGForce / 25f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progressTarget, label = "GForceMeter")
    val meterColor = if (progressTarget > 0.8f) Color.Red else if (progressTarget > 0.4f) Color.Yellow else Color.Green

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Accelerometer Load", color = Color.Gray, fontSize = 14.sp)
            Text("${String.format("%.1f", currentGForce)} G", color = meterColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = animatedProgress, modifier = Modifier.fillMaxWidth(0.6f).height(12.dp).padding(top = 8.dp, bottom = 48.dp).clip(RoundedCornerShape(6.dp)), color = meterColor, trackColor = Color.DarkGray)

            if (isCountingDown) {
                Text("FALL DETECTED!", color = Color.Red, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                Text("Dispatching SOS in $timeLeft", color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(bottom = 48.dp))
                Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(0.6f).height(60.dp)) { Text("I'M OKAY (CANCEL)", fontSize = 18.sp, color = Color.White) }
            } else {
                Button(onClick = { showPresetsDialog = true }, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), modifier = Modifier.size(220.dp).shadow(12.dp, CircleShape)) { Text("SOS", fontSize = 56.sp, color = Color.White, fontWeight = FontWeight.ExtraBold) }
            }
        }

        if (showPresetsDialog) {
            AlertDialog(
                onDismissRequest = { showPresetsDialog = false }, title = { Text("Select Emergency Type") },
                text = { Column { PresetButton("Medical Emergency", onExecuteSos) { showPresetsDialog = false } ; PresetButton("Theft / Robbery", onExecuteSos) { showPresetsDialog = false } ; PresetButton("General SOS", onExecuteSos) { showPresetsDialog = false } } },
                confirmButton = { TextButton(onClick = { showPresetsDialog = false }) { Text("Cancel", color = Color.Gray) } }
            )
        }
    }
}

@Composable
fun PresetButton(message: String, onExecuteSos: (String) -> Unit, closeDialog: () -> Unit) { Button(onClick = { onExecuteSos(message); closeDialog() }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))) { Text(message) } }