package com.elo.smarthomecontrol

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elo.smarthomecontrol.data.ThemePreferences
import com.elo.smarthomecontrol.ui.theme.SmartHomeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePrefs = ThemePreferences(this)

        setContent {
            val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)

            SmartHomeTheme(darkTheme = isDarkMode) {
                SmartHomeApp(themePrefs)
            }
        }
    }
}

@Composable
fun SmartHomeApp(themePrefs: ThemePreferences) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val userId = auth.uid ?: return
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf("home") }
    val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = if (isDarkMode) Color(0xFF0B1C2C) else Color(0xFFE8EAF6)) {
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Ana Sayfa", tint = if (isDarkMode) Color.White else Color(0xFF0B1C2C)) },
                    label = { Text("Ana Sayfa", color = if (isDarkMode) Color.White else Color(0xFF0B1C2C)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "profile",
                    onClick = { selectedTab = "profile" },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profil", tint = if (isDarkMode) Color.White else Color(0xFF0B1C2C)) },
                    label = { Text("Profil", color = if (isDarkMode) Color.White else Color(0xFF0B1C2C)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "settings",
                    onClick = { selectedTab = "settings" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ayarlar", tint = if (isDarkMode) Color.White else Color(0xFF0B1C2C)) },
                    label = { Text("Ayarlar", color = if (isDarkMode) Color.White else Color(0xFF0B1C2C)) }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            "home" -> SmartHomeDashboard(
                ref = FirebaseDatabase.getInstance().getReference("users").child(userId),
                modifier = Modifier.padding(innerPadding)
            )

            "profile" -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDarkMode) Color(0xFF0B1C2C) else Color.White)
                    .padding(innerPadding)
            ) {
                ProfileScreen(
                    email = auth.currentUser?.email ?: "Bilinmiyor",
                    onLogout = {
                        auth.signOut()
                        (context as? Activity)?.finish()
                    },
                    onPasswordChange = { newPassword ->
                        auth.currentUser?.updatePassword(newPassword)
                    },
                    userId = userId
                )
            }

            "settings" -> SettingsScreen(themePrefs)
        }
    }
}

@Composable
fun SettingsScreen(themePrefs: ThemePreferences) {
    val scope = rememberCoroutineScope()
    val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)

    SmartHomeTheme(darkTheme = isDarkMode) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Ayarlar ⚙️",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Karanlık Tema",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = {
                            scope.launch {
                                themePrefs.setDarkMode(it)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isDarkMode)
                        "🌙 Karanlık tema aktif"
                    else
                        "☀️ Aydınlık tema aktif",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SmartHomeDashboard(ref: DatabaseReference, modifier: Modifier = Modifier) {
    var ledStatus by remember { mutableStateOf("off") }
    var fanStatus by remember { mutableStateOf("off") }
    var pirStatus by remember { mutableStateOf("none") }
    var waterLevel by remember { mutableStateOf("normal") }

    var temperatureList by remember { mutableStateOf(listOf<Double>()) }
    var humidityList by remember { mutableStateOf(listOf<Double>()) }

    LaunchedEffect(Unit) {
        ref.child("devices").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ledStatus = snapshot.child("led").getValue(String::class.java) ?: "off"
                fanStatus = snapshot.child("fan").getValue(String::class.java) ?: "off"
                pirStatus = snapshot.child("pir").getValue(String::class.java) ?: "none"
                waterLevel = snapshot.child("water_level").getValue(String::class.java) ?: "normal"
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        ref.child("sensors/temperature_log").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val values = snapshot.children.mapNotNull { it.getValue(Double::class.java) }
                temperatureList = values.takeLast(5)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        ref.child("sensors/humidity_log").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val values = snapshot.children.mapNotNull { it.getValue(Double::class.java) }
                humidityList = values.takeLast(5)
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
                        ref.child("devices/led").setValue(newState)
                    }
                )
            }

            item {
                DeviceCard(
                    name = "🌬️ Fan",
                    status = fanStatus,
                    onToggle = {
                        val newState = if (fanStatus == "on") "off" else "on"
                        ref.child("devices/fan").setValue(newState)
                    }
                )
            }

            item { InfoCard("🚶 Hareket Sensörü (PIR)", pirStatus) }
            item { InfoCard("💧 Su Seviyesi", waterLevel) }
            item { DualChart(temperatureList, humidityList) }
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
                    checkedThumbColor = Color(0xFF4CAF50),
                    checkedTrackColor = Color(0xFF81C784),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.LightGray
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
fun DualChart(temperatureList: List<Double>, humidityList: List<Double>) {
    if (temperatureList.isEmpty() || humidityList.isEmpty()) {
        Text("Veri bekleniyor...", color = Color.Gray)
        return
    }

    Spacer(Modifier.height(16.dp))
    Text("🌡️ Sıcaklık ve 💧 Nem Grafiği", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(12.dp))

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 8.dp)
    ) {
        val maxTemp = (temperatureList.maxOrNull() ?: 0.0).toFloat()
        val minTemp = (temperatureList.minOrNull() ?: 0.0).toFloat()
        val maxHum = (humidityList.maxOrNull() ?: 0.0).toFloat()
        val minHum = (humidityList.minOrNull() ?: 0.0).toFloat()

        val stepX = size.width / (temperatureList.size - 1)
        val tempStepY = if (maxTemp - minTemp == 0f) 1f else size.height / (maxTemp - minTemp)
        val humStepY = if (maxHum - minHum == 0f) 1f else size.height / (maxHum - minHum)

        for (i in 0 until temperatureList.size - 1) {
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(i * stepX, size.height - ((temperatureList[i].toFloat() - minTemp) * tempStepY)),
                end = Offset((i + 1) * stepX, size.height - ((temperatureList[i + 1].toFloat() - minTemp) * tempStepY)),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }

        for (i in 0 until humidityList.size - 1) {
            drawLine(
                color = Color(0xFF2196F3),
                start = Offset(i * stepX, size.height - ((humidityList[i].toFloat() - minHum) * humStepY)),
                end = Offset((i + 1) * stepX, size.height - ((humidityList[i + 1].toFloat() - minHum) * humStepY)),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    Text("🌡️ Son sıcaklık: ${temperatureList.last().roundToInt()}°C")
    Text("💧 Son nem: ${humidityList.last().roundToInt()}%")
}