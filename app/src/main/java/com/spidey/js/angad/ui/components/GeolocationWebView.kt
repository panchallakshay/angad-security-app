package com.spidey.js.angad.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spidey.js.angad.ui.theme.RoyalGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import kotlin.math.*

data class GeoResult(
    val ip: String,
    val country: String,
    val region: String,
    val city: String,
    val latitude: Double,
    val longitude: Double
)

@Composable
fun GeolocationWebView(domain: String, modifier: Modifier = Modifier) {
    var ipAddress by remember { mutableStateOf<String?>(null) }
    var geoData by remember { mutableStateOf<GeoResult?>(null) }
    var mapBitmap by remember { mutableStateOf<Bitmap?>(null) }
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

                // STEP 1: Resolve Domain to IP (Bypass local DNS if needed)
                var resolvedIp = try {
                    InetAddress.getByName(cleanDomain).hostAddress ?: "0.0.0.0"
                } catch (e: Exception) { "0.0.0.0" }

                // Robust Multi-DoH Fallback for Blocked Domains
                if (resolvedIp == "0.0.0.0" || resolvedIp.startsWith("127.") || resolvedIp.startsWith("10.")) {
                    var dohSuccess = false
                    val dohUrls = listOf(
                        "https://cloudflare-dns.com/dns-query?name=$cleanDomain&type=A",
                        "https://dns.alidns.com/resolve?name=$cleanDomain&type=1"
                    )
                    
                    for (dohUrl in dohUrls) {
                        try {
                            val dohConn = URL(dohUrl).openConnection() as HttpURLConnection
                            dohConn.setRequestProperty("accept", "application/dns-json")
                            dohConn.setRequestProperty("User-Agent", "AngadSecurityApp/1.0")
                            dohConn.connectTimeout = 6000
                            dohConn.readTimeout = 6000
                            val jsonStr = dohConn.inputStream.bufferedReader().readText()
                            dohConn.disconnect()
                            
                            val dohJson = JSONObject(jsonStr)
                            val answers = dohJson.optJSONArray("Answer")
                            if (answers != null) {
                                for (i in 0 until answers.length()) {
                                    val answer = answers.getJSONObject(i)
                                    if (answer.optInt("type") == 1) { // Type 1 is A record (IPv4)
                                        resolvedIp = answer.optString("data", resolvedIp)
                                        dohSuccess = true
                                        break
                                    }
                                }
                            }
                            if (dohSuccess) break
                        } catch (_: Exception) {}
                    }
                }
                ipAddress = resolvedIp

                // STEP 2: Robust Multi-API Geolocation Fetching
                var geo: GeoResult? = null
                
                // Try 1: ip-api.com (Direct IP, fastest but has 45/min rate limit)
                if (geo == null) {
                    try {
                        val apiUrl1 = "http://208.95.112.1/json/$resolvedIp?fields=status,country,regionName,city,lat,lon"
                        val conn1 = URL(apiUrl1).openConnection() as HttpURLConnection
                        conn1.connectTimeout = 4000
                        conn1.readTimeout = 4000
                        val jsonStr1 = conn1.inputStream.bufferedReader().readText()
                        conn1.disconnect()
                        val json1 = JSONObject(jsonStr1)
                        if (json1.optString("status") == "success") {
                            geo = GeoResult(resolvedIp, json1.optString("country", ""), json1.optString("regionName", ""), json1.optString("city", ""), json1.optDouble("lat", 0.0), json1.optDouble("lon", 0.0))
                        }
                    } catch (e: Exception) { /* Rate limited or HTTP blocked */ }
                }

                // Try 2: ipwho.is (HTTPS, highly reliable, handles Anycast IPs well)
                if (geo == null) {
                    try {
                        val apiUrl2 = "https://ipwho.is/$resolvedIp"
                        val conn2 = URL(apiUrl2).openConnection() as HttpURLConnection
                        conn2.connectTimeout = 4000
                        conn2.readTimeout = 4000
                        val jsonStr2 = conn2.inputStream.bufferedReader().readText()
                        conn2.disconnect()
                        val json2 = JSONObject(jsonStr2)
                        if (json2.optBoolean("success", false)) {
                            geo = GeoResult(resolvedIp, json2.optString("country", ""), json2.optString("region", ""), json2.optString("city", ""), json2.optDouble("latitude", 0.0), json2.optDouble("longitude", 0.0))
                        }
                    } catch (e: Exception) { /* Network error */ }
                }

                // Try 3: Our Custom Render Backend (Slowest cold start, but no rate limits + GeoLite2 database)
                if (geo == null) {
                    try {
                        val apiUrl3 = "https://angad-geo-server.onrender.com/api/geolocation/$resolvedIp"
                        val conn3 = URL(apiUrl3).openConnection() as HttpURLConnection
                        conn3.connectTimeout = 8000
                        conn3.readTimeout = 8000
                        val jsonStr3 = conn3.inputStream.bufferedReader().readText()
                        conn3.disconnect()
                        val json3 = JSONObject(jsonStr3)
                        if (json3.optBoolean("success", false)) {
                            val loc = json3.getJSONObject("location")
                            geo = GeoResult(resolvedIp, loc.optString("country", ""), loc.optString("region", ""), loc.optString("city", ""), loc.optDouble("latitude", 0.0), loc.optDouble("longitude", 0.0))
                        }
                    } catch (e: Exception) { /* Backend asleep/dead */ }
                }

                if (geo != null) {
                    geoData = geo
                    // STEP 3: Fetch map tiles robustly via Canvas
                    mapBitmap = buildMapBitmap(geo.latitude, geo.longitude)
                } else {
                    errorMsg = "Location unavailable (All APIs failed)"
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

        if (mapBitmap != null) {
            Image(
                bitmap = mapBitmap!!.asImageBitmap(),
                contentDescription = "IP Location Map",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1a1c29)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RoyalGold, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        } else if (errorMsg != null) {
            Text(text = errorMsg ?: "", color = Color(0xFFFF5252), fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

/**
 * Fetches a 3x3 grid of OSM tiles, stitches them into one bitmap,
 * and draws a red marker at the target coordinates.
 * Resolves OSM tiles via Google DoH to bypass local DNS failures.
 */
private fun buildMapBitmap(lat: Double, lon: Double, zoom: Int = 12): Bitmap? {
    return try {
        val tileSize = 256
        val n = 1 shl zoom
        val xtile = ((lon + 180.0) / 360.0 * n).toInt()
        val ytile = ((1.0 - ln(tan(Math.toRadians(lat)) + 1.0 / cos(Math.toRadians(lat))) / PI) / 2.0 * n).toInt()

        val gridSize = 3
        val resultBitmap = Bitmap.createBitmap(tileSize * gridSize, tileSize * gridSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#1a1c29") }
        canvas.drawRect(0f, 0f, resultBitmap.width.toFloat(), resultBitmap.height.toFloat(), bgPaint)

        for (dx in -1..1) {
            for (dy in -1..1) {
                val tx = xtile + dx
                val ty = ytile + dy
                try {
                    // Fetch tile using standard HTTPS to avoid Android cleartext/Host header blocks
                    val tileUrl = "https://tile.openstreetmap.org/$zoom/$tx/$ty.png"
                    val conn = URL(tileUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    conn.setRequestProperty("User-Agent", "AngadSecurityApp/1.0")
                    val tile = BitmapFactory.decodeStream(conn.inputStream)
                    conn.disconnect()
                    if (tile != null) {
                        val px = (dx + 1) * tileSize
                        val py = (dy + 1) * tileSize
                        canvas.drawBitmap(tile, px.toFloat(), py.toFloat(), null)
                        tile.recycle()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        // Calculate marker position
        val xExact = (lon + 180.0) / 360.0 * n
        val yExact = (1.0 - ln(tan(Math.toRadians(lat)) + 1.0 / cos(Math.toRadians(lat))) / PI) / 2.0 * n
        val markerX = ((xExact - xtile + 1) * tileSize).toFloat()
        val markerY = ((yExact - ytile + 1) * tileSize).toFloat()

        val glowPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#44FF1744")
            isAntiAlias = true
        }
        canvas.drawCircle(markerX, markerY, 24f, glowPaint)

        val markerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#FF1744")
            isAntiAlias = true
        }
        canvas.drawCircle(markerX, markerY, 10f, markerPaint)

        val borderPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawCircle(markerX, markerY, 10f, borderPaint)

        resultBitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

