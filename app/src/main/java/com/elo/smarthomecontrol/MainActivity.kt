package com.elo.smarthomecontrol

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.elo.smarthomecontrol.data.ThemePreferences
import com.elo.smarthomecontrol.ui.theme.SmartHomeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themePrefs = ThemePreferences(this)

        // 🔥 İlk olarak dili uygula (UI donmadan)
        lifecycleScope.launch {
            val lang = themePrefs.languageFlow.first()
            applyLocale(this@MainActivity, lang)

            // 🔥 Compose'u burada başlat
            setContent {
                val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)
                SmartHomeTheme(darkTheme = isDarkMode) {
                    SmartHomeApp(themePrefs)
                }
        }
    }
}
    fun applyLocale(activity: Activity, langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        activity.resources.updateConfiguration(
            config,
            activity.resources.displayMetrics
        )
    }

@Composable
fun SmartHomeApp(themePrefs: ThemePreferences) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    val userId = auth.currentUser?.uid ?: run {
        Toast.makeText(context, stringResource(R.string.session_not_found), Toast.LENGTH_SHORT).show()
        context.startActivity(Intent(context, LoginActivity::class.java))
        (context as? Activity)?.finish()
        return
    }

    val email = auth.currentUser?.email ?: stringResource(R.string.unknown)
    var selectedTab by remember { mutableStateOf("home") }
    val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = if (isDarkMode) Color(0xFF0B1C2C) else Color(0xFFE8EAF6)) {
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home" },
                    icon = { Icon(Icons.Default.Home,
                        contentDescription = stringResource(R.string.nav_home),
                        tint = if (isDarkMode) Color.White else Color(0xFF0B1C2C)) },
                    label = { Text(stringResource(R.string.nav_home), color = if (isDarkMode) Color.White else Color(0xFF0B1C2C)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "profile",
                    onClick = { selectedTab = "profile" },
                    icon = { Icon(Icons.Default.AccountCircle,
                        contentDescription = stringResource(R.string.nav_profile),
                        tint = if (isDarkMode) Color.White else Color(0xFF0B1C2C)) },
                    label = { Text(stringResource(R.string.nav_profile)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "settings",
                    onClick = { selectedTab = "settings" },
                    icon = { Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        tint = if (isDarkMode) Color.White else Color(0xFF0B1C2C)) },
                    label = { Text(stringResource(R.string.nav_settings)) }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            "home" -> SmartHomeDashboard(
                ref = FirebaseDatabase.getInstance().getReference("users").child(userId),
                modifier = Modifier.padding(innerPadding)
            )

            "profile" -> ProfileScreen(
                email = email,
                userId = userId,
                onLogout = {
                    auth.signOut()
                    context.startActivity(Intent(context, LoginActivity::class.java))
                    (context as? Activity)?.finish()
                },
                onPasswordChange = { newPassword -> auth.currentUser?.updatePassword(newPassword) }
            )

            "settings" -> SettingsScreen(themePrefs)
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

    // 🔹 Firebase dinleyiciler
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
                temperatureList = snapshot.children.mapNotNull { it.getValue(Double::class.java) }.takeLast(5)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        ref.child("sensors/humidity_log").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                humidityList = snapshot.children.mapNotNull { it.getValue(Double::class.java) }.takeLast(5)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    val themePrefs = ThemePreferences(LocalContext.current)
    val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)
    val gradientColors = if (isDarkMode)
        listOf(Color(0xFF0B1C2C), Color(0xFF1C3A5F))
    else
        listOf(Color(0xFFE8EAF6), Color(0xFFBBDEFB))
    val textColor = if (isDarkMode) Color.White else Color(0xFF0B1C2C)
    val cardBg = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.85f)
    val shadowElevation = if (isDarkMode) 4.dp else 10.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(gradientColors))
            .padding(16.dp)
    ) {
        LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Image(
                    painter = painterResource(id = R.drawable.logo_no_text),
                    contentDescription = stringResource(R.string.logo_desc),
                    modifier = Modifier.size(180.dp).padding(top = 8.dp, bottom = 24.dp)
                )
            }
            item { DeviceCardStyled(stringResource(R.string.device_led), ledStatus, cardBg, shadowElevation, textColor) {
                ref.child("devices/led").setValue(if (ledStatus == "on") "off" else "on")
            }}
            item { DeviceCardStyled(stringResource(R.string.device_fan), fanStatus, cardBg, shadowElevation, textColor) {
                ref.child("devices/fan").setValue(if (fanStatus == "on") "off" else "on")
            }}
            item { InfoCardStyled(stringResource(R.string.pir_sensor), pirStatus, cardBg, textColor, shadowElevation) }
            item { InfoCardStyled(stringResource(R.string.water_level), waterLevel, cardBg, textColor, shadowElevation) }
            item { DualChartStyled(temperatureList, humidityList, textColor) }
        }
    }
}

@Composable
fun DeviceCardStyled(name: String, status: String, cardBg: Color, elevation: Dp, textColor: Color, onToggle: () -> Unit) {
    val isOn = status == "on"
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(elevation),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = textColor)
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
fun InfoCardStyled(name: String, value: String, cardBg: Color, textColor: Color, elevation: Dp) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(elevation),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = textColor)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = textColor)
        }
    }
}

@Composable
fun DualChartStyled(temperatureList: List<Double>, humidityList: List<Double>, textColor: Color) {
    if (temperatureList.isEmpty() || humidityList.isEmpty()) {
        Text(stringResource(R.string.waiting_data))
        return
    }

    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.temp_humidity_graph), style = MaterialTheme.typography.titleMedium, color = textColor)
    Spacer(Modifier.height(12.dp))

    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 8.dp)) {
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
    Text(stringResource(
        R.string.last_temperature,
        temperatureList.last().roundToInt()
    ), color = textColor)
    Text(stringResource(
        R.string.last_humidity,
        humidityList.last().roundToInt()
    ), color = textColor)
}

fun updateLocale(activity: Activity, langCode: String, themePrefs: ThemePreferences) {
    val locale = Locale(langCode)
    Locale.setDefault(locale)
    val config = Configuration(activity.resources.configuration)
    config.setLocale(locale)
    activity.resources.updateConfiguration(config, activity.resources.displayMetrics)

    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch { themePrefs.setLanguage(langCode) }

    val intent = activity.intent
    activity.finish()
    activity.startActivity(intent)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(themePrefs: ThemePreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)
    val storedLanguage by themePrefs.languageFlow.collectAsState(initial = "tr")
    var selectedLanguage by remember { mutableStateOf("tr") }

    LaunchedEffect(storedLanguage) {
        selectedLanguage = storedLanguage
    }
    var is24HourFormat by remember { mutableStateOf(true) }

    val gradientColors = if (isDarkMode)
        listOf(Color(0xFF0B1C2C), Color(0xFF1C3A5F))
    else
        listOf(Color(0xFFE8EAF6), Color(0xFFBBDEFB))
    val textColor = if (isDarkMode) Color.White else Color(0xFF0B1C2C)
    val languages = listOf("Türkçe" to "tr", "English" to "en")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(gradientColors))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(stringResource(R.string.settings_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(24.dp))

            // 🌗 Tema Değişimi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.dark_theme), fontSize = 18.sp, color = textColor)
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { scope.launch { themePrefs.setDarkMode(it) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4CAF50),
                        checkedTrackColor = Color(0xFF81C784)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = textColor.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // 🌐 Dil Seçimi
            Text(stringResource(R.string.language_settings), color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = languages.firstOrNull { it.second == selectedLanguage }?.first
                        ?: stringResource(R.string.language_turkish),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.app_language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        focusedLabelColor = textColor,
                        cursorColor = textColor
                    )
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    languages.forEach { (label, code) ->
                        DropdownMenuItem(
                            text = { Text(label, color = textColor) },
                            onClick = {
                                selectedLanguage = code
                                expanded = false
                                updateLocale(context as Activity, code, themePrefs)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🕒 Saat Formatı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.time_format_24h), fontSize = 18.sp, color = textColor)
                Switch(
                    checked = is24HourFormat,
                    onCheckedChange = {
                        is24HourFormat = it
                        val format = if (it)
                            context.getString(R.string.time_24h)
                        else
                            context.getString(R.string.time_12h)

                        Toast.makeText(
                            context,
                            context.getString(R.string.time_format_changed, format),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}
}