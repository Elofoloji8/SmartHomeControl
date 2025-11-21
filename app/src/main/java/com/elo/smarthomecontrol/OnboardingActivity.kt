package com.elo.smarthomecontrol

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elo.smarthomecontrol.ui.theme.SmartHomeTheme
import kotlinx.coroutines.launch
import java.util.*

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 💡 Locale bilgisi doğrudan Activity'den okunuyor (LocalContext yok)
        val currentLocale = resources.configuration.locales[0].language

        setContent {
            SmartHomeTheme {
                OnboardingScreen(
                    currentLocale = currentLocale,
                    onFinish = {
                        startActivity(Intent(this, StartActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(currentLocale: String, onFinish: () -> Unit) {
    val pages = remember {
        if (currentLocale == "en") {
            listOf(
                OnboardingPage(
                    title = "Device Control",
                    description = "Easily control your smart home devices.",
                    imageRes = R.drawable.device_control
                ),
                OnboardingPage(
                    title = "Data Tracking",
                    description = "Monitor temperature, humidity and sensor data instantly.",
                    imageRes = R.drawable.data_tracking
                ),
                OnboardingPage(
                    title = "Profile",
                    description = "Manage your profile and personalize your theme.",
                    imageRes = R.drawable.profile
                )
            )
        } else {
            listOf(
                OnboardingPage(
                    title = "Cihaz Kontrolü",
                    description = "Akıllı ev cihazlarını kolayca kontrol edin.",
                    imageRes = R.drawable.device_control
                ),
                OnboardingPage(
                    title = "Veri Takibi",
                    description = "Sıcaklık, nem ve sensör verilerini anlık takip edin.",
                    imageRes = R.drawable.data_tracking
                ),
                OnboardingPage(
                    title = "Profil",
                    description = "Profilinizi yönetin ve temayı kişiselleştirin.",
                    imageRes = R.drawable.profile
                )
            )
        }
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFE8EAF6), Color(0xFFBBDEFB))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 40.dp), // 🔹 Alt kısımda yer açıldı
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween // 🔹 Butonun her zaman görünmesini sağlar
        ) {
            // 🔹 Pager’ı üst kısımda göster
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val item = pages[page]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = item.imageRes),
                        contentDescription = item.title,
                        modifier = Modifier.size(250.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(item.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    Text(item.description, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 🔹 Sayfa noktaları
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                repeat(pages.size) { index ->
                    val color =
                        if (pagerState.currentPage == index) Color(0xFF0B1C2C) else Color.Gray.copy(alpha = 0.4f)
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(10.dp)
                            .background(color, shape = MaterialTheme.shapes.small)
                    )
                }
            }

            // 🔹 İleri / Başla butonu
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.lastIndex) {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B1C2C)),
                modifier = Modifier.fillMaxWidth(0.8f) // orta genişlikte buton
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.lastIndex)
                        if (currentLocale == "en") "Start" else "Başla"
                    else
                        if (currentLocale == "en") "Next" else "İleri",
                    color = Color.White
                )
            }

            // 🔹 Kaydırma ipucu
            Text(
                text = if (currentLocale == "en") "Swipe to explore →" else "Kaydırarak keşfet →",
                color = Color.Gray.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val imageRes: Int
)