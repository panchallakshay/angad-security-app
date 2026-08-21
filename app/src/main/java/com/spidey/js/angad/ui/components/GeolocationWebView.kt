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
import java.net.InetAddress
import java.net.URL

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GeolocationWebView(domain: String, modifier: Modifier = Modifier) {
    var ipAddress by remember { mutableStateOf<String?>(null) }
    var geoData by remember { mutableStateOf<GeoResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Step 1: Resolve domain to IP, then fetch geolocation JSON from API
    LaunchedEffect(domain) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                // Resolve domain -> IP
                val cleanDomain = domain.trim()
                    .removePrefix("http://").removePrefix("https://")
                    .split("/")[0]
                val inet = InetAddress.getByName(cleanDomain)
                val resolvedIp = inet.hostAddress ?: "8.8.8.8"
                ipAddress = resolvedIp

                // Fetch geolocation JSON from our API
                val apiUrl = "https://angad-geo-server.onrender.com/api/geolocation/$resolvedIp"
                val jsonStr = URL(apiUrl).readText()
                val json = JSONObject(jsonStr)

                if (json.optBoolean("success", false)) {
                    val loc = json.getJSONObject("location")
                    geoData = GeoResult(
                        ip = json.optString("ip", resolvedIp),
                        country = loc.optString("country", ""),
                        region = loc.optString("region", ""),
                        city = loc.optString("city", ""),
                        latitude = loc.optDouble("latitude", 0.0),
                        longitude = loc.optDouble("longitude", 0.0),
                        accuracy = loc.optInt("accuracy_radius_km", 25)
                    )
                } else {
                    errorMsg = json.optString("error", "Location not available")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "Connection error"
                // Use a fallback IP if DNS resolution failed
                if (ipAddress == null) ipAddress = "8.8.8.8"
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Status badge
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

        // Step 2: Render the map locally in WebView
        if (geoData != null) {
            val mapHtml = buildMapHtml(geoData!!)

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
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
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
                update = { _ -> /* No-op: map is already loaded */ }
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
    val longitude: Double,
    val accuracy: Int
)

/**
 * Builds a self-contained HTML page with Leaflet.js map.
 * No server dependency - only needs CDN for Leaflet library and map tiles.
 */
private fun buildMapHtml(geo: GeoResult): String {
    val locationLabel = listOf(geo.city, geo.region, geo.country)
        .filter { it.isNotBlank() }
        .joinToString(", ")

    return """
    <!DOCTYPE html>
    <html>
    <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css"/>
    <style>
    *{margin:0;padding:0;box-sizing:border-box}
    body{background:#0d0e15;overflow:hidden;display:flex;flex-direction:column;height:100vh;font-family:sans-serif;color:#fff;gap:6px;padding:6px}
    .hdr{display:flex;justify-content:space-between;align-items:center;background:rgba(255,255,255,.05);border:1px solid rgba(218,165,32,.3);border-radius:6px;padding:6px 10px;font-size:10px}
    .hdr .t{color:#d4af37;font-weight:700;letter-spacing:1px;text-transform:uppercase}
    .hdr .ip{background:rgba(255,23,68,.2);color:#ff5252;border:1px solid rgba(255,23,68,.4);padding:1px 5px;border-radius:4px;font-weight:700;font-size:10px}
    #map{flex:1;border-radius:8px;border:1px solid rgba(255,255,255,.1);min-height:120px}
    .ftr{font-size:10px;color:#999;padding:4px 8px;background:rgba(255,255,255,.03);border-radius:6px}
    .ftr b{color:#fff}
    .leaflet-popup-content-wrapper{background:#151722;color:#fff;border:1px solid #d4af37;border-radius:6px}
    .leaflet-popup-tip{background:#151722}
    </style>
    </head>
    <body>
    <div class="hdr">
      <span class="t">${geo.country.ifBlank { "THREAT ORIGIN" }}</span>
      <span class="ip">${geo.ip}</span>
    </div>
    <div id="map"></div>
    <div class="ftr">
      <b>Location:</b> $locationLabel &nbsp;·&nbsp;
      <b>Coords:</b> ${"%.4f".format(geo.latitude)}, ${"%.4f".format(geo.longitude)}
    </div>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js"></script>
    <script>
    var map=L.map('map',{zoomControl:false,attributionControl:false}).setView([${geo.latitude},${geo.longitude}],11);
    L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',{maxZoom:19,subdomains:'abcd'}).addTo(map);
    var icon=L.divIcon({className:'x',html:"<div style='background:#ff1744;width:14px;height:14px;border-radius:50%;border:2px solid #fff;box-shadow:0 0 12px #ff1744'></div>",iconSize:[14,14],iconAnchor:[7,7]});
    L.marker([${geo.latitude},${geo.longitude}],{icon:icon}).addTo(map).bindPopup("<b style='color:#d4af37'>${geo.city.replace("'", "\\'")}</b> ${geo.country.replace("'", "\\'")}<br><span style='font-size:10px;color:#888'>${geo.ip}</span>").openPopup();
    ${if (geo.accuracy > 0) "L.circle([${geo.latitude},${geo.longitude}],{color:'#ff1744',fillColor:'#ff1744',fillOpacity:.12,weight:1,radius:${geo.accuracy * 1000}}).addTo(map);" else ""}
    setTimeout(function(){map.invalidateSize()},200);
    setTimeout(function(){map.invalidateSize()},600);
    </script>
    </body>
    </html>
    """.trimIndent()
}
