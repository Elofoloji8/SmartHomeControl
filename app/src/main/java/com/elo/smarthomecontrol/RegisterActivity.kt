package com.elo.smarthomecontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.elo.smarthomecontrol.data.ThemePreferences
import com.elo.smarthomecontrol.ui.theme.SmartHomeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        val themePrefs = ThemePreferences(this)

        setContent {
            val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)

            SmartHomeTheme(darkTheme = isDarkMode) {
                RegisterScreen(
                    isDarkMode = isDarkMode,
                    onRegister = { email, password -> registerUser(email, password) },
                    onLoginClick = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid

                    if (uid == null) {
                        Toast.makeText(this, "Kullanıcı kimliği alınamadı, lütfen tekrar deneyin.", Toast.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    // 🔹 Kullanıcı verileri
                    val userData = mapOf(
                        "email" to email,
                        "profileImage" to "",
                        "createdAt" to System.currentTimeMillis(),
                        "lastLogin" to System.currentTimeMillis(),
                        "displayName" to "",
                        "theme" to "light"
                    )

                    // 🔹 Veritabanına kaydet
                    database.child("users").child(uid).setValue(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Kayıt başarılı ✅", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Veri kaydedilemedi: ${it.message}", Toast.LENGTH_LONG).show()
                        }

                } else {
                    Toast.makeText(
                        this,
                        "Kayıt başarısız: ${task.exception?.localizedMessage ?: "Bilinmeyen hata"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    isDarkMode: Boolean,
    onRegister: (String, String) -> Unit,
    onLoginClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // 🎨 Tema renkleri
    val gradientColors = if (isDarkMode) {
        listOf(Color(0xFF0B1C2C), Color(0xFF1C3A5F))
    } else {
        listOf(Color(0xFFE8EAF6), Color(0xFFBBDEFB))
    }

    val textColor = if (isDarkMode) Color.White else Color(0xFF0B1C2C)
    val buttonColor = if (isDarkMode) Color.White else Color(0xFF112D44)
    val buttonTextColor = if (isDarkMode) Color(0xFF0B1C2C) else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🖼️ Uygulama logosu (şeffaf PNG)
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 16.dp)
            )

            // 📧 E-posta
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-posta", color = textColor) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = textColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.6f),
                    cursorColor = textColor,
                    focusedLabelColor = textColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔐 Şifre alanı
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Şifre", color = textColor) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon =
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
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

            Spacer(modifier = Modifier.height(24.dp))

            // 🔘 Kayıt butonu
            Button(
                onClick = { onRegister(email.trim(), password.trim()) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Kayıt Ol", color = buttonTextColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔗 Giriş yap butonu
            TextButton(onClick = onLoginClick) {
                Text("Zaten hesabın var mı? Giriş yap", color = textColor.copy(alpha = 0.8f))
            }
        }
    }
}