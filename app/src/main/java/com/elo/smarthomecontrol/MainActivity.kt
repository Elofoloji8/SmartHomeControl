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
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*
import kotlin.math.roundToInt
import com.elo.smarthomecontrol.TcpManager.TcpManager

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

    fun updateLocale(
        activity: Activity,
        langCode: String,
        themePrefs: ThemePreferences
    ) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration(activity.resources.configuration)
        config.setLocale(locale)

        activity.resources.updateConfiguration(
            config,
            activity.resources.displayMetrics
        )

        // ✅ DOĞRU lifecycleScope
        (activity as ComponentActivity).lifecycleScope.launch {
            themePrefs.setLanguage(langCode)
        }

        activity.recreate()
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
    fun SmartHomeDashboard(
        ref: DatabaseReference,
        modifier: Modifier = Modifier
    ) {
        var ledStatus by remember { mutableStateOf("off") }
        var fanStatus by remember { mutableStateOf("off") }
        var pirDetected by remember { mutableStateOf(false) }
        var gasDetected by remember { mutableStateOf(false) }

        // ✅ ESP'den gelen yüzde
        var waterLevel by remember { mutableStateOf(0) }

        var temperatureList by remember { mutableStateOf(listOf<Double>()) }

        val tcpManager = remember { TcpManager() }
        val scope = rememberCoroutineScope()

        // WATER
        LaunchedEffect(Unit) {
            while (true) {
                tcpManager.requestWaterLevel { level ->
                    waterLevel = level
                }
                delay(2000)
            }
        }

        //PIR
        LaunchedEffect(Unit) {
            while (true) {
                tcpManager.requestPirStatus { detected ->
                    pirDetected = detected
                }
                delay(1000) // 1 sn yeterli
            }
        }

        // 🔥 Firebase dinleyici (UI sync için)
        LaunchedEffect(Unit) {
            ref.child("devices").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    ledStatus = snapshot.child("led").getValue(String::class.java) ?: "off"
                    fanStatus = snapshot.child("fan").getValue(String::class.java) ?: "off"
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            FirebaseDatabase.getInstance()
                .getReference("sensors")
                .child("temperature_log")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        temperatureList = snapshot.children.mapNotNull { snap ->
                            when (val value = snap.value) {
                                is Long -> value.toDouble()
                                is Double -> value
                                else -> null
                            }
                        }.takeLast(5)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

        }

        // 🎨 Tema
        val themePrefs = ThemePreferences(LocalContext.current)
        val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)

        val gradientColors = if (isDarkMode)
            listOf(Color(0xFF0B1C2C), Color(0xFF1C3A5F))
        else
            listOf(Color(0xFFE8EAF6), Color(0xFFBBDEFB))

        val textColor = if (isDarkMode) Color.White else Color(0xFF0B1C2C)
        val cardBg = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.9f)

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
                .padding(16.dp)
        ) {
            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {

                // 🔷 LOGO
                item {
                    Image(
                        painter = painterResource(id = R.drawable.logo_no_text),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(180.dp)
                            .padding(top = 8.dp, bottom = 24.dp)
                    )
                }

                // 🔷 LED SWITCH
                item {
                    DeviceCardStyled(
                        name = "LED",
                        status = ledStatus,
                        cardBg = cardBg,
                        elevation = 10.dp,
                        textColor = textColor
                    ) {
                        val newState = if (ledStatus == "on") "off" else "on"
                        ledStatus = newState

                        if (newState == "on") {
                            // 💡 LED AÇ
                            tcpManager.sendRgb(255, 255, 255)
                        } else {
                            // 💡 LED KAPAT
                            tcpManager.sendOff()
                        }

                        ref.child("devices/led").setValue(newState)
                    }
                }

                // 🔷 RGB COLOR PICKER
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("RGB LED Control", color = textColor)

                            Spacer(Modifier.height(16.dp))
                            val controller = rememberColorPickerController()

                            HsvColorPicker(
                                modifier = Modifier.size(160.dp),
                                controller = controller,
                                onColorChanged = {
                                    // ❗ LED KAPALIYSA RENK GÖNDERME
                                    if (ledStatus != "on") return@HsvColorPicker

                                    val c = it.color
                                    scope.launch {
                                        tcpManager.sendRgb(
                                            (c.red * 255).toInt(),
                                            (c.green * 255).toInt(),
                                            (c.blue * 255).toInt()
                                        )
                                    }
                                }
                            )

                            Spacer(Modifier.height(12.dp))
                            AlphaSlider(
                                modifier = Modifier.fillMaxWidth(),
                                controller = controller
                            )
                        }
                    }
                }

                // 🔷 FAN
                item {
                    DeviceCardStyled(
                        "Fan",
                        fanStatus,
                        cardBg,
                        10.dp,
                        textColor
                    ) {
                        val newState = if (fanStatus == "on") "off" else "on"
                        fanStatus = newState
                        ref.child("devices/fan").setValue(newState)
                    }
                }

                //SERVO
                item {
                    ServoControlCard(
                        tcpManager = tcpManager,
                        cardBg = cardBg,
                        textColor = textColor
                    )
                }

                // GAZ
                item {
                    GasStatusCard(
                        gasDetected = gasDetected,
                        cardBg = cardBg,
                        textColor = textColor,
                        onRefresh = {
                            tcpManager.requestGasStatus {
                                gasDetected = it
                            }
                        }
                    )
                }

                // 🔷 PIR
                item {
                    InfoCardStyled(
                        "Hareket Sensörü",
                        if (pirDetected) "HAREKET VAR 🚨" else "Hareket yok",
                        cardBg,
                        textColor,
                        10.dp
                    )
                }

                // 🔷 WATER LEVEL
                item {
                    InfoCardStyled(
                        "Su Seviyesi",
                        "%$waterLevel dolu",
                        cardBg,
                        textColor,
                        10.dp
                    )
                }

                // 🔷 GRAFİK
                item {
                    TemperatureChartStyled(temperatureList, textColor)
                }
            }
        }
    }
    @Composable
    fun ServoControlCard(
        tcpManager: TcpManager,
        cardBg: Color,
        textColor: Color
    ) {
        var isOpen by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Garaj Kapısı",
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    Button(
                        onClick = {
                            tcpManager.sendServo(true)   // SERVO:OPEN
                            isOpen = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("AÇ (90°)")
                    }

                    Button(
                        onClick = {
                            tcpManager.sendServo(false)  // SERVO:CLOSE
                            isOpen = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        )
                    ) {
                        Text("KAPAT (0°)")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isOpen) "Durum: AÇIK" else "Durum: KAPALI",
                    color = if (isOpen) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    @Composable
    fun GasStatusCard(
        gasDetected: Boolean,
        cardBg: Color,
        textColor: Color,
        onRefresh: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Text(
                        text = "⛽ Gaz Sensörü",
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (gasDetected) "GAZ VAR ⚠️" else "Gaz yok ✅",
                        color = if (gasDetected) Color.Red else Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(onClick = onRefresh) {
                    Text("Kontrol Et")
                }
            }
        }
    }

    @Composable
    fun DeviceCardStyled(name: String, status: String, cardBg: Color, elevation: Dp, textColor: Color, onToggle: () -> Unit) {
        val isOn = status == "on"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(elevation),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(elevation),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = textColor)
                Text(value, style = MaterialTheme.typography.bodyLarge, color = textColor)
            }
        }
    }

    @Composable
    fun TemperatureChartStyled(
        temperatureList: List<Double>,
        textColor: Color
    ) {
        if (temperatureList.isEmpty()) {
            Text(stringResource(R.string.waiting_data), color = textColor)
            return
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.temp_graph),
            style = MaterialTheme.typography.titleMedium,
            color = textColor
        )

        Spacer(Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 8.dp)
        ) {
            val maxTemp = temperatureList.maxOrNull()!!.toFloat()
            val minTemp = temperatureList.minOrNull()!!.toFloat()

            val count = temperatureList.size.coerceAtLeast(2)
            val stepX = size.width / (count - 1)
            val stepY =
                if (maxTemp - minTemp == 0f) 1f
                else size.height / (maxTemp - minTemp)

            for (i in 0 until temperatureList.size - 1) {
                drawLine(
                    color = Color(0xFF4CAF50),
                    start = Offset(
                        i * stepX,
                        size.height - ((temperatureList[i].toFloat() - minTemp) * stepY)
                    ),
                    end = Offset(
                        (i + 1) * stepX,
                        size.height - ((temperatureList[i + 1].toFloat() - minTemp) * stepY)
                    ),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(
                R.string.last_temperature,
                temperatureList.last().roundToInt()
            ),
            color = textColor
        )
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
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
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