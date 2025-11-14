package com.elo.smarthomecontrol

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartHomeApp()
        }
    }
}

@Composable
fun SmartHomeApp() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var selectedTab by remember { mutableStateOf("home") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Ana Sayfa") },
                    label = { Text("Ana Sayfa") }
                )
                NavigationBarItem(
                    selected = selectedTab == "profile",
                    onClick = { selectedTab = "profile" },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profil") },
                    label = { Text("Profil") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            "home" -> SmartHomeDashboard(
                ref = FirebaseDatabase.getInstance().getReference("devices"),
                modifier = Modifier.padding(innerPadding)
            )

            "profile" -> ProfileScreen(
                email = auth.currentUser?.email ?: "Bilinmiyor",
                onLogout = {
                    auth.signOut()
                    (context as? Activity)?.finish()
                },
                onPasswordChange = { newPassword ->
                    auth.currentUser?.updatePassword(newPassword)
                },
                userId = auth.uid ?: ""
            )
        }
    }
}

@Composable
fun SmartHomeDashboard(ref: DatabaseReference, modifier: Modifier = Modifier) {
    var ledStatus by remember { mutableStateOf("off") }
    var fanStatus by remember { mutableStateOf("off") }
    var pirStatus by remember { mutableStateOf("none") }
    var waterLevel by remember { mutableStateOf("normal") }

    LaunchedEffect(Unit) {
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ledStatus = snapshot.child("led").getValue(String::class.java) ?: "off"
                fanStatus = snapshot.child("fan").getValue(String::class.java) ?: "off"
                pirStatus = snapshot.child("pir").getValue(String::class.java) ?: "none"
                waterLevel = snapshot.child("water_level").getValue(String::class.java) ?: "normal"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Text("🏠 Akıllı Ev Kontrol Paneli", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(24.dp))
            }

            item {
                DeviceCard(
                    name = "💡 LED Işığı",
                    status = ledStatus,
                    onToggle = {
                        val newState = if (ledStatus == "on") "off" else "on"
                        ref.child("led").setValue(newState)
                    }
                )
            }

            item {
                DeviceCard(
                    name = "🌬️ Fan",
                    status = fanStatus,
                    onToggle = {
                        val newState = if (fanStatus == "on") "off" else "on"
                        ref.child("fan").setValue(newState)
                    }
                )
            }

            item {
                InfoCard(name = "🚶 Hareket Sensörü (PIR)", value = pirStatus)
            }

            item {
                InfoCard(name = "💧 Su Seviyesi", value = waterLevel)
            }

            item {
                TemperatureChart(ref = FirebaseDatabase.getInstance().reference)
            }
        }
    }
}

@Composable
fun DeviceCard(name: String, status: String, onToggle: () -> Unit) {
    val isOn = status == "on"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = isOn,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@Composable
fun InfoCard(name: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun TemperatureChart(ref: DatabaseReference) {
    var temperatureList by remember { mutableStateOf(listOf<Double>()) }

    // 🔁 Firebase dinleyicisi
    LaunchedEffect(Unit) {
        ref.child("sensors/temperature_log").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val values = snapshot.children.mapNotNull { it.getValue(Double::class.java) }
                temperatureList = values.takeLast(5)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    if (temperatureList.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text("🌡️ Son 5 Dakikalık Sıcaklık Değişimi", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 8.dp)
        ) {
            val maxTemp = (temperatureList.maxOrNull() ?: 0.0).toFloat()
            val minTemp = (temperatureList.minOrNull() ?: 0.0).toFloat()
            val stepX = size.width / (temperatureList.size - 1)
            val stepY = if (maxTemp - minTemp == 0f) 1f else size.height / (maxTemp - minTemp)

            for (i in 0 until temperatureList.size - 1) {
                val start = Offset(
                    x = i * stepX,
                    y = size.height - ((temperatureList[i].toFloat() - minTemp) * stepY)
                )
                val end = Offset(
                    x = (i + 1) * stepX,
                    y = size.height - ((temperatureList[i + 1].toFloat() - minTemp) * stepY)
                )

                drawLine(
                    color = Color(0xFF4CAF50),
                    start = start,
                    end = end,
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Son ölçüm: ${temperatureList.last().roundToInt()}°C")
    } else {
        Text("Veri bekleniyor...", color = Color.Gray)
    }
}