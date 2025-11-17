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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ProfileActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val email = auth.currentUser?.email ?: "Bilinmiyor"
                val userId = auth.uid ?: ""

                ProfileScreen(
                    email = email,
                    userId = userId,
                    onLogout = {
                        auth.signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    onPasswordChange = { newPassword: String ->
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

    // Firebase'den profil resmi oku
    LaunchedEffect(Unit) {
        databaseRef.child("profileImage").get().addOnSuccessListener { snapshot ->
            base64Image = snapshot.value as? String
        }
    }

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

    // 🎨 Lacivert arka plan + beyaz tonlar
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1C2C))
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Profil Bilgileri",
                fontSize = 26.sp,
                color = Color(0xFFF5F5F5),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🖼 Profil Fotoğrafı
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
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

            Spacer(modifier = Modifier.height(14.dp))

            // 🌤 Fotoğraf Değiştir butonu (beyaz)
            Button(
                onClick = { imagePicker.launch("image/*") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = Color(0xFF0B1C2C)
                ),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text("Fotoğraf Değiştir", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 📨 Bilgi kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("E-posta", color = Color.White.copy(alpha = 0.6f))
                    Text(email, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Yeni Şifre", color = Color.White) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                    cursorColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 💾 Şifre Güncelle butonu
            Button(
                onClick = { onPasswordChange(password) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0B1C2C)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Şifreyi Güncelle", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🚪 Çıkış Yap butonu
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.8f),
                    contentColor = Color(0xFFD32F2F)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Çıkış Yap", fontWeight = FontWeight.Bold)
            }
        }
    }
}