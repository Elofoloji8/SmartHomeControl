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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.elo.smarthomecontrol.data.ThemePreferences
import com.elo.smarthomecontrol.ui.theme.SmartHomeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
                val email = auth.currentUser?.email ?: ""

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
                                Toast.makeText(
                                    this,
                                    getString(R.string.password_updated),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            ?.addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    getString(R.string.error_with_message, it.message ?: ""),
                                    Toast.LENGTH_LONG
                                ).show()
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
    var createdAt by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf(email) }

    val themePrefs = remember { ThemePreferences(context) }
    val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        databaseRef.get()
            .addOnSuccessListener { snapshot ->
                base64Image = snapshot.child("profileImage").getValue(String::class.java)

                val safeCreatedAt =
                    snapshot.child("createdAt").value?.toString()?.toLongOrNull() ?: 0L
                if (safeCreatedAt > 0L) {
                    val date = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm")
                    createdAt = date.format(java.util.Date(safeCreatedAt))
                }

                userEmail =
                    snapshot.child("email").getValue(String::class.java)
                        ?: email
            }
            .addOnFailureListener {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_data_fetch, it.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
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
            val base64String =
                Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

            databaseRef.child("profileImage").setValue(base64String)

            Toast.makeText(context, context.getString(R.string.photo_updated), Toast.LENGTH_SHORT)
                .show()
        }
    }

    val safeBitmap = remember(base64Image) {
        try {
            base64Image?.let {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            null
        }
    }

    val gradientColors = if (isDarkMode)
        listOf(Color(0xFF0B1C2C), Color(0xFF1C3A5F))
    else
        listOf(Color(0xFFE8EAF6), Color(0xFFBBDEFB))

    val textColor = if (isDarkMode) Color.White else Color(0xFF0B1C2C)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = stringResource(R.string.profile_title),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    bitmapImage != null ->
                        Image(
                            bitmap = bitmapImage!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(130.dp).clip(CircleShape)
                        )

                    safeBitmap != null ->
                        Image(
                            bitmap = safeBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(130.dp).clip(CircleShape)
                        )

                    else ->
                        Image(
                            painter = rememberAsyncImagePainter("https://cdn-icons-png.flaticon.com/512/847/847969.png"),
                            contentDescription = null,
                            modifier = Modifier.size(130.dp).clip(CircleShape)
                        )
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { imagePicker.launch("image/*") },
                shape = RoundedCornerShape(30.dp),
                colors = if (!isDarkMode) {
                    ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF112D44)
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(stringResource(R.string.change_photo))
            }

            Spacer(Modifier.height(30.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {

                    Text(stringResource(R.string.email_label), color = textColor)
                    Text(userEmail ?: "", color = textColor)

                    if (createdAt.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.created_at_label), color = textColor)
                        Text(createdAt, color = textColor)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.new_password_label)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onPasswordChange(password) },
                modifier = Modifier.fillMaxWidth(),
                colors = if (!isDarkMode) {
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF112D44),
                        contentColor = Color.White
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(stringResource(R.string.update_password_button))
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = if (!isDarkMode) {
                    ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFD32F2F)
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(stringResource(R.string.logout_button))
            }
        }
    }
}