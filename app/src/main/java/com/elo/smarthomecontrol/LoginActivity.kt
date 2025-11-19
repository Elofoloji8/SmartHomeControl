package com.elo.smarthomecontrol

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.elo.smarthomecontrol.data.ThemePreferences
import com.elo.smarthomecontrol.ui.theme.SmartHomeTheme
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        val themePrefs = ThemePreferences(this)

        setContent {
            val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)

            SmartHomeTheme(darkTheme = isDarkMode) {
                LoginScreen(
                    isDarkMode = isDarkMode,
                    onLoginSuccess = { uid, email ->
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("USER_UID", uid)
                            putExtra("USER_EMAIL", email)
                        }
                        startActivity(intent)
                        finish()
                    },
                    onGoToRegister = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                        finish()
                    },
                    auth = auth,
                    database = database
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    isDarkMode: Boolean,
    onLoginSuccess: (String, String) -> Unit,
    onGoToRegister: () -> Unit,
    auth: FirebaseAuth,
    database: com.google.firebase.database.DatabaseReference
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    // 🎨 Arka plan ve renk teması
    val gradientColors = if (isDarkMode) {
        listOf(Color(0xFF0B1C2C), Color(0xFF1C3A5F))
    } else {
        listOf(Color(0xFFE8EAF6), Color(0xFFBBDEFB))
    }
    val textColor = if (isDarkMode) Color.White else Color(0xFF0B1C2C)
    val buttonColor = if (isDarkMode) Color.White else Color(0xFF112D44)
    val buttonTextColor = if (isDarkMode) Color(0xFF0B1C2C) else Color.White

    // 🧭 Ekran düzeni
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 🖼️ Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(180.dp).padding(bottom = 16.dp)
            )

            // 📧 E-posta alanı
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email_label), color = textColor) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = textColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.6f),
                    cursorColor = textColor,
                    focusedLabelColor = textColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            Spacer(Modifier.height(12.dp))

            // 🔒 Şifre alanı
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_label), color = textColor) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(icon, contentDescription = null, tint = textColor)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = textColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.6f),
                    cursorColor = textColor,
                    focusedLabelColor = textColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            Spacer(Modifier.height(24.dp))

            // 🔘 Giriş Butonu
            Button(
                onClick = {
                    message = ""
                    if (email.isBlank() || password.isBlank()) {
                        message = context.getString(R.string.error_empty_fields)
                        return@Button
                    }
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        message = context.getString(R.string.error_invalid_email)
                        return@Button
                    }

                    loading = true
                    auth.signInWithEmailAndPassword(email.trim(), password.trim())
                        .addOnCompleteListener { task ->
                            loading = false
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                val uid = user?.uid
                                if (uid != null) {
                                    val now = System.currentTimeMillis()
                                    val userRef = database.child("users").child(uid)
                                    userRef.get().addOnSuccessListener { snapshot ->
                                        if (!snapshot.exists()) {
                                            val userData = mapOf(
                                                "email" to email,
                                                "profileImage" to "",
                                                "createdAt" to now,
                                                "lastLogin" to now
                                            )
                                            userRef.setValue(userData)
                                        } else {
                                            userRef.child("lastLogin").setValue(now)
                                        }
                                    }
                                    onLoginSuccess(uid, email)
                                } else {
                                    message = "Kullanıcı bilgisi alınamadı."
                                }
                            } else {
                                val errorMsg = when (val e = task.exception) {
                                    is FirebaseAuthInvalidUserException -> "Böyle bir kullanıcı yok"
                                    is FirebaseAuthInvalidCredentialsException -> "E-posta veya şifre hatalı"
                                    is FirebaseNetworkException -> "İnternet bağlantısı yok"
                                    else -> e?.localizedMessage ?: "Bilinmeyen hata"
                                }
                                message = context.getString(R.string.error_login_failed, errorMsg)
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                enabled = !loading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (loading)
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = buttonTextColor,
                        strokeWidth = 2.dp
                    )
                else
                    Text(stringResource(R.string.login_button), color = buttonTextColor)
            }

            Spacer(Modifier.height(16.dp))

            // 🔗 Kayıt Ol
            TextButton(onClick = onGoToRegister) {
                Text(stringResource(R.string.register_text), color = textColor.copy(alpha = 0.8f))
            }

            Spacer(Modifier.height(8.dp))
            if (message.isNotEmpty()) {
                Text(message, color = Color.Red)
            }
        }
    }
}