package com.spidey.js.angad.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.spidey.js.angad.ui.theme.RoyalGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

data class GeoResult(
    val ip: String,
    val country: String,
    val region: String,
    val city: String,
    val latitude: Double,
    val longitude: Double
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GeolocationWebView(domain: String, modifier: Modifier = Modifier) {
    var ipAddress by remember { mutableStateOf<String?>(null) }
    var geoData by remember { mutableStateOf<GeoResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(domain) {
        isLoading = true
        errorMsg = null
        withContext(Dispatchers.IO) {
            try {
                val cleanDomain = domain.trim()
                    .removePrefix("http://").removePrefix("https://")
                    .split("/")[0]

                // Try local DNS first
                var resolvedIp = try {
                    InetAddress.getByName(cleanDomain).hostAddress ?: "0.0.0.0"
                } catch (e: Exception) { "0.0.0.0" }

                // If VPN blocked it (0.0.0.0), use Google DNS-over-HTTPS
                if (resolvedIp == "0.0.0.0" || resolvedIp.startsWith("127.") || resolvedIp.startsWith("10.")) {
                    try {
                        val dohUrl = "https://dns.google/resolve?name=$cleanDomain&type=A"
                        val dohConn = URL(dohUrl).openConnection() as HttpURLConnection
                        dohConn.connectTimeout = 4000
                        dohConn.readTimeout = 4000
                        val dohJson = JSONObject(dohConn.inputStream.bufferedReader().readText())
                        dohConn.disconnect()
                        val answers = dohJson.optJSONArray("Answer")
                        if (answers != null && answers.length() > 0) {
                            resolvedIp = answers.getJSONObject(answers.length() - 1).optString("data", resolvedIp)
                        }
                    } catch (_: Exception) {}
                }
                ipAddress = resolvedIp

                // Fetch geolocation from ipwho.is (HTTPS, free, always online)
                val apiUrl = "https://ipwho.is/$resolvedIp"
                val conn = URL(apiUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                val jsonStr = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(jsonStr)
                if (json.optBoolean("success", false)) {
                    geoData = GeoResult(
                        ip = resolvedIp,
                        country = json.optString("country", ""),
                        region = json.optString("region", ""),
                        city = json.optString("city", ""),
                        latitude = json.optDouble("latitude", 0.0),
                        longitude = json.optDouble("longitude", 0.0)
                    )
                } else {
                    errorMsg = "Location unavailable"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "Network error"
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Status
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isLoading -> "RESOLVING DNS..."
                    geoData != null -> "TRACED: ${geoData!!.city}, ${geoData!!.country}"
                    ipAddress != null -> "RESOLVED IP: $ipAddress"
                    else -> "RESOLVING..."
                },
                color = RoyalGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = RoyalGold,
                    strokeWidth = 2.dp
                )
            }
        }

        // Map via OpenStreetMap embed (most reliable - no JS library needed)
        if (geoData != null) {
            val lat = geoData!!.latitude
            val lon = geoData!!.longitude
            val delta = 0.05
            val bbox = "${lon - delta},${lat - delta},${lon + delta},${lat + delta}"
            val mapUrl = "https://www.openstreetmap.org/export/embed.html?bbox=$bbox&layer=mapnik&marker=$lat,$lon"

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp)),
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(android.graphics.Color.parseColor("#0D0E15"))
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        webViewClient = WebViewClient()
                        loadUrl(mapUrl)
                    }
                },
                update = { /* no-op */ }
            )
        } else if (!isLoading && errorMsg != null) {
            Text(
                text = errorMsg ?: "Unable to locate",
                color = Color(0xFFFF5252),
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
