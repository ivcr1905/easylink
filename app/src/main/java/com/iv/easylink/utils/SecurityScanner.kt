package com.iv.easylink.utils

import androidx.core.net.toUri
import java.util.Locale.getDefault

object SecurityScanner {
    fun isSuspiciousUrl(url: String): Boolean {
        return try {
            val uri = url.toUri()
            val host = uri.host?.lowercase(getDefault()) ?: return false
            val ipRegex = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$".toRegex()
            if (host.matches(ipRegex)) return true
            val suspiciousList = listOf(".tk", ".ml", ".cf", ".gq", ".xyz",".ga",".top")
            if (suspiciousList.any { host.endsWith(it) }) return true
            val suspiciousKeywordsList = listOf("login", "secure", "billing", "verify", "account","support","auth")
            if (host.contains("-") && suspiciousKeywordsList.any { host.contains(it) }) return true
            val trustedBrands = listOf("netflix", "amazon", "paypal", "microsoft","apple","google","bank")
            val parts = host.split(".")
            if (parts.size > 2) {
                val subdomain = parts.dropLast(2).joinToString(".")
                if (trustedBrands.any { (subdomain.contains(it)) }) return true
            }
            return false
        } catch (e: Exception) {
            false
        }
    }
}