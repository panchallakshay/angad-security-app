package com.spidey.js.angad.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spidey.js.angad.db.DnsEvent
import com.spidey.js.angad.ui.theme.*
import com.spidey.js.angad.util.PreferencesManager
import com.spidey.js.angad.ui.components.GeolocationWebView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityInsightSheet(event: DnsEvent, onDismiss: () -> Unit) {
    var showFullReport by remember { mutableStateOf(false) }

    val metadataMap = remember(event.aiMetadata) {
        event.aiMetadata?.split(";")?.associate {
            val pair = it.split("=", limit = 2)
            if (pair.size == 2) pair[0] to pair[1] else "" to ""
        } ?: emptyMap()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SECURITY INSIGHT",
                style = MaterialTheme.typography.labelLarge,
                color = RoyalGold,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            RiskGauge(score = event.riskScore.toFloat())
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = event.domain, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "Accessed by ${event.appLabel}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            InsightSection("SERVER GEOLOCATION") {
                GeolocationWebView(domain = event.domain)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (!showFullReport) {
                InsightSection("AI TRI-MODEL ANALYSIS") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ModelScoreItem(
                                label = "NETWORK GUARD",
                                sublabel = "Flow / Intrusion",
                                score = metadataMap["M1_Score"] ?: "0.00"
                            )
                            Box(
                                modifier = Modifier.width(1.dp).height(50.dp)
                                    .background(Color.White.copy(alpha = 0.1f))
                            )
                            ModelScoreItem(
                                label = "SHIELDNET AI",
                                sublabel = "Phishing / Scam",
                                score = metadataMap["M2_Score"] ?: "0.00"
                            )
                            Box(
                                modifier = Modifier.width(1.dp).height(50.dp)
                                    .background(Color.White.copy(alpha = 0.1f))
                            )
                            ModelScoreItem(
                                label = "PAYLOADNET",
                                sublabel = "DNS Byte Pattern",
                                score = metadataMap["M3_Score"] ?: "0.00"
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Network Guard: DDoS/Botnet  •  ShieldNet: Phishing/Scam  •  PayloadNet: DNS Tunneling",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                InsightSection("AI ANALYSIS VERDICT") { VerdictRow(event) }
                Spacer(modifier = Modifier.height(16.dp))

                val rawReasons = metadataMap["Reasons"] ?: ""
                val reasons = if (rawReasons.contains("|")) rawReasons.split("|") else rawReasons.split(",")
                val filteredReasons = reasons.map { it.trim() }.filter { it.isNotBlank() }
                if (filteredReasons.isNotEmpty()) {
                    InsightSection("DETECTION REASONS") {
                        filteredReasons.forEach { BulletItem(it) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (event.isThreat || event.riskScore >= 0.50) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val prefManager = remember { PreferencesManager(context) }
                    var allowAdded by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            scope.launch {
                                prefManager.addToAllowlist(event.domain)
                                allowAdded = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LavaCrimson.copy(alpha = 0.25f)),
                        enabled = !allowAdded
                    ) {
                        Text(
                            text = if (allowAdded) "DOMAIN ADDED TO ALLOWLIST" else "ALLOW & CONTINUE TO DOMAIN",
                            color = if (allowAdded) RoyalGold else LavaCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = { showFullReport = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VIEW ENSEMBLE PARAMETERS")
                }
            } else {
                FullParameterReport(metadataMap["Features"])
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showFullReport = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Text("BACK TO SUMMARY")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CLOSE") }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ModelScoreItem(label: String, score: String, sublabel: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = score, style = MaterialTheme.typography.titleLarge, color = RoyalGold, fontWeight = FontWeight.Black)
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
        if (sublabel.isNotBlank()) {
            Text(text = sublabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
        }
    }
}

@Composable
fun FullParameterReport(featuresStr: String?) {
    val features = featuresStr?.split(",")?.associate {
        val pair = it.trim().split("=")
        if (pair.size == 2) pair[0] to pair[1] else "" to ""
    } ?: emptyMap()

    val categories = mutableListOf(
        "URL & DOMAIN STRUCTURE" to listOf(
            "url_length", "host_length", "path_length", "query_length",
            "num_dots", "num_hyphens", "num_underscores", "num_slashes",
            "num_subdomains", "subdomain_length", "path_depth", "query_param_count", "registered_domain_len"
        ),
        "ENTROPY & LINGUISTICS" to listOf(
            "url_entropy", "host_entropy", "path_entropy", "num_digits",
            "digit_ratio", "num_letters", "letter_ratio", "consecutive_consonants",
            "vowel_ratio", "vowel_consonant_ratio", "repeated_char_max"
        ),
        "SECURITY & BRAND SIGNALS" to listOf(
            "suspicious_tld", "suspicious_keywords_count", "brand_in_subdomain",
            "brand_in_path", "brand_mismatch", "has_ip_address", "has_punycode",
            "has_port", "is_https", "has_hex_encoding", "tld_in_subdomain", "has_dangerous_ext"
        )
    )

    // Also display any present legacy or flow keys
    val remainingKeys = features.keys.filter { key ->
        key.isNotBlank() && categories.none { (_, keys) -> keys.contains(key) }
    }
    if (remainingKeys.isNotEmpty()) {
        categories.add("ADDITIONAL PARAMETERS" to remainingKeys)
    }

    categories.forEach { (catName, keys) ->
        val presentKeys = keys.filter { features.containsKey(it) }
        if (presentKeys.isNotEmpty()) {
            Text(text = catName, style = MaterialTheme.typography.labelSmall, color = RoyalGold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp).fillMaxWidth())
            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    presentKeys.forEach { key ->
                        FeatureRow(key.replace("_", " ").uppercase(), features[key] ?: "0.00")
                    }
                }
            }
        }
    }
}

@Composable
fun RiskGauge(score: Float) {
    val color = when {
        score > 0.8f -> LavaCrimson
        score > 0.5f -> DivineSaffron
        else -> RoyalGold
    }
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(100.dp)) {
            drawArc(color = Color.DarkGray.copy(alpha = 0.3f), startAngle = 135f, sweepAngle = 270f, useCenter = false, style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = color, startAngle = 135f, sweepAngle = 270f * score, useCenter = false, style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${(score * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(text = "RISK", style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun InsightSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
fun VerdictRow(event: DnsEvent) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = if (event.isThreat) LavaCrimson else RoyalGold, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = if (event.isThreat) "PURGED: ${event.threatType}" else "ALLOWED: Safe Domain", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (event.isThreat) LavaCrimson else RoyalGold)
    }
}

@Composable
fun BulletItem(text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text("•", color = RoyalGold, modifier = Modifier.padding(end = 8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
    }
}

@Composable
fun FeatureRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 10.sp)
    }
}
