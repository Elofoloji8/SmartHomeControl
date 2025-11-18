package com.elo.smarthomecontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            LoginScreen(
                onLoginSuccess = {
                    startActivity(Intent(this, MainActivity::class.java))
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

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit,
    auth: FirebaseAuth,
    database: com.google.firebase.database.DatabaseReference
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    // 🎨 Lacivert tema
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Smart Home Control",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-posta") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color.White,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Şifre") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color.White,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    val uid = user?.uid ?: return@addOnCompleteListener

                                    // Kullanıcı veritabanında varsa güncelle, yoksa oluştur
                                    val userRef = database.child("users").child(uid)
                                    userRef.get().addOnSuccessListener { snapshot ->
                                        if (!snapshot.exists()) {
                                            val userData = mapOf(
                                                "email" to email,
                                                "profileImage" to "",
                                                "createdAt" to System.currentTimeMillis(),
                                                "lastLogin" to System.currentTimeMillis()
                                            )
                                            userRef.setValue(userData)
                                        } else {
                                            userRef.child("lastLogin")
                                                .setValue(System.currentTimeMillis())
                                        }
                                    }

                                    Toast.makeText(
                                        auth.app.applicationContext,
                                        "Giriş başarılı ✅",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onLoginSuccess()
                                } else {
                                    message =
                                        "Giriş başarısız: ${task.exception?.localizedMessage ?: "Bilinmeyen hata"}"
                                }
                            }
                    } else {
                        message = "Lütfen e-posta ve şifre girin"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Giriş Yap", color = Color(0xFF0B1C2C))
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onGoToRegister) {
                Text("Hesabın yok mu? Kayıt Ol", color = Color.LightGray)
            }

            Spacer(Modifier.height(8.dp))
            Text(message, color = Color.Red)
        }
    }
}