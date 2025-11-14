package com.elo.smarthomecontrol

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class StartActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Kullanıcı oturum açık → MainActivity’ye git
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // Kullanıcı giriş yapmamış → LoginActivity’ye git
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish()
    }
}