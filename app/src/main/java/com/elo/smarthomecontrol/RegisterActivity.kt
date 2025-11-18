package com.elo.smarthomecontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            RegisterScreen(
                onRegister = { email, password -> registerUser(email, password) },
                onLoginClick = {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            )
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
                    val uid = user?.uid ?: return@addOnCompleteListener

                    val userData = mapOf(
                        "email" to email,
                        "profileImage" to "",
                        "createdAt" to System.currentTimeMillis(),
                        "lastLogin" to System.currentTimeMillis()
                    )

                    // 🔹 Kullanıcıyı Realtime Database'e ekle
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

@Composable
fun RegisterScreen(onRegister: (String, String) -> Unit, onLoginClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1C2C)), // Lacivert arka plan
        color = Color(0xFF0B1C2C)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Smart Home Control",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-posta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color.White,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Şifre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color.White,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onRegister(email.trim(), password.trim()) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Kayıt Ol", color = Color(0xFF0B1C2C))
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onLoginClick) {
                Text("Zaten hesabın var mı? Giriş yap", color = Color.LightGray)
            }
        }
    }
}