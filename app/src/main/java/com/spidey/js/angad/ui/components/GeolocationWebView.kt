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

                // Resolve IP (with Google DoH fallback for VPN-blocked domains)
                var resolvedIp = try {
                    InetAddress.getByName(cleanDomain).hostAddress ?: "0.0.0.0"
                } catch (e: Exception) { "0.0.0.0" }

                if (resolvedIp == "0.0.0.0" || resolvedIp.startsWith("127.") || resolvedIp.startsWith("10.")) {
                    try {
                        val dohConn = URL("https://dns.google/resolve?name=$cleanDomain&type=A").openConnection() as HttpURLConnection
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

                // Fetch geolocation
                val conn = URL("https://ipwho.is/$resolvedIp").openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                conn.disconnect()

                if (json.optBoolean("success", false)) {
                    val geo = GeoResult(
                        ip = resolvedIp,
                        country = json.optString("country", ""),
                        region = json.optString("region", ""),
                        city = json.optString("city", ""),
                        latitude = json.optDouble("latitude", 0.0),
                        longitude = json.optDouble("longitude", 0.0)
                    )
                    geoData = geo

                    // Fetch map tiles and build map bitmap (bypasses VPN WebView block)
                    mapBitmap = buildMapBitmap(geo.latitude, geo.longitude)
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
 * Uses HttpURLConnection (same path as API calls - bypasses VPN WebView block).
 */
private fun buildMapBitmap(lat: Double, lon: Double, zoom: Int = 12): Bitmap? {
    return try {
        val tileSize = 256

        // Convert lat/lon to tile coordinates
        val n = 1 shl zoom
        val xtile = ((lon + 180.0) / 360.0 * n).toInt()
        val ytile = ((1.0 - ln(tan(Math.toRadians(lat)) + 1.0 / cos(Math.toRadians(lat))) / PI) / 2.0 * n).toInt()

        // Fetch 3x3 grid of tiles for wider view
        val gridSize = 3
        val resultBitmap = Bitmap.createBitmap(tileSize * gridSize, tileSize * gridSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Fill background
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#1a1c29") }
        canvas.drawRect(0f, 0f, resultBitmap.width.toFloat(), resultBitmap.height.toFloat(), bgPaint)

        for (dx in -1..1) {
            for (dy in -1..1) {
                val tx = xtile + dx
                val ty = ytile + dy
                try {
                    val tileUrl = "https://tile.openstreetmap.org/$zoom/$tx/$ty.png"
                    val conn = URL(tileUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.setRequestProperty("User-Agent", "AngadSecurityApp/1.0")
                    val tile = BitmapFactory.decodeStream(conn.inputStream)
                    conn.disconnect()
                    if (tile != null) {
                        val px = (dx + 1) * tileSize
                        val py = (dy + 1) * tileSize
                        canvas.drawBitmap(tile, px.toFloat(), py.toFloat(), null)
                        tile.recycle()
                    }
                } catch (_: Exception) { /* skip failed tile */ }
            }
        }

        // Calculate pixel position of marker within the stitched bitmap
        val xExact = (lon + 180.0) / 360.0 * n
        val yExact = (1.0 - ln(tan(Math.toRadians(lat)) + 1.0 / cos(Math.toRadians(lat))) / PI) / 2.0 * n
        val markerX = ((xExact - xtile + 1) * tileSize).toFloat()
        val markerY = ((yExact - ytile + 1) * tileSize).toFloat()

        // Draw red glowing marker
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
