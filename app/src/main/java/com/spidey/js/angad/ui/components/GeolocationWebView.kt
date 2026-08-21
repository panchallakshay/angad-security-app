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
                // Step 1: Resolve domain -> IP
                val cleanDomain = domain.trim()
                    .removePrefix("http://").removePrefix("https://")
                    .split("/")[0]

                // Try local DNS first
                var resolvedIp = try {
                    InetAddress.getByName(cleanDomain).hostAddress ?: "0.0.0.0"
                } catch (e: Exception) { "0.0.0.0" }

                // If VPN blocked it (0.0.0.0) or it's a private IP, use Google DNS-over-HTTPS
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
                    } catch (e: Exception) { /* keep whatever we had */ }
                }
                ipAddress = resolvedIp

                // Step 2: Fetch geolocation from ipwho.is (free, HTTPS, always online)
                val apiUrl = "https://ipwho.is/$resolvedIp"
                val conn = URL(apiUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.requestMethod = "GET"

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
                    errorMsg = "Location unavailable for this IP"
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
        // Status row
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

        // Map or error
        if (geoData != null) {
            val mapHtml = remember(geoData) { buildMapHtml(geoData!!) }

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
                        loadDataWithBaseURL(
                            "https://cdnjs.cloudflare.com",
                            mapHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
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

data class GeoResult(
    val ip: String,
    val country: String,
    val region: String,
    val city: String,
    val latitude: Double,
    val longitude: Double
)

private fun buildMapHtml(geo: GeoResult): String {
    val safeCity = geo.city.replace("'", "\\'").replace("\"", "&quot;")
    val safeCountry = geo.country.replace("'", "\\'").replace("\"", "&quot;")

    return """<!DOCTYPE html><html><head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css"/>
<style>
*{margin:0;padding:0}
html,body{width:100%;height:100%;overflow:hidden;background:#0d0e15}
#map{width:100%;height:100%;position:absolute;top:0;left:0;right:0;bottom:0}
.leaflet-popup-content-wrapper{background:#151722;color:#fff;border:1px solid #d4af37;border-radius:5px;font-size:11px}
.leaflet-popup-tip{background:#151722}
</style></head><body>
<div id="map"></div>
<script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js"></script>
<script>
var m=L.map('map',{zoomControl:false,attributionControl:false}).setView([${geo.latitude},${geo.longitude}],12);
L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',{maxZoom:19,subdomains:'abcd'}).addTo(m);
var ic=L.divIcon({className:'x',html:"<div style='background:#ff1744;width:14px;height:14px;border-radius:50%;border:2px solid #fff;box-shadow:0 0 12px #ff1744'></div>",iconSize:[14,14],iconAnchor:[7,7]});
L.marker([${geo.latitude},${geo.longitude}],{icon:ic}).addTo(m).bindPopup("<b style='color:#d4af37'>${safeCity}</b> ${safeCountry}<br><span style='font-size:10px;color:#888'>${geo.ip}</span>").openPopup();
setTimeout(function(){m.invalidateSize()},150);
setTimeout(function(){m.invalidateSize()},400);
</script></body></html>"""
}
