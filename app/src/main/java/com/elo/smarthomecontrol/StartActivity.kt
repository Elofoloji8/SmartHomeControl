package com.elo.smarthomecontrol

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay

class StartActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance().reference

        setContent {
            // 🟦 1. SPLASH EKRANI (Uygulama logosu)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B1C2C)), // Lacivert zemin
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "SmartHomeControl Logo",
                    modifier = Modifier.size(220.dp)
                )
            }

            // 🕒 2. 2 saniye bekle → kullanıcıyı yönlendir
            LaunchedEffect(Unit) {
                delay(2000)

                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val uid = currentUser.uid
                    val userRef = database.child("users").child(uid)

                    // Kullanıcının veritabanında kaydı varsa güncelle, yoksa oluştur
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

                        // Ana ekrana yönlendir
                        startActivity(Intent(this@StartActivity, MainActivity::class.java))
                        finish()
                    }
                } else {
                    // Giriş yapılmamış → LoginActivity’ye yönlendir
                    startActivity(Intent(this@StartActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
}