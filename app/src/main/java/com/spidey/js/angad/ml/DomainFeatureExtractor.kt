package com.spidey.js.angad.ml

import android.net.Uri
import java.util.*
import kotlin.math.ln

object DomainFeatureExtractor {

    private val RISKY_TLDS = setOf(
        ".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top", ".pw", ".cc", ".su",
        ".buzz", ".club", ".work", ".shop", ".online", ".site", ".live", ".icu"
    )

    private val SUSPICIOUS_KEYWORDS = setOf(
        "login", "signin", "verify", "verification", "secure", "account", "banking",
        "update", "support", "confirm", "password", "wallet", "auth", "billing",
        "service", "portal", "admin", "recover", "token", "security", "appleid",
        "paypal", "netflix", "microsoft", "google", "amazon", "facebook"
    )

    private val POPULAR_BRANDS = setOf(
        "google", "apple", "microsoft", "amazon", "facebook", "paypal", "netflix",
        "instagram", "whatsapp", "coinbase", "binance", "metamask", "telegram",
        "twitter", "linkedin", "dropbox", "chase", "wellsfargo", "bankofamerica"
    )

    private val COMMON_TLDS = setOf(
        ".com", ".org", ".net", ".edu", ".gov", ".co", ".io", ".info"
    )

    private val DANGEROUS_EXTENSIONS = setOf(
        ".exe", ".apk", ".zip", ".rar", ".scr", ".bat", ".bin", ".dmg", ".sh", ".vbs", ".msi"
    )

    private val TRUSTED_INFRASTRUCTURE = setOf(
        // Google & Android
        "google.com", "googleapis.com", "gstatic.com", "googleusercontent.com",
        "android.com", "gvt1.com", "ggpht.com", "googlevideo.com", "youtube.com", "ytimg.com",
        "play.google.com",
        // Meta / Facebook / WhatsApp / Instagram
        "facebook.com", "fbcdn.net", "instagram.com", "cdninstagram.com", "whatsapp.com", "whatsapp.net",
        "meta.com", "facebook.net", "fbsbx.com",
        // Apple
        "apple.com", "icloud.com", "aaplimg.com", "mzstatic.com",
        // Microsoft
        "microsoft.com", "live.com", "office.com", "windows.com", "msn.com", "bing.com", "azure.com", "msftncsi.com",
        // Samsung & Device OEMs
        "samsung.com", "samsungcloud.com", "samsungapps.com", "samsungqbe.com",
        // CDNs & Infrastructure
        "cloudflare.com", "cloudfront.net", "akamaihd.net", "akamaized.net", "fastly.net", "edgekey.net",
        // Common Essential Services
        "jio.com", "airtel.in", "gov.in", "nic.in", "github.com", "githubusercontent.com"
    )

    fun isBuiltinTrusted(domain: String): Boolean {
        val cleanHost = domain.lowercase().trim().removePrefix("www.")
        return TRUSTED_INFRASTRUCTURE.any { cleanHost == it || cleanHost.endsWith(".$it") }
    }

    val FEATURE_NAMES = listOf(
        "url_length", "host_length", "path_length", "query_length",
        "num_dots", "num_hyphens", "num_underscores", "num_slashes",
        "num_question_marks", "num_equal_signs", "num_at_symbols", "num_ampersands",
        "num_percent_signs", "num_digits", "digit_ratio", "num_letters",
        "letter_ratio", "num_subdomains", "subdomain_length", "url_entropy",
        "host_entropy", "path_entropy", "has_ip_address", "has_punycode",
        "has_port", "is_https", "suspicious_tld", "suspicious_keywords_count",
        "brand_in_subdomain", "brand_in_path", "brand_mismatch", "consecutive_consonants",
        "vowel_ratio", "vowel_consonant_ratio", "has_hex_encoding", "repeated_char_max",
        "tld_in_subdomain", "path_depth", "query_param_count", "has_dangerous_ext",
        "registered_domain_len"
    )

    /**
     * Primary feature extractor for ShieldNet (41 features).
     */
    fun extract(rawUrl: String): FloatArray {
        val features = FloatArray(41)
        val fullUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            "http://$rawUrl"
        } else {
            rawUrl
        }

        val uri = try {
            Uri.parse(fullUrl)
        } catch (e: Exception) {
            null
        }

        val scheme = uri?.scheme?.lowercase() ?: (if (rawUrl.startsWith("https://")) "https" else "http")
        val host = (uri?.host ?: extractHostFallback(rawUrl)).lowercase()
        val path = uri?.path ?: ""
        val query = uri?.query ?: ""
        val registeredDomain = extractRegisteredDomain(host)

        val hostParts = host.split('.')
        val subdomains = if (hostParts.size > 2) hostParts.dropLast(2).joinToString(".") else ""
        val subdomainCount = if (hostParts.size > 2) hostParts.size - 2 else 0

        // 0-3: Length features
        features[0] = (rawUrl.length.toFloat() / 255f).coerceIn(0f, 1f)
        features[1] = (host.length.toFloat() / 100f).coerceIn(0f, 1f)
        features[2] = (path.length.toFloat() / 100f).coerceIn(0f, 1f)
        features[3] = (query.length.toFloat() / 100f).coerceIn(0f, 1f)

        // 4-13: Symbol & character counts
        features[4] = (rawUrl.count { it == '.' }.toFloat() / 10f).coerceIn(0f, 1f)
        features[5] = (rawUrl.count { it == '-' }.toFloat() / 10f).coerceIn(0f, 1f)
        features[6] = (rawUrl.count { it == '_' }.toFloat() / 5f).coerceIn(0f, 1f)
        features[7] = (rawUrl.count { it == '/' }.toFloat() / 10f).coerceIn(0f, 1f)
        features[8] = (rawUrl.count { it == '?' }.toFloat() / 3f).coerceIn(0f, 1f)
        features[9] = (rawUrl.count { it == '=' }.toFloat() / 5f).coerceIn(0f, 1f)
        features[10] = (rawUrl.count { it == '@' }.toFloat() / 2f).coerceIn(0f, 1f)
        features[11] = (rawUrl.count { it == '&' }.toFloat() / 5f).coerceIn(0f, 1f)
        features[12] = (rawUrl.count { it == '%' }.toFloat() / 5f).coerceIn(0f, 1f)
        features[13] = (rawUrl.count { it.isDigit() }.toFloat() / 20f).coerceIn(0f, 1f)

        // 14-17: Ratios & letter counts
        val lengthNonZero = rawUrl.length.coerceAtLeast(1)
        val digitCount = rawUrl.count { it.isDigit() }
        val letterCount = rawUrl.count { it.isLetter() }
        features[14] = (digitCount.toFloat() / lengthNonZero.toFloat()).coerceIn(0f, 1f)
        features[15] = (letterCount.toFloat() / 50f).coerceIn(0f, 1f)
        features[16] = (letterCount.toFloat() / lengthNonZero.toFloat()).coerceIn(0f, 1f)

        // 18-19: Subdomain features
        features[18] = (subdomainCount.toFloat() / 5f).coerceIn(0f, 1f)
        features[19] = (subdomains.length.toFloat() / 50f).coerceIn(0f, 1f)

        // 20-22: Entropy
        features[20] = (calculateEntropy(rawUrl) / 8f).coerceIn(0f, 1f)
        features[21] = (calculateEntropy(host) / 8f).coerceIn(0f, 1f)
        features[22] = (calculateEntropy(path) / 8f).coerceIn(0f, 1f)

        // 23-26: Flags
        features[23] = if (isIpAddress(host)) 1.0f else 0.0f
        features[24] = if (host.contains("xn--")) 1.0f else 0.0f
        features[25] = if (rawUrl.contains(":[0-9]+".toRegex())) 1.0f else 0.0f
        features[26] = if (scheme == "https") 1.0f else 0.0f

        // 27-28: TLD Risk & Keywords
        features[27] = if (RISKY_TLDS.any { host.endsWith(it) }) 1.0f else 0.0f
        val kwCount = SUSPICIOUS_KEYWORDS.count { rawUrl.lowercase().contains(it) }
        features[28] = (kwCount.toFloat() / 4f).coerceIn(0f, 1f)

        // 29-31: Brand checks (detects brand impersonation like verify-paypal-account.tk)
        val matchedBrand = POPULAR_BRANDS.firstOrNull { host.contains(it) || path.lowercase().contains(it) }
        val isOfficialBrandDomain = matchedBrand != null && (
            host == "$matchedBrand.com" || host.endsWith(".$matchedBrand.com") ||
            host == "$matchedBrand.net" || host.endsWith(".$matchedBrand.net") ||
            host == "$matchedBrand.org" || host.endsWith(".$matchedBrand.org")
        )
        val brandInSub = POPULAR_BRANDS.any { subdomains.contains(it) }
        val brandInPath = POPULAR_BRANDS.any { path.lowercase().contains(it) }
        val brandMismatch = matchedBrand != null && !isOfficialBrandDomain

        features[29] = if (brandInSub) 1.0f else 0.0f
        features[30] = if (brandInPath || (matchedBrand != null && !isOfficialBrandDomain)) 1.0f else 0.0f
        features[31] = if (brandMismatch) 1.0f else 0.0f

        // 32-34: Linguistic patterns
        val vowels = rawUrl.count { it.lowercaseChar() in "aeiou" }
        val consonants = rawUrl.count { it.isLetter() && it.lowercaseChar() !in "aeiou" }
        features[32] = (calculateMaxConsonants(host).toFloat() / 15f).coerceIn(0f, 1f)
        features[33] = (vowels.toFloat() / lengthNonZero.toFloat()).coerceIn(0f, 1f)
        features[34] = (vowels.toFloat() / consonants.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

        // 35-36: Encoding & repetition
        features[35] = if (rawUrl.contains("%[0-9a-fA-F]{2}".toRegex())) 1.0f else 0.0f
        features[36] = (calculateMaxRepeatedChar(rawUrl).toFloat() / 10f).coerceIn(0f, 1f)

        // 37-40: Advanced URL semantics
        features[37] = if (COMMON_TLDS.any { subdomains.contains(it) || path.contains(it) }) 1.0f else 0.0f
        val pathSegments = path.split('/').filter { it.isNotBlank() }
        features[38] = (pathSegments.size.toFloat() / 10f).coerceIn(0f, 1f)
        val queryParams = query.split('&').filter { it.isNotBlank() }
        features[39] = (queryParams.size.toFloat() / 10f).coerceIn(0f, 1f)
        features[40] = if (DANGEROUS_EXTENSIONS.any { rawUrl.lowercase().endsWith(it) || path.lowercase().endsWith(it) }) 1.0f else (registeredDomain.length.toFloat() / 50f).coerceIn(0f, 1f)

        return features
    }

    /**
     * 65-feature extractor for legacy / CICIDS2017 Model 1 support.
     */
    fun extract65(domain: String, dstPort: Int, isTcp: Boolean): FloatArray {
        val features65 = FloatArray(65)
        val f41 = extract(domain)
        for (i in 0 until 41) {
            features65[i] = f41[i]
        }
        features65[41] = (dstPort.toFloat() / 65535f).coerceIn(0f, 1f)
        features65[42] = if (isTcp) 1.0f else 0.0f
        val cal = Calendar.getInstance()
        features65[43] = cal.get(Calendar.HOUR_OF_DAY).toFloat() / 23f
        features65[44] = if (cal.get(Calendar.DAY_OF_WEEK) in Calendar.MONDAY..Calendar.FRIDAY) 1.0f else 0.0f
        return features65
    }

    /**
     * Compatibility bridge for existing calls.
     */
    fun extract(domain: String, dstPort: Int, isTcp: Boolean): FloatArray {
        return extract65(domain, dstPort, isTcp)
    }

    private fun extractHostFallback(url: String): String {
        var clean = url.substringAfter("://").substringBefore('/').substringBefore('?')
        clean = clean.substringBefore(':')
        return clean
    }

    private fun extractRegisteredDomain(host: String): String {
        val parts = host.split('.')
        return if (parts.size >= 2) {
            "${parts[parts.size - 2]}.${parts.last()}"
        } else {
            host
        }
    }

    private fun isIpAddress(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size == 4) {
            return parts.all { it.toIntOrNull()?.let { num -> num in 0..255 } == true }
        }
        return false
    }

    private fun calculateEntropy(s: String): Float {
        if (s.isEmpty()) return 0f
        val freqs = s.groupingBy { it }.eachCount()
        var entropy = 0.0
        for (count in freqs.values) {
            val p = count.toDouble() / s.length
            entropy -= p * (ln(p) / ln(2.0))
        }
        return entropy.toFloat()
    }

    private fun calculateMaxConsonants(s: String): Int {
        var maxCount = 0
        var currentCount = 0
        val vowels = "aeiou"
        for (c in s.lowercase()) {
            if (c.isLetter() && c !in vowels) {
                currentCount++
                maxCount = maxOf(maxCount, currentCount)
            } else {
                currentCount = 0
            }
        }
        return maxCount
    }

    private fun calculateMaxRepeatedChar(s: String): Int {
        if (s.isEmpty()) return 0
        var maxCount = 1
        var currentCount = 1
        for (i in 1 until s.length) {
            if (s[i] == s[i - 1]) {
                currentCount++
                maxCount = maxOf(maxCount, currentCount)
            } else {
                currentCount = 1
            }
        }
        return maxCount
    }
}
