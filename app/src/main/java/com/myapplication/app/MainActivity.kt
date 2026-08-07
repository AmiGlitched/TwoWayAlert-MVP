package com.myapplication.app

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
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Patterns
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import com.myapplication.app.model.Contact
import com.myapplication.app.model.SosHistoryEntry
import com.myapplication.app.ui.components.FeatureToggleTile
import com.myapplication.app.ui.components.FloatingGlassNavBar
import com.myapplication.app.ui.components.GlassPanel
import com.myapplication.app.ui.theme.AccentAmber
import com.myapplication.app.ui.theme.AccentGreen
import com.myapplication.app.ui.theme.AccentRed
import com.myapplication.app.ui.theme.CanvasDeep
import com.myapplication.app.ui.theme.CanvasElevated
import com.myapplication.app.ui.theme.GlassFillLight
import com.myapplication.app.ui.theme.TextPrimary
import com.myapplication.app.ui.theme.TextSecondary
import com.myapplication.app.ui.theme.TwoWayAlertTheme
import com.myapplication.app.utils.ContactStore
import com.myapplication.app.utils.HistoryStore
import com.myapplication.app.utils.PREFS_KEY_COMFORT_MODE
import com.myapplication.app.utils.ScaledUi
import com.myapplication.app.utils.SirenTorch
import com.myapplication.app.utils.UiScale
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {

    // Global state so the background service can trigger the UI
    private var isCountingDown by mutableStateOf(false)
    private var countdownTimer by mutableIntStateOf(10)

    private var pendingEmergencyType by mutableStateOf("Automated Fall Detection Alert!")
    //////
    private var predictionResult by mutableStateOf("")

    /////


    // volume-button triple-click trigger tracking
    private val volumePressTimestamps = mutableListOf<Long>()

    private var comfortModeEnabled by mutableStateOf(false)


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

        comfortModeEnabled = prefs.getBoolean(PREFS_KEY_COMFORT_MODE, false)

        // NEW: Handle intent extras for prediction
        val result = intent.getStringExtra("PREDICTION_RESULT")
        if (result != null) {
            predictionResult = result
        }


        ///
        // Check if the app was violently woken up by the background service
        handleIntent(intent, prefs)

        setContent {
            TwoWayAlertTheme {
                ScaledUi(uiScale = if (comfortModeEnabled) UiScale.COMFORT else UiScale.NORMAL) {
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
                        val permsToRequest = mutableListOf(
                            Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_CONTACTS
                        )
                        // POST_NOTIFICATIONS only exists as a runtime permission from API 33 (Tiramisu) onward;
                        // requesting it on older versions is a no-op at best and can trip lint/strict checks
                        if (Build.VERSION.SDK_INT >= 33) permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) permsToRequest.add(Manifest.permission.CAMERA)
                        permissionLauncher.launch(permsToRequest.toTypedArray())
                    }

                    // Firebase already persists the session on-device indefinitely (until sign-out),
                    // so that alone is the source of truth. Relying on isLoggedIn too meant a user with
                    // a perfectly valid Firebase session could get bounced back to the register screen
                    // if local prefs ever got out of sync (partial data clear, restore from backup, etc).
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null && !prefs.getBoolean("isLoggedIn", false)) {
                        // session is valid but local flag lagged behind — repair it instead of logging them out
                        prefs.edit().putBoolean("isLoggedIn", true).apply()
                    }

                    // Contacts are mandatory unless the person has explicitly opted into 112-only mode.
                    // Recomputed on every recomposition so adding a contact or flipping the toggle
                    // immediately clears the gate without needing an app restart.
                    val hasAnyContact = remember(currentRoute) { ContactStore.load(prefs).isNotEmpty() }
                    val call112Only = prefs.getBoolean("call112", false)
                    val contactsRequirementMet = hasAnyContact || call112Only

                    val startScreen = when {
                        !prefs.getBoolean("hasSeenOnboarding", false) -> "onboarding"
                        !prefs.getBoolean("hasCompletedPreSetup", false) -> "preSetup"
                        firebaseUser == null -> "auth"
                        !contactsRequirementMet -> "contacts"
                        else -> "alert"
                    }

                    Scaffold(
                        topBar = {
                            if (currentRoute in listOf("alert", "contacts", "history", "alertFeatures")) {
                                TopAppBar(
                                    title = { Text("Two-Way Alert", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                                    navigationIcon = {
                                        IconButton(onClick = { navController.navigate("profile") { launchSingleTop = true } }) {
                                            Icon(
                                                Icons.Outlined.Person, contentDescription = "Profile",
                                                tint = if (currentRoute == "profile") AccentRed else TextSecondary
                                            )
                                        }
                                    },
                                    actions = { SystemActiveBadge(modifier = Modifier.padding(end = 12.dp)) },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasDeep)
                                )
                            }
                        },
                        containerColor = CanvasDeep
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            // Enforce the mandatory-contact rule: can't reach Home or History until at
                            // least one contact is saved, unless 112-only mode is on. Contacts, Alert
                            // Features (where the 112-only toggle lives), and Profile stay reachable.
                            LaunchedEffect(currentRoute, contactsRequirementMet) {
                                if (!contactsRequirementMet && currentRoute in listOf("alert", "history")) {
                                    navController.navigate("contacts") { launchSingleTop = true }
                                }
                            }

                            // subtle accent-color wash behind the home screen only, ties the whole main
                            // screen to the alarm-red identity without recoloring every other screen
                            if (currentRoute == "alert") {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(
                                        Brush.radialGradient(
                                            colors = listOf(AccentRed.copy(alpha = 0.14f), CanvasDeep),
                                            radius = 1400f
                                        )
                                    )
                                )
                            }

                            // order used to pick a left/right slide direction between the 4 main tabs,
                            // so moving Home -> Contacts slides one way and Contacts -> Home slides back
                            val tabOrder = listOf("alert", "alertFeatures", "contacts", "history")
                            fun tabIndex(route: String?) = tabOrder.indexOf(route).let { if (it == -1) 0 else it }

                            NavHost(
                                navController = navController, startDestination = startScreen,
                                enterTransition = {
                                    val fromIdx = tabIndex(initialState.destination.route)
                                    val toIdx = tabIndex(targetState.destination.route)
                                    slideInHorizontally(animationSpec = tween(280)) { width -> if (toIdx >= fromIdx) width else -width } + fadeIn(tween(200))
                                },
                                exitTransition = {
                                    val fromIdx = tabIndex(initialState.destination.route)
                                    val toIdx = tabIndex(targetState.destination.route)
                                    slideOutHorizontally(animationSpec = tween(280)) { width -> if (toIdx >= fromIdx) -width else width } + fadeOut(tween(200))
                                }
                            ) {
                                composable("onboarding") {
                                    OnboardingScreen(prefs) {
                                        navController.navigate("preSetup") { popUpTo("onboarding") { inclusive = true } }
                                    }
                                }
                                composable("preSetup") {
                                    PreSetupScreen(navController, prefs) {
                                        val dest = if (FirebaseAuth.getInstance().currentUser != null) "alert" else "auth"
                                        navController.navigate(dest) { popUpTo("preSetup") { inclusive = true } }
                                    }
                                }
                                composable("auth") { AuthScreen(navController, prefs) }
                                composable("contacts") { ContactsScreen(navController, prefs) }
                                composable("profile") { ProfileScreen(navController, prefs) }
                                composable("alertFeatures") {
                                    AlertFeaturesScreen(navController, prefs, comfortModeEnabled) { enabled ->
                                        comfortModeEnabled = enabled
                                    }
                                }
                                composable("history") { HistoryScreen(prefs) }
                                composable("alert") {
                                    AlertScreen(
                                        isCountingDown = isCountingDown, timeLeft = countdownTimer, predictionResult = predictionResult,   //
                                        onExecuteSos = { message -> executeSOS(message, prefs) }, onCancel = { cancelCountdown() }
                                    )

                                    LaunchedEffect(isCountingDown) {
                                        if (isCountingDown) {
                                            while (countdownTimer > 0) { triggerShortVibration(); delay(1000L); countdownTimer-- }
                                            if (countdownTimer == 0) { executeSOS(pendingEmergencyType, prefs); isCountingDown = false }
                                        }
                                    }
                                }
                            }

                            if (currentRoute in listOf("alert", "contacts", "history", "alertFeatures")) {
                                FloatingGlassNavBar(
                                    currentRoute = currentRoute,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
                                    onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    // ðŸ”˜ Volume-button triple-click trigger: 3 presses of either volume key within 1.5s fires manual SOS.
    // works even when the screen is locked since KeyEvents reach dispatchKeyEvent before the system handles them.
    @Suppress("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val volumeTriggerEnabled = prefs.getBoolean("triggerVolumeButton", false)

        if (volumeTriggerEnabled && event.action == KeyEvent.ACTION_DOWN &&
            (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        ) {
            val now = System.currentTimeMillis()
            volumePressTimestamps.add(now)
            volumePressTimestamps.removeAll { now - it > 1500L }

            if (volumePressTimestamps.size >= 3) {
                volumePressTimestamps.clear()
                pendingEmergencyType = "Volume Button Triple-Press SOS!"
                isCountingDown = true
                countdownTimer = 10
                Toast.makeText(this, "SOS countdown started (Volume Buttons) - tap I'M OKAY to cancel", Toast.LENGTH_LONG).show()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }


    // This catches intents if the app is already open in the background
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent, getSharedPreferences("AppPrefs", MODE_PRIVATE))
    }

    private fun handleIntent(intent: Intent, prefs: android.content.SharedPreferences) {
        if (intent.getBooleanExtra("FALL_DETECTED", false)) {

            pendingEmergencyType = "Automated Fall Detection Alert!"

            isCountingDown = true
            countdownTimer = 10
        }
        if (intent.getBooleanExtra("MANUAL_SOS_TRIGGERED", false)) {

            // deliberate button press (in-app or notification action) - no countdown needed, they chose it
            executeSOS("Manual Notification SOS Triggered!", prefs)
        }
        if (intent.getBooleanExtra("SHAKE_SOS_TRIGGERED", false)) {
            // accident-prone trigger - give the same cancel window as fall detection
            pendingEmergencyType = "Shake Trigger SOS!"
            isCountingDown = true
            countdownTimer = 10
        }
        if (intent.getBooleanExtra("POWER_BUTTON_SOS_TRIGGERED", false)) {
            pendingEmergencyType = "Power Button Triple-Press SOS!"
            isCountingDown = true
            countdownTimer = 10
        }

    }

    private fun cancelCountdown() {
        isCountingDown = false; countdownTimer = 10

        SirenTorch.stopSiren()
        SirenTorch.stopTorchStrobe(this)

        Toast.makeText(this, "False Alarm Cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun executeSOS(emergencyType: String, prefs: android.content.SharedPreferences) {
        val contacts = ContactStore.load(prefs).map { it.phone }.filter { it.isNotBlank() }
        if (contacts.isEmpty()) { Toast.makeText(this, "No contacts saved!", Toast.LENGTH_LONG).show(); return }

        val call112 = prefs.getBoolean("call112", false)
        val vitalContext = prefs.getString("vitalContext", "None") ?: "None"
        val silentMode = prefs.getBoolean("silentMode", false)
        val customTemplate = prefs.getString("customMessageTemplate", "") ?: ""

        // Loud mode: siren + torch strobe until cancelled or the SMS/call flow below finishes kicking off
        if (!silentMode) {
            SirenTorch.startSiren()
            SirenTorch.startTorchStrobe(this)
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    val locationString = if (location != null) "https://maps.google.com/?q=${location.latitude},${location.longitude}" else "Location unavailable."
                    sendSosMessages(emergencyType, vitalContext, customTemplate, locationString, contacts, call112, prefs)
                }
                .addOnFailureListener {
                    // don't let a location lookup failure mean the SOS never goes out at all
                    sendSosMessages(emergencyType, vitalContext, customTemplate, "Location unavailable.", contacts, call112, prefs)
                }
        } else {
            // no location permission at all - still send what we can
            sendSosMessages(emergencyType, vitalContext, customTemplate, "Location unavailable.", contacts, call112, prefs)
        }
    }

    private fun sendSosMessages(
        emergencyType: String, vitalContext: String, customTemplate: String, locationString: String,
        contacts: List<String>, call112: Boolean, prefs: android.content.SharedPreferences
    ) {
        val finalMessage = if (customTemplate.isNotBlank()) {
            "$customTemplate\nType: $emergencyType\nContext: $vitalContext\nLoc: $locationString"
        } else {
            "SOS ALERT: $emergencyType\nContext: $vitalContext\nLoc: $locationString"
        }

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) getSystemService(SmsManager::class.java) else SmsManager.getDefault()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            for (number in contacts) { try { smsManager.sendTextMessage(number, null, finalMessage, null, null) } catch (e: Exception) { e.printStackTrace() } }
            Toast.makeText(this, "SMS Sent!", Toast.LENGTH_SHORT).show()
        }

        val numberToCall = if (call112) "112" else contacts.first()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$numberToCall")))
        }

        HistoryStore.add(prefs, SosHistoryEntry(System.currentTimeMillis(), emergencyType, locationString))
        startPeriodicTrackingIfEnabled(prefs, contacts, smsManager)
    }

    // Sends a follow-up location SMS every N seconds (from the profile setting) for 10 minutes, so responders
    // can keep tabs on movement after the initial alert. Stops automatically, or immediately if cancelCountdown() runs.
    private fun startPeriodicTrackingIfEnabled(prefs: android.content.SharedPreferences, contacts: List<String>, smsManager: SmsManager) {
        val intervalSeconds = prefs.getString("trackingIntervalSeconds", "0")?.toIntOrNull() ?: 0
        if (intervalSeconds <= 0) return

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val handler = android.os.Handler(mainLooper)
        var elapsedSeconds = 0
        val maxDurationSeconds = 600 // 10 minutes of follow-up pings, then stop on its own

        val runnable = object : Runnable {
            override fun run() {
                elapsedSeconds += intervalSeconds
                if (elapsedSeconds > maxDurationSeconds) return

                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        val locationString = if (location != null) "https://maps.google.com/?q=${location.latitude},${location.longitude}" else "Location unavailable."
                        val trackingMessage = "Tracking Update: still in progress.\nLoc: $locationString"
                        if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                            for (number in contacts) { try { smsManager.sendTextMessage(number, null, trackingMessage, null, null) } catch (e: Exception) { e.printStackTrace() } }
                        }
                    }
                }
                handler.postDelayed(this, intervalSeconds * 1000L)
            }
        }
        handler.postDelayed(runnable, intervalSeconds * 1000L)
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

    Column(modifier = Modifier.fillMaxSize().background(CanvasDeep).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Two-Way Alert", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AccentRed, modifier = Modifier.padding(bottom = 32.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(if (isLoginMode) "Sign In" else "Create Account", fontSize = 20.sp, color = TextPrimary, modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(value = email, onValueChange = { email = it; errorMessage = "" }, label = { Text("Email", color = TextSecondary) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary), modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it; errorMessage = "" }, label = { Text("Password", color = TextSecondary) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary), modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, contentDescription = null, tint = TextSecondary) } }
                )

                if (errorMessage.isNotEmpty()) Text(errorMessage, color = AccentRed, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))

                Spacer(modifier = Modifier.height(32.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = AccentRed)
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
                                                            .putBoolean("isLoggedIn", true)
                                                            .apply()

                                                        @Suppress("UNCHECKED_CAST")
                                                        val remoteContacts = ContactStore.fromFirestoreList(doc.get("contacts") as? List<Map<String, Any>>)
                                                        if (remoteContacts.isNotEmpty()) ContactStore.save(prefs, remoteContacts)
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
                        modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) { Text(if (isLoginMode) "Login" else "Register", color = Color.White) }
                }

                TextButton(onClick = { isLoginMode = !isLoginMode; errorMessage = "" }, modifier = Modifier.padding(top = 16.dp)) {
                    Text(if (isLoginMode) "Don't have an account? Register" else "Already have an account? Login", color = TextSecondary)
                }

                if (isLoginMode) {
                    TextButton(onClick = {
                        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            errorMessage = "Enter your email above first, then tap this again."
                        } else {
                            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                                errorMessage = if (task.isSuccessful) "Reset link sent to $email" else (task.exception?.localizedMessage ?: "Couldn't send reset email")
                            }
                        }
                    }) {
                        Text("Forgot password?", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, prefs: android.content.SharedPreferences) {
    var name by remember { mutableStateOf(prefs.getString("userName", "") ?: "") }
    var age by remember { mutableStateOf(prefs.getString("userAge", "") ?: "") }
    val sexOptions = listOf("Male", "Female", "Rather Not Say")
    var sex by remember { mutableStateOf(prefs.getString("userSex", sexOptions[0]) ?: sexOptions[0]) }
    var sexExpanded by remember { mutableStateOf(false) }

    val contextOptions = listOf("None", "Traveling Alone", "Asthma / Breathing Issues", "Heart Condition", "Mobility Impaired", "Deaf / Hard of Hearing", "Severe Allergies")
    var vitalContext by remember { mutableStateOf(prefs.getString("vitalContext", contextOptions[0]) ?: contextOptions[0]) }
    var expanded by remember { mutableStateOf(false) }

    var call112 by remember { mutableStateOf(prefs.getBoolean("call112", false)) }

    // THE TWO NEW SEPARATE TOGGLES
    var isSosNotificationRunning by remember { mutableStateOf(prefs.getBoolean("isSosNotificationRunning", false)) }
    var isFallDetectionRunning by remember { mutableStateOf(prefs.getBoolean("isFallDetectionRunning", false)) }
    var showPermissionGuide by remember { mutableStateOf(false) }

    // Profile is deliberately just the things the app can't function safely without:
    // who you are, and what responders/contacts need to know about you. Everything else
    // (triggers, siren, silent mode, custom message, tracking interval) lives in Alert Features now.
    Column(modifier = Modifier.fillMaxSize().background(CanvasDeep).padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            TextButton(onClick = {
                FirebaseAuth.getInstance().signOut()
                prefs.edit().clear().apply()
                navController.context.stopService(Intent(navController.context, SosForegroundService::class.java))
                navController.navigate("auth") { popUpTo(0) }
            }) { Text("Logout", color = AccentRed) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("This info is required for the app to alert your contacts correctly.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    ExposedDropdownMenuBox(expanded = sexExpanded, onExpandedChange = { sexExpanded = !sexExpanded }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = sex, onValueChange = {}, readOnly = true, label = { Text("Sex") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = sexExpanded, onDismissRequest = { sexExpanded = false }) {
                            sexOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = { sex = option; sexExpanded = false })
                            }
                        }
                    }
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
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                prefs.edit()
                    .putString("userName", name).putString("userAge", age).putString("userSex", sex)
                    .putString("vitalContext", vitalContext)
                    .apply()
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val userData = hashMapOf("name" to name, "age" to age, "sex" to sex, "vitalContext" to vitalContext)
                    FirebaseFirestore.getInstance().collection("users").document(uid).set(userData, SetOptions.merge())
                }
                Toast.makeText(navController.context, "Profile Saved. Go to Contacts next!", Toast.LENGTH_LONG).show()
                navController.navigate("contacts") // Forces the onboarding flow naturally
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
        ) { Text("Save & Next ->", color = Color.White) }
        Spacer(modifier = Modifier.height(110.dp)) // clears the floating pill nav bar
    }
}

@Composable
fun AlertFeaturesScreen(
    navController: NavController, prefs: android.content.SharedPreferences,
    comfortModeEnabled: Boolean, onComfortModeChange: (Boolean) -> Unit
) {
    var call112 by remember { mutableStateOf(prefs.getBoolean("call112", false)) }
    var isSosNotificationRunning by remember { mutableStateOf(prefs.getBoolean("isSosNotificationRunning", false)) }
    var isFallDetectionRunning by remember { mutableStateOf(prefs.getBoolean("isFallDetectionRunning", false)) }
    var showPermissionGuide by remember { mutableStateOf(false) }
    var customMessage by remember { mutableStateOf(prefs.getString("customMessageTemplate", "") ?: "") }
    var silentMode by remember { mutableStateOf(prefs.getBoolean("silentMode", false)) }
    var trackingInterval by remember { mutableStateOf(prefs.getString("trackingIntervalSeconds", "0") ?: "0") }
    var shakeTriggerEnabled by remember { mutableStateOf(prefs.getBoolean("triggerShake", false)) }
    var volumeTriggerEnabled by remember { mutableStateOf(prefs.getBoolean("triggerVolumeButton", false)) }
    var powerButtonTriggerEnabled by remember { mutableStateOf(prefs.getBoolean("triggerPowerButton", false)) }

    fun updateBackgroundService() {
        val serviceIntent = Intent(navController.context, SosForegroundService::class.java).apply {
            putExtra("ENABLE_FALL_DETECTION", isFallDetectionRunning)
            putExtra("ENABLE_SHAKE_TRIGGER", shakeTriggerEnabled)
            putExtra("ENABLE_POWER_BUTTON_TRIGGER", powerButtonTriggerEnabled)
        }
        if (isSosNotificationRunning || isFallDetectionRunning || shakeTriggerEnabled || powerButtonTriggerEnabled) {

            ContextCompat.startForegroundService(navController.context, serviceIntent)
        } else {
            navController.context.stopService(serviceIntent)
        }
    }


    // THE PERMISSION TUTORIAL POPUP
    if (showPermissionGuide) {
        AlertDialog(
            onDismissRequest = { showPermissionGuide = false },
            title = { Text("Setup Required", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text("To automatically wake up your phone during a fall, Android requires a special permission.\n\n1. We will open Settings.\n2. Find 'Two Way Alert' in the list.\n3. Turn ON 'Allow display over other apps'.\n4. Press back to return here.", color = TextSecondary)
            },
            confirmButton = {
                Button(onClick = {
                    showPermissionGuide = false
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${navController.context.packageName}"))
                    navController.context.startActivity(intent)
                }, colors = ButtonDefaults.buttonColors(containerColor = AccentRed)) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionGuide = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = CanvasElevated, textContentColor = TextPrimary, titleContentColor = TextPrimary
        )
    }

    var showAdvancedTriggers by remember { mutableStateOf(!comfortModeEnabled) }

    Column(modifier = Modifier.fillMaxSize().background(CanvasDeep).padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Alert Features", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
        Text(
            if (comfortModeEnabled) "Turn on the ways you want to trigger an SOS. Shown one at a time for easier reading."
            else "Turn on the ways you want to trigger an SOS, and how it behaves.",
            color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 20.dp)
        )

        // Primary tiles: the easiest, most reliable triggers. Shown to everyone directly.
        // Comfort Mode stacks these full-width instead of 2-per-row for bigger text and targets.
        val primaryTilePairs = listOf(
            Triple(Icons.Outlined.NotificationsActive, "Quick-Access SOS" to "Pins an SOS button to your notifications.") { checked: Boolean ->
                isSosNotificationRunning = checked
                prefs.edit().putBoolean("isSosNotificationRunning", checked).apply()
                updateBackgroundService()
            },
            Triple(Icons.Outlined.WarningAmber, "24/7 Fall Detection" to "Monitors for hard drops in the background.") { checked: Boolean ->
                if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(navController.context)) {
                    showPermissionGuide = true
                } else {
                    isFallDetectionRunning = checked
                    prefs.edit().putBoolean("isFallDetectionRunning", checked).apply()
                    updateBackgroundService()
                }
            }
        )
        val primaryEnabled = listOf(isSosNotificationRunning, isFallDetectionRunning)

        if (comfortModeEnabled) {
            primaryTilePairs.forEachIndexed { i, (icon, pair, onToggle) ->
                FeatureToggleTile(icon = icon, title = pair.first, subtitle = pair.second, enabled = primaryEnabled[i], modifier = Modifier.fillMaxWidth(), onToggle = onToggle)
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                primaryTilePairs.forEachIndexed { i, (icon, pair, onToggle) ->
                    FeatureToggleTile(icon = icon, title = pair.first, subtitle = pair.second, enabled = primaryEnabled[i], modifier = Modifier.weight(1f), onToggle = onToggle)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        val secondaryTilePairs = listOf(
            Triple(Icons.Outlined.WarningAmber, "Shake to Trigger" to "Firmly shake the phone to fire an alert.") { checked: Boolean ->
                shakeTriggerEnabled = checked
                prefs.edit().putBoolean("triggerShake", checked).apply()
                updateBackgroundService()
            },
            Triple(Icons.AutoMirrored.Outlined.FormatListBulleted, "Silent Mode" to "Skips the siren + flashlight strobe on alert.") { checked: Boolean ->
                silentMode = checked
                prefs.edit().putBoolean("silentMode", checked).apply()
            }
        )
        val secondaryEnabled = listOf(shakeTriggerEnabled, silentMode)

        if (comfortModeEnabled) {
            secondaryTilePairs.forEachIndexed { i, (icon, pair, onToggle) ->
                FeatureToggleTile(icon = icon, title = pair.first, subtitle = pair.second, enabled = secondaryEnabled[i], modifier = Modifier.fillMaxWidth(), onToggle = onToggle)
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                secondaryTilePairs.forEachIndexed { i, (icon, pair, onToggle) ->
                    FeatureToggleTile(icon = icon, title = pair.first, subtitle = pair.second, enabled = secondaryEnabled[i], modifier = Modifier.weight(1f), onToggle = onToggle)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Advanced triggers (volume/power triple-press) need quick, precise button presses that
        // aren't always easy for elderly users, so they're tucked behind a toggle in Comfort Mode
        // instead of competing for attention with the simpler options above.
        if (comfortModeEnabled) {
            TextButton(onClick = { showAdvancedTriggers = !showAdvancedTriggers }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showAdvancedTriggers) "Hide advanced triggers" else "Show advanced triggers (volume / power button)", color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (showAdvancedTriggers) {
            val advancedTilePairs = listOf(
                Triple(Icons.Outlined.NotificationsActive, "Volume Triple-Press" to "Press either volume key 3x to fire an alert.") { checked: Boolean ->
                    volumeTriggerEnabled = checked
                    prefs.edit().putBoolean("triggerVolumeButton", checked).apply()
                },
                Triple(Icons.Outlined.Call, "Power Triple-Press" to "Screen off/on 3x fires an alert, even locked.") { checked: Boolean ->
                    powerButtonTriggerEnabled = checked
                    prefs.edit().putBoolean("triggerPowerButton", checked).apply()
                    updateBackgroundService()
                }
            )
            val advancedEnabled = listOf(volumeTriggerEnabled, powerButtonTriggerEnabled)

            if (comfortModeEnabled) {
                advancedTilePairs.forEachIndexed { i, (icon, pair, onToggle) ->
                    FeatureToggleTile(icon = icon, title = pair.first, subtitle = pair.second, enabled = advancedEnabled[i], modifier = Modifier.fillMaxWidth(), onToggle = onToggle)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    advancedTilePairs.forEachIndexed { i, (icon, pair, onToggle) ->
                        FeatureToggleTile(icon = icon, title = pair.first, subtitle = pair.second, enabled = advancedEnabled[i], modifier = Modifier.weight(1f), onToggle = onToggle)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureToggleTile(
                icon = Icons.Outlined.Person, title = "Comfort Mode", subtitle = "Bigger text & buttons app-wide (elderly-friendly).",
                enabled = comfortModeEnabled, modifier = Modifier.weight(1f)
            ) {
                prefs.edit().putBoolean(PREFS_KEY_COMFORT_MODE, it).apply()
                onComfortModeChange(it)
                showAdvancedTriggers = !it
            }
            FeatureToggleTile(
                icon = Icons.Outlined.Call, title = "Dial 112 on SOS", subtitle = "Calls emergency services instead of your contact.",
                enabled = call112, modifier = Modifier.weight(1f)
            ) {
                call112 = it
                prefs.edit().putBoolean("call112", it).apply()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(
                    value = customMessage, onValueChange = { customMessage = it },
                    label = { Text("Custom SOS Message (optional)") },
                    placeholder = { Text("EMERGENCY! I need immediate help.", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = trackingInterval, onValueChange = { trackingInterval = it.filter { c -> c.isDigit() } },
                    label = { Text("Periodic Tracking SMS Interval (seconds, 0 = off)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                prefs.edit()
                    .putString("customMessageTemplate", customMessage)
                    .putString("trackingIntervalSeconds", trackingInterval.ifBlank { "0" })
                    .apply()
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val userData = hashMapOf(
                    "call112" to call112, "customMessageTemplate" to customMessage, "trackingIntervalSeconds" to trackingInterval
                    )
                    FirebaseFirestore.getInstance().collection("users").document(uid).set(userData, SetOptions.merge())
                }
                Toast.makeText(navController.context, "Alert settings saved", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
        ) { Text("Save Alert Settings", color = Color.White) }
        Spacer(modifier = Modifier.height(110.dp)) // clears the floating pill nav bar
    }
}

@Composable
fun ContactsScreen(navController: NavController, prefs: android.content.SharedPreferences) {
    var contacts by remember { mutableStateOf(ContactStore.load(prefs)) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var phoneContacts by remember { mutableStateOf(listOf<Contact>()) }
    var importSearchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    fun persist() {
        ContactStore.save(prefs, contacts)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(hashMapOf("contacts" to ContactStore.toFirestoreList(contacts)), SetOptions.merge())
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            phoneContacts = loadDeviceContacts(context)
            showImportDialog = true
        } else {
            Toast.makeText(context, "Contacts permission needed to import", Toast.LENGTH_SHORT).show()
        }
    }

    if (showImportDialog) {
        val filteredContacts = remember(importSearchQuery, phoneContacts) {
            if (importSearchQuery.isBlank()) phoneContacts
            else phoneContacts.filter {
                it.name.contains(importSearchQuery, ignoreCase = true) || it.phone.contains(importSearchQuery)
            }
        }
        val alreadyAddedNumbers = remember(contacts) { contacts.map { it.phone }.toSet() }

        Dialog(onDismissRequest = { showImportDialog = false; importSearchQuery = "" }) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), tint = CanvasElevated.copy(alpha = 0.94f)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Import from Phone", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importSearchQuery, onValueChange = { importSearchQuery = it },
                        placeholder = { Text("Search name or number", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredContacts.isEmpty()) {
                        Text(
                            if (phoneContacts.isEmpty()) "No phone contacts with numbers found." else "No matches for \"$importSearchQuery\".",
                            color = TextSecondary, modifier = Modifier.padding(vertical = 20.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                            items(filteredContacts) { pc ->
                                val alreadyAdded = alreadyAddedNumbers.contains(pc.phone)
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable(enabled = !alreadyAdded) {
                                            contacts = contacts.toMutableList().apply { add(pc) }
                                            persist()
                                            Toast.makeText(context, "${pc.name} added", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pc.name, color = if (alreadyAdded) TextSecondary else TextPrimary, fontWeight = FontWeight.Medium)
                                        Text(pc.phone, color = TextSecondary, fontSize = 12.sp)
                                    }
                                    if (alreadyAdded) {
                                        Text("Added", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    } else {
                                        Icon(Icons.Outlined.Add, contentDescription = "Add", tint = AccentRed)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showImportDialog = false; importSearchQuery = "" }, modifier = Modifier.fillMaxWidth()) {
                        Text("Done", color = AccentRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(CanvasDeep).padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Emergency Contacts", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 16.dp))

        val requirementMet = contacts.isNotEmpty() || prefs.getBoolean("call112", false)
        if (!requirementMet) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), tint = AccentAmber.copy(alpha = 0.16f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add a contact to continue", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "At least one emergency contact is required, unless you'd rather alerts call 112 directly \u2014 turn that on under Alert Features \u2192 Dial 112 on SOS.",
                        color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    phoneContacts = loadDeviceContacts(context)
                    showImportDialog = true
                } else {
                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GlassFillLight, contentColor = TextPrimary)
        ) { Text("Import from Phone Contacts") }

        Spacer(modifier = Modifier.height(20.dp))

        if (contacts.isEmpty()) {
            Text("No contacts added yet. Add one below or import from your phone.", color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp))
        }

        contacts.forEachIndexed { index, contact ->
            GlassPanel(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(contact.name.ifBlank { "Contact ${index + 1}" }, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(contact.phone, color = TextSecondary, fontSize = 13.sp)
                    }
                    TextButton(onClick = {
                        contacts = contacts.toMutableList().apply { removeAt(index) }
                        persist()
                    }) { Text("Remove", color = AccentRed) }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Add a contact manually", color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                val cleaned = newPhone.trim()
                if (cleaned.isBlank()) {
                    Toast.makeText(navController.context, "Enter a phone number first", Toast.LENGTH_SHORT).show()
                } else if (!Patterns.PHONE.matcher(cleaned).matches() || cleaned.filter { it.isDigit() }.length < 7) {
                    Toast.makeText(navController.context, "That doesn't look like a valid phone number", Toast.LENGTH_SHORT).show()
                } else {
                    contacts = contacts.toMutableList().apply { add(Contact(newName.ifBlank { "Contact ${contacts.size + 1}" }, cleaned)) }
                    persist()
                    newName = ""; newPhone = ""
                    Toast.makeText(navController.context, "Contact added", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
        ) { Text("+ Add Contact", color = Color.White) }
        Spacer(modifier = Modifier.height(110.dp)) // clears the floating pill nav bar
    }
}

// Reads the device's phone contacts (name + primary number) for the Import flow.
// Caller must have already confirmed READ_CONTACTS is granted.
private fun loadDeviceContacts(context: android.content.Context): List<Contact> {
    val results = mutableListOf<Contact>()
    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
        null, null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
    )
    cursor?.use {
        val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (it.moveToNext()) {
            val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Unknown" else "Unknown"
            val number = if (numberIdx >= 0) it.getString(numberIdx) else null
            if (!number.isNullOrBlank()) results.add(Contact(name, number))
        }
    }
    return results.distinctBy { it.phone }
}

@Composable
fun HistoryScreen(prefs: android.content.SharedPreferences) {
    var entries by remember { mutableStateOf(HistoryStore.load(prefs)) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().background(CanvasDeep).padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("SOS History", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            if (entries.isNotEmpty()) {
                TextButton(onClick = { HistoryStore.clear(prefs); entries = mutableListOf() }) { Text("Clear", color = AccentRed) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Text("No SOS alerts triggered yet.", color = TextSecondary)
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                entries.forEach { entry ->
                    GlassPanel(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(entry.type, color = AccentRed, fontWeight = FontWeight.Bold)
                            Text(dateFormat.format(Date(entry.timestamp)), color = TextSecondary, fontSize = 12.sp)
                            Text(entry.location, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(110.dp)) // clears the floating pill nav bar
            }
        }
    }
}

@Composable
fun SystemActiveBadge(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    GlassPanel(modifier = modifier, shape = RoundedCornerShape(50)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(7.dp).background(AccentGreen.copy(alpha = pulseAlpha), shape = CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text("System Active", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun LiveAccelerometerReadout(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var gForce by remember { mutableFloatStateOf(1.0f) } // in multiples of standard gravity

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    GlassPanel(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MOTION SENSOR", style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(String.format(java.util.Locale.US, "%.2f g", gForce), style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            // fluid amber -> red gradient meter representing live impact force, per design spec
            val fraction = (gForce / 3f).coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(GlassFillLight)) {
                Box(
                    modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight()
                        .background(Brush.horizontalGradient(colors = listOf(AccentAmber, AccentRed)))
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlertScreen(isCountingDown: Boolean, timeLeft: Int, predictionResult: String, onExecuteSos: (String) -> Unit, onCancel: () -> Unit) {
    var showPresetsDialog by remember { mutableStateOf(false) }
    var showQuickActions by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(CanvasDeep)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {

            if (isCountingDown) {
                GlassPanel(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ALERT DETECTED", color = AccentRed, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Dispatching SOS in $timeLeft", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                        if (predictionResult.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Prediction: $predictionResult", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(28.dp))
                        Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2E)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Text("I'M OKAY (CANCEL)", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                    }
                }
            } else {
                Text("System Ready", color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 32.dp))
                Text("Tap for SOS · Hold for more options", color = TextSecondary.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 16.dp))

                // layered glow — several soft radial washes at increasing radius/decreasing alpha,
                // "light emitting through liquid glass" rather than one flat ring
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(300.dp).background(Brush.radialGradient(colors = listOf(AccentRed.copy(alpha = 0.10f), Color.Transparent)), shape = CircleShape))
                    Box(modifier = Modifier.size(260.dp).background(Brush.radialGradient(colors = listOf(AccentRed.copy(alpha = 0.18f), Color.Transparent)), shape = CircleShape))
                    Box(modifier = Modifier.size(220.dp).background(Brush.radialGradient(colors = listOf(AccentRed.copy(alpha = 0.28f), Color.Transparent)), shape = CircleShape))

                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .shadow(16.dp, CircleShape, ambientColor = AccentRed, spotColor = AccentRed)
                            .clip(CircleShape)
                            .background(AccentRed)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showPresetsDialog = true },
                                onLongClick = { showQuickActions = true }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SOS", fontSize = 52.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                LiveAccelerometerReadout(modifier = Modifier.fillMaxWidth(0.6f))
            }
        }

        if (showPresetsDialog) {
            Dialog(onDismissRequest = { showPresetsDialog = false }) {
                GlassPanel(modifier = Modifier.fillMaxWidth(), tint = CanvasElevated.copy(alpha = 0.94f)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Select Emergency Type", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Contacts get your location either way — this just labels what's happening.", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        PresetButton("Medical Emergency", Icons.Outlined.WarningAmber, AccentRed, onExecuteSos) { showPresetsDialog = false }
                        Spacer(modifier = Modifier.height(10.dp))
                        PresetButton("Theft / Robbery", Icons.Outlined.WarningAmber, AccentAmber, onExecuteSos) { showPresetsDialog = false }
                        Spacer(modifier = Modifier.height(10.dp))
                        PresetButton("General SOS", Icons.Outlined.NotificationsActive, AccentRed, onExecuteSos) { showPresetsDialog = false }
                        Spacer(modifier = Modifier.height(14.dp))
                        TextButton(onClick = { showPresetsDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Medium) }
                    }
                }
            }
        }

        // Long-press quick actions — triggered by holding the SOS button
        if (showQuickActions) {
            Dialog(onDismissRequest = { showQuickActions = false }) {
                GlassPanel(modifier = Modifier.fillMaxWidth(), tint = CanvasElevated.copy(alpha = 0.94f)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("More Emergency Options", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("These still use your current Silent Mode and contact settings from Alert Features.", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        PresetButton("Silent Alarm", Icons.AutoMirrored.Outlined.FormatListBulleted, TextSecondary, onExecuteSos) { showQuickActions = false }
                        Spacer(modifier = Modifier.height(10.dp))
                        PresetButton("Call Contacts Now", Icons.Outlined.Call, AccentRed, onExecuteSos) { showQuickActions = false }
                        Spacer(modifier = Modifier.height(14.dp))
                        TextButton(onClick = { showQuickActions = false }, modifier = Modifier.fillMaxWidth()) { Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Medium) }
                    }
                }
            }
        }
    }
}

@Composable
fun PresetButton(
    message: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color,
    onExecuteSos: (String) -> Unit, closeDialog: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(width = 1.dp, color = accent.copy(alpha = 0.35f), shape = RoundedCornerShape(16.dp))
            .clickable { onExecuteSos(message); closeDialog() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(accent.copy(alpha = 0.25f), shape = CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(message, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
    }
}
