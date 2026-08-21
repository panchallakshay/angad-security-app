package com.spidey.js.angad

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spidey.js.angad.ui.theme.*
import com.spidey.js.angad.util.PreferencesManager
import kotlinx.coroutines.launch

class ThreatAlertActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DOMAIN = "extra_domain"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_RISK_SCORE = "extra_risk_score"
        const val EXTRA_THREAT_TYPE = "extra_threat_type"
        const val EXTRA_REASONS = "extra_reasons"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val domain = intent.getStringExtra(EXTRA_DOMAIN) ?: "Unknown Domain"
        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL) ?: "Browser"
        val riskScore = intent.getFloatExtra(EXTRA_RISK_SCORE, 0.95f)
        val threatType = intent.getStringExtra(EXTRA_THREAT_TYPE) ?: "Phishing Threat"
        val reasons = intent.getStringArrayExtra(EXTRA_REASONS)?.toList() ?: listOf("Malicious link pattern detected")

        setContent {
            AngadTheme {
                ThreatAlertPopup(
                    domain = domain,
                    appLabel = appLabel,
                    riskScore = riskScore,
                    threatType = threatType,
                    reasons = reasons,
                    onAllow = {
                        val prefManager = PreferencesManager(this@ThreatAlertActivity)
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            prefManager.addToAllowlist(domain)
                        }
                        Toast.makeText(this@ThreatAlertActivity, "✅ Domain whitelisted. You can reload the page.", Toast.LENGTH_LONG).show()
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
fun ThreatAlertPopup(
    domain: String,
    appLabel: String,
    riskScore: Float,
    threatType: String,
    reasons: List<String>,
    onAllow: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "auraScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TempleSurface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Warning Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(90.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().scale(auraScale)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(LavaCrimson.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(LavaCrimson.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = LavaCrimson,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "🛡️ ANGAD SHIELD ALERT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = RoyalGold,
                        letterSpacing = 2.sp
                    )
                )

                Text(
                    text = "MALICIOUS LINK BLOCKED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = LavaCrimson,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Domain Container
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = domain,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Accessed via $appLabel • Risk: ${(riskScore * 100).toInt()}% ($threatType)",
                            style = MaterialTheme.typography.labelSmall,
                            color = LavaCrimson
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bullet reasons
                Column(modifier = Modifier.fillMaxWidth()) {
                    reasons.take(3).forEach { reason ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("⚠️", fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = AncientWhite.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LavaCrimson),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🔙 GO BACK TO SAFETY",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalGold)
                ) {
                    Text(
                        text = "⚠️ STILL CONTINUE ANYWAY (BYPASS)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
