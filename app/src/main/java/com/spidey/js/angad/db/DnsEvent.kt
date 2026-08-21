package com.spidey.js.angad.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dns_events")
data class DnsEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val appPackage: String,
    val appLabel: String,
    val timestamp: Long,
    val queryType: String,
    val isThreat: Boolean = false,
    val threatType: String? = null,
    val riskScore: Double = 0.0,
    val aiMetadata: String? = null // JSON-encoded features and reasons
)
