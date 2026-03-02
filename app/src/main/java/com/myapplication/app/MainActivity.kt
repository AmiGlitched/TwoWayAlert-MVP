package com.myapplication.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    // Global state so the background service can trigger the UI
    private var isCountingDown by mutableStateOf(false)
    private var countdownTimer by mutableIntStateOf(10)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FORCE SCREEN TO WAKE UP AND BYPASS LOCK SCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        // Check if the app was violently woken up by the background service
        handleIntent(intent, prefs)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()
                // FIRE ALARM NAVIGATION OVERRIDE
                LaunchedEffect(isCountingDown) {
                    if (isCountingDown && navController.currentDestination?.route != "alert") {
                        navController.navigate("alert") {
                            popUpTo(0) // Wipes the back history so they can't accidentally exit
                            launchSingleTop = true
                        }
                    }
                }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val allGranted = permissions.values.all { it }
                    if (!allGranted) Toast.makeText(this, "Permissions required for SOS!", Toast.LENGTH_LONG).show()
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
                    )
                }

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
                                    isCountingDown = isCountingDown, timeLeft = countdownTimer,
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

    // This catches intents if the app is already open in the background
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent, getSharedPreferences("AppPrefs", MODE_PRIVATE))
    }

    private fun handleIntent(intent: Intent, prefs: android.content.SharedPreferences) {
        if (intent.getBooleanExtra("FALL_DETECTED", false)) {
            isCountingDown = true
            countdownTimer = 10
        }
        if (intent.getBooleanExtra("MANUAL_SOS_TRIGGERED", false)) {
            executeSOS("Manual Notification SOS Triggered!", prefs)
        }
    }

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
    var isLoading by remember { mutableStateOf(false) }

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
                            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = auth.currentUser?.uid
                                    if (uid != null) {
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
                                            } else { prefs.edit().putBoolean("isLoggedIn", true).apply() }
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

    // THE TWO NEW SEPARATE TOGGLES
    var isSosNotificationRunning by remember { mutableStateOf(prefs.getBoolean("isSosNotificationRunning", false)) }
    var isFallDetectionRunning by remember { mutableStateOf(prefs.getBoolean("isFallDetectionRunning", false)) }
    var showPermissionGuide by remember { mutableStateOf(false) }

    fun updateBackgroundService() {
        val serviceIntent = Intent(navController.context, SosForegroundService::class.java).apply {
            putExtra("ENABLE_FALL_DETECTION", isFallDetectionRunning)
        }
        if (isSosNotificationRunning || isFallDetectionRunning) {
            ContextCompat.startForegroundService(navController.context, serviceIntent)
        } else {
            navController.context.stopService(serviceIntent)
        }
    }

    // THE PERMISSION TUTORIAL POPUP
    if (showPermissionGuide) {
        AlertDialog(
            onDismissRequest = { showPermissionGuide = false },
            title = { Text("Setup Required", fontWeight = FontWeight.Bold) },
            text = {
                Text("To automatically wake up your phone during a fall, Android requires a special permission.\n\n1. We will open Settings.\n2. Find 'Two Way Alert' in the list.\n3. Turn ON 'Allow display over other apps'.\n4. Press back to return here.")
            },
            confirmButton = {
                Button(onClick = {
                    showPermissionGuide = false
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${navController.context.packageName}"))
                    navController.context.startActivity(intent)
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionGuide = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color(0xFF1E1E1E), textContentColor = Color.White, titleContentColor = Color.White
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            TextButton(onClick = {
                FirebaseAuth.getInstance().signOut()
                prefs.edit().clear().apply()
                navController.context.stopService(Intent(navController.context, SosForegroundService::class.java))
                navController.navigate("auth") { popUpTo(0) }
            }) { Text("Logout", color = Color.Red) }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // TOGGLE 1: Persistent SOS Button (No sensor, no special permissions needed)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), modifier = Modifier.fillMaxWidth().shadow(4.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Quick-Access SOS Button", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Pins an SOS button to your notifications for instant access.", color = Color.Gray, fontSize = 12.sp)
                }
                Switch(
                    checked = isSosNotificationRunning,
                    onCheckedChange = {
                        isSosNotificationRunning = it
                        prefs.edit().putBoolean("isSosNotificationRunning", it).apply()
                        updateBackgroundService()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Red, checkedTrackColor = Color(0xFF5C1C1C))
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // TOGGLE 2: Fall Detection (Uses sensor, needs Overlay permission)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), modifier = Modifier.fillMaxWidth().shadow(4.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("24/7 Fall Detection", fontWeight = FontWeight.Bold, color = Color.Green)
                    Text("Monitors for hard drops even when the app is completely closed.", color = Color.Gray, fontSize = 12.sp)
                }
                Switch(
                    checked = isFallDetectionRunning,
                    onCheckedChange = { isChecked ->
                        if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(navController.context)) {
                            showPermissionGuide = true // Trigger the tutorial popup
                        } else {
                            isFallDetectionRunning = isChecked
                            prefs.edit().putBoolean("isFallDetectionRunning", isChecked).apply()
                            updateBackgroundService()
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Green, checkedTrackColor = Color(0xFF1B5E20))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // User Data Fields
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
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val userData = hashMapOf("name" to name, "age" to age, "sex" to sex, "vitalContext" to vitalContext, "call112" to call112)
                    FirebaseFirestore.getInstance().collection("users").document(uid).set(userData, SetOptions.merge())
                }
                Toast.makeText(navController.context, "Profile Saved. Go to Contacts next!", Toast.LENGTH_LONG).show()
                navController.navigate("contacts") // Forces the onboarding flow naturally
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) { Text("Save & Next ->", color = Color.White) }
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
fun AlertScreen(isCountingDown: Boolean, timeLeft: Int, onExecuteSos: (String) -> Unit, onCancel: () -> Unit) {
    var showPresetsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {

            if (isCountingDown) {
                Text("FALL DETECTED!", color = Color.Red, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                Text("Dispatching SOS in $timeLeft", color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(bottom = 48.dp))
                Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(0.6f).height(60.dp)) { Text("I'M OKAY (CANCEL)", fontSize = 18.sp, color = Color.White) }
            } else {
                Text("System Ready", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 32.dp))
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