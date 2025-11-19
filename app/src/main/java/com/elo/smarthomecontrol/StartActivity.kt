package com.elo.smarthomecontrol

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elo.smarthomecontrol.data.ThemePreferences
import com.elo.smarthomecontrol.ui.theme.SmartHomeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance().reference
        val themePrefs = ThemePreferences(this)

        setContent {
            val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)
            SmartHomeTheme(darkTheme = isDarkMode) {
                SplashScreen(isDarkMode) {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        val uid = currentUser.uid
                        val userRef = database.child("users").child(uid)

                        userRef.get().addOnSuccessListener { snapshot ->
                            if (!snapshot.exists()) {
                                val userData = mapOf(
                                    "email" to currentUser.email,
                                    "profileImage" to "",
                                    "createdAt" to System.currentTimeMillis(),
                                    "lastLogin" to System.currentTimeMillis()
                                )
                                userRef.setValue(userData)
                            } else {
                                userRef.child("lastLogin").setValue(System.currentTimeMillis())
                            }

                            startActivity(Intent(this@StartActivity, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        startActivity(Intent(this@StartActivity, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(isDarkMode: Boolean, onFinish: () -> Unit) {
    val gradientColors = if (isDarkMode) {
        listOf(Color(0xFF0B1C2C), Color(0xFF1C3A5F))
    } else {
        listOf(Color(0xFFE8EAF6), Color(0xFFBBDEFB))
    }

    val logoRes = R.drawable.logo
    val textColor = if (isDarkMode) Color.White else Color(0xFF0B1C2C)

    // ✨ Fade animasyonu
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            logoAlpha.animateTo(1f, animationSpec = tween(durationMillis = 1000))
            delay(300)
            textAlpha.animateTo(1f, animationSpec = tween(durationMillis = 700))
        }
        delay(2000)
        onFinish()
    }

    // 🎬 Ekran düzeni
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = stringResource(R.string.splash_logo_desc),
                modifier = Modifier
                    .size(230.dp)
                    .alpha(logoAlpha.value)
            )
        }
    }
}
