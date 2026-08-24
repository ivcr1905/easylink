package com.iv.easylink.utils

import java.net.IDN
import java.net.URI
import java.util.Locale

object SecurityScanner {
    private val ipv4Pattern = Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")
    private val suspiciousTlds = setOf("tk", "ml", "cf", "gq", "ga", "top")
    private val suspiciousKeywords = arrayOf("login", "secure", "billing", "verify", "account", "support", "auth")
    private val protectedBrands = arrayOf("netflix", "amazon", "paypal", "microsoft", "apple", "google")

    fun isSuspiciousUrl(url: String): Boolean {
        return try {
            val host = URI(url).host?.lowercase(Locale.ROOT) ?: return true
            if (host == "localhost" || ipv4Pattern.matches(host) || host.contains(':')) return true
            if (host.substringAfterLast('.') in suspiciousTlds) return true
            if (host.startsWith("xn--") || host.contains(".xn--")) {
                val unicode = IDN.toUnicode(host).lowercase(Locale.ROOT)
                if (protectedBrands.any(unicode::contains)) return true
            }
            if ('-' in host && suspiciousKeywords.any(host::contains)) return true
            val labels = host.split('.')
            if (labels.size > 2) {
                val registrableApproximation = labels.takeLast(2).joinToString(".")
                val subdomain = labels.dropLast(2).joinToString(".")
                if (protectedBrands.any { it in subdomain && !registrableApproximation.startsWith("$it.") }) return true
            }
            false
        } catch (_: Exception) {
            true
        }
    }
}
