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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import android.graphics.ImageDecoder
import android.os.Build
import androidx.compose.ui.graphics.asImageBitmap
import java.io.InputStream

class ProfileActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProfileScreen(
                email = auth.currentUser?.email ?: "Bilinmiyor",
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
                },
                userId = auth.uid ?: ""
            )
        }
    }
}

@Composable
fun ProfileScreen(
    email: String,
    onLogout: () -> Unit,
    onPasswordChange: (String) -> Unit,
    userId: String
) {
    val context = LocalContext.current
    val databaseRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)

    var password by remember { mutableStateOf("") }
    var base64Image by remember { mutableStateOf<String?>(null) }
    var bitmapImage by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // 📥 Firebase'den profil resmi oku
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

            // Firebase'e kaydet
            databaseRef.child("profileImage").setValue(base64String)
            Toast.makeText(context, "Profil fotoğrafı güncellendi ✅", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Profil Bilgileri", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(20.dp))

            // Profil Fotoğrafı
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                when {
                    bitmapImage != null -> {
                        Image(
                            bitmap = bitmapImage!!.asImageBitmap(),
                            contentDescription = "Profil Fotoğrafı",
                            modifier = Modifier.size(120.dp).clip(CircleShape)
                        )
                    }
                    base64Image != null -> {
                        val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Firebase Fotoğrafı",
                            modifier = Modifier.size(120.dp).clip(CircleShape)
                        )
                    }
                    else -> {
                        Image(
                            painter = rememberAsyncImagePainter("https://cdn-icons-png.flaticon.com/512/847/847969.png"),
                            contentDescription = "Varsayılan Profil",
                            modifier = Modifier.size(120.dp).clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = { imagePicker.launch("image/*") }) {
                Text("Fotoğraf Değiştir")
            }

            Spacer(modifier = Modifier.height(30.dp))
            Text("E-posta: $email")
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Yeni Şifre") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { onPasswordChange(password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Şifreyi Güncelle")
            }

            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Çıkış Yap")
            }
        }
    }
}