package com.sonbum.diacalendar2.presentation.officewebsite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonbum.diacalendar2.data.local.OfficeWebsiteRegistry
import com.sonbum.diacalendar2.ui.theme.DiaCalendar2Theme
import org.koin.android.ext.android.inject

class OfficeWebsiteActivity : ComponentActivity() {
    private val registry: OfficeWebsiteRegistry by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val officeName = intent.getStringExtra("officeName") ?: ""
        val incomingUrl = intent.getStringExtra("url")
        val dateParam = incomingUrl?.let { extractDateParam(it) }
        val variantUrls = registry.getVariantUrls(officeName)
        val dayUrl = variantUrls["day"]?.let { withDate(it, dateParam) }
        val monthUrl = variantUrls["month"]?.let { withDate(it, dateParam) }
        val initialUrl = dayUrl
            ?: monthUrl
            ?: incomingUrl
            ?: run { finish(); return }

        enableEdgeToEdge()
        setContent {
            DiaCalendar2Theme {
                var currentUrl by remember { mutableStateOf(initialUrl) }
                var webView by remember { mutableStateOf<android.webkit.WebView?>(null) }

                Column(modifier = Modifier.fillMaxSize()) {
                    OfficeWebsiteScreen(
                        url = currentUrl,
                        officeName = officeName,
                        onBack = { finish() },
                        onWebViewReady = { webView = it },
                        modifier = Modifier.weight(1f)
                    )

                    if (variantUrls.size >= 2 && dayUrl != null && monthUrl != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(horizontal = 5.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    currentUrl = dayUrl
                                    webView?.loadUrl(dayUrl)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .padding(horizontal = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentUrl == dayUrl) Color.Blue else Color.Gray,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Day", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    currentUrl = monthUrl
                                    webView?.loadUrl(monthUrl)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .padding(horizontal = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentUrl == monthUrl) Color.Blue else Color.Gray,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Month", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val DATE_PARAM_REGEX = Regex("""[?&]date=(\d{4}-\d{2}-\d{2})""")

private fun extractDateParam(url: String): String? =
    DATE_PARAM_REGEX.find(url)?.groupValues?.get(1)

private fun withDate(url: String, date: String?): String {
    if (date.isNullOrBlank()) return url
    if (DATE_PARAM_REGEX.containsMatchIn(url)) return url
    val separator = if (url.contains('?')) '&' else '?'
    return "$url${separator}date=$date"
}
