package com.myapplication.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.myapplication.app.ui.components.GlassPanel
import com.myapplication.app.ui.theme.AccentRed
import com.myapplication.app.ui.theme.CanvasDeep
import com.myapplication.app.ui.theme.TextPrimary
import com.myapplication.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val body: String,
)

private val onboardingPages = listOf(
    OnboardingPage(
        Icons.Filled.Warning, "Welcome to Two-Way Alert",
        "A personal safety app that watches for falls automatically and gets you help fast when you need it even if you can't reach your phone.",
    ),
    OnboardingPage(
        Icons.Filled.Notifications, "Fall Detection, Always On",
        "Enable 24/7 Fall Detection in your Profile and the app quietly monitors for hard drops in the background. If one's detected, you get 10 seconds to cancel before contacts are alerted."
    ),
    OnboardingPage(
        Icons.Filled.Warning, "More Ways to Call for Help",
        "Besides the SOS button, you can trigger an alert by shaking your phone, triple-pressing a volume key, or triple-pressing power handy if your hands are busy or the phone is locked."
    ),
    OnboardingPage(
        Icons.Filled.Contacts, "Set Up Your Contacts",
        "Add the people who should hear from you first. Every alert texts them your live location, and can call your primary contact or 112 automatically."
    ),
    OnboardingPage(
        Icons.Filled.Call, "You're Ready",
        "Next, we'll ask for a few permissions (SMS, location, camera, notifications) so alerts can actually go out. Let's get your profile set up."
    )
)

@Composable
fun OnboardingScreen(prefs: android.content.SharedPreferences, onFinished: () -> Unit) {
    val pagerState = rememberPagerState { onboardingPages.size }
    val scope = rememberCoroutineScope()

    fun finish() {
        prefs.edit { putBoolean("hasSeenOnboarding", true) }
        onFinished()
    }

    Column(modifier = Modifier.fillMaxSize().background(CanvasDeep)) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { finish() }) { Text("Skip", color = TextSecondary) }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
            val page = onboardingPages[pageIndex]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                GlassPanel(modifier = Modifier.size(120.dp), shape = CircleShape) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(page.icon, contentDescription = null, tint = AccentRed, modifier = Modifier.size(52.dp))
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
                Text(page.title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(page.body, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
            }
        }

        // dot indicator
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.Center) {
            repeat(onboardingPages.size) { i ->
                val isSelected = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 8.dp)
                        .background(if (isSelected) AccentRed else Color(0xFF3A3A3E), shape = CircleShape)
                )
            }
        }

        Button(
            onClick = {
                if (pagerState.currentPage == onboardingPages.lastIndex) {
                    finish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp).height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
        ) {
            Text(
                if (pagerState.currentPage == onboardingPages.lastIndex) "Get Started" else "Next",
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}