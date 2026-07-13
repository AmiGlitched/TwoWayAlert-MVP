package com.myapplication.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.myapplication.app.ui.components.GlassPanel
import com.myapplication.app.ui.theme.AccentRed
import com.myapplication.app.ui.theme.CanvasDeep
import com.myapplication.app.ui.theme.TextPrimary
import com.myapplication.app.ui.theme.TextSecondary
import com.myapplication.app.utils.PREFS_KEY_COMFORT_MODE

// The reasons drive which features get suggested as defaults later (e.g. an elderly/fall-risk
// user gets Fall Detection pre-enabled; a "traveling alone" user gets discreet triggers surfaced
// higher). The mapping is applied once here at setup time, not force-locked — everything stays
// editable afterward in Alert Features.
private val sosReasons = listOf(
    "Fall Risk / Elderly Care",
    "Medical Condition",
    "Traveling Alone / Personal Safety",
    "General Safety",
    "Other"
)

private val sexOptions = listOf("Male", "Female", "Rather Not Say")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreSetupScreen(navController: NavController, prefs: android.content.SharedPreferences, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(sexOptions[0]) }
    var sexExpanded by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf(sosReasons[3]) }
    var reasonExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(CanvasDeep).padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Let's Set Up Two-Way Alert", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "A few quick details before you sign in — this is what makes your alerts actually useful to responders, and lets us tailor the app to you.",
            color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 20.dp)
        )

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it; errorMessage = "" }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = age, onValueChange = { age = it.filter { c -> c.isDigit() }; errorMessage = "" },
                        label = { Text("Age") }, modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
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
                ExposedDropdownMenuBox(expanded = reasonExpanded, onExpandedChange = { reasonExpanded = !reasonExpanded }) {
                    OutlinedTextField(
                        value = reason, onValueChange = {}, readOnly = true,
                        label = { Text("Main reason you need this app") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = reasonExpanded, onDismissRequest = { reasonExpanded = false }) {
                        sosReasons.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { reason = option; reasonExpanded = false })
                        }
                    }
                }
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(errorMessage, color = AccentRed, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "If you're setting this up for an elderly parent or family member, just enter their age — " +
                    "we'll automatically switch on Comfort Mode (bigger text and buttons) for them. You can change this anytime in Alert Features.",
            color = TextSecondary, fontSize = 11.5.sp, lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                val ageInt = age.toIntOrNull()
                when {
                    name.isBlank() -> errorMessage = "Enter a name."
                    ageInt == null || ageInt <= 0 || ageInt > 120 -> errorMessage = "Enter a valid age."
                    sex.isBlank() -> errorMessage = "Enter sex."
                    else -> {
                        val autoComfortMode = ageInt >= 60
                        prefs.edit()
                            .putString("userName", name)
                            .putString("userAge", age)
                            .putString("userSex", sex)
                            .putString("sosReason", reason)
                            .putBoolean(PREFS_KEY_COMFORT_MODE, autoComfortMode)
                            .putBoolean("hasCompletedPreSetup", true)
                            .apply()
                        onDone()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
        ) { Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold) }
    }
}