package com.elo.smarthomecontrol

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.elo.smarthomecontrol.data.ThemePreferences
import com.elo.smarthomecontrol.ui.theme.SmartHomeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ProfileActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themePrefs = ThemePreferences(this)

        setContent {
            val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)

            SmartHomeTheme(darkTheme = isDarkMode) {
                val userId = auth.uid ?: ""
                val email = auth.currentUser?.email ?: "Bilinmiyor"

                ProfileScreen(
                    email = email,
                    userId = userId,
                    onLogout = {
                        auth.signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    onPasswordChange = { newPassword ->
                        auth.currentUser?.updatePassword(newPassword)
                            ?.addOnSuccessListener {
                                Toast.makeText(this, "Şifre güncellendi ✅", Toast.LENGTH_SHORT).show()
                            }
                            ?.addOnFailureListener {
                                Toast.makeText(this, "Hata: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    email: String,
    userId: String,
    onLogout: () -> Unit,
    onPasswordChange: (String) -> Unit
) {
    val context = LocalContext.current
    val databaseRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)

    var password by remember { mutableStateOf("") }
    var base64Image by remember { mutableStateOf<String?>(null) }
    var bitmapImage by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var createdAt by remember { mutableStateOf<String>("") }

    val scope = rememberCoroutineScope()
    val themePrefs = remember { ThemePreferences(context) }
    val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)

    // 🔄 Firebase'den kullanıcı verilerini al
    LaunchedEffect(Unit) {
        databaseRef.get().addOnSuccessListener { snapshot ->
            base64Image = snapshot.child("profileImage").getValue(String::class.java)
            val createdAtMillis = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
            if (createdAtMillis > 0L) {
                val date = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                createdAt = date.format(java.util.Date(createdAtMillis))
            }
        }
    }

    // 📷 Galeriden fotoğraf seçme
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            bitmapImage = bitmap

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

            databaseRef.child("profileImage").setValue(base64String)
            Toast.makeText(context, "Profil fotoğrafı güncellendi ✅", Toast.LENGTH_SHORT).show()
        }
    }

    // 🎨 Temaya göre arka plan gradient renkleri
    val gradientColors = if (isDarkMode) {
        listOf(Color(0xFF0B1C2C), Color(0xFF1C3A5F))
    } else {
        listOf(Color(0xFFE8EAF6), Color(0xFFBBDEFB))
    }

    val textColor = if (isDarkMode) Color.White else Color(0xFF0B1C2C)

    // 🎨 Buton renkleri
    val darkBlue = Color(0xFF0B1C2C)
    val navyBlue = Color(0xFF112D44)

    val photoButtonColors = if (isDarkMode) {
        ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.9f),
            contentColor = darkBlue
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = navyBlue,
            contentColor = Color.White
        )
    }

    val primaryButtonColors = if (isDarkMode) {
        ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = darkBlue
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = navyBlue,
            contentColor = Color.White
        )
    }

    val secondaryButtonColors = if (isDarkMode) {
        ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.8f),
            contentColor = Color(0xFFD32F2F)
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFCDD2),
            contentColor = Color(0xFFB71C1C)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(gradientColors))
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = "Profil Bilgileri",
                fontSize = 26.sp,
                color = textColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Profil Fotoğrafı
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    bitmapImage != null -> {
                        Image(
                            bitmap = bitmapImage!!.asImageBitmap(),
                            contentDescription = "Profil Fotoğrafı",
                            modifier = Modifier.size(130.dp).clip(CircleShape)
                        )
                    }

                    base64Image != null -> {
                        val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Firebase Fotoğrafı",
                            modifier = Modifier.size(130.dp).clip(CircleShape)
                        )
                    }

                    else -> {
                        Image(
                            painter = rememberAsyncImagePainter("https://cdn-icons-png.flaticon.com/512/847/847969.png"),
                            contentDescription = "Varsayılan Profil",
                            modifier = Modifier.size(130.dp).clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { imagePicker.launch("image/*") },
                colors = photoButtonColors,
                shape = RoundedCornerShape(30.dp)
            ) {
                Text("Fotoğraf Değiştir", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Bilgiler Kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("E-posta", color = textColor.copy(alpha = 0.7f))
                    Text(email, color = textColor, fontWeight = FontWeight.Medium)

                    if (createdAt.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Kayıt Tarihi", color = textColor.copy(alpha = 0.7f))
                        Text(createdAt, color = textColor, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Yeni Şifre") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onPasswordChange(password) },
                modifier = Modifier.fillMaxWidth(),
                colors = primaryButtonColors,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Şifreyi Güncelle", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLogout,
                colors = secondaryButtonColors,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Çıkış Yap", fontWeight = FontWeight.Bold)
            }
        }
    }
}