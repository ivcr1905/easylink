package com.iv.easylink.data.metadata

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Html
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.util.Locale

data class LinkMetadata(val title: String?, val siteName: String?, val imageUrl: String?)

class MetadataClient {
    private val permits = Semaphore(2)

    suspend fun fetch(url: String): LinkMetadata? = permits.withPermit {
        withContext(Dispatchers.IO) { fetchBlocking(url) }
    }

    private fun fetchBlocking(initialUrl: String): LinkMetadata? {
        var current = URI(initialUrl)
        repeat(MAX_REDIRECTS + 1) {
            if (!NetworkSafety.isPublicWebUri(current)) return null
            val connection = (current.toURL().openConnection() as? HttpURLConnection) ?: return null
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.setRequestProperty("User-Agent", "EasyLink/1.0 (+https://example.com/easylink)")
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")
                when (connection.responseCode) {
                    in 300..399 -> {
                        val location = connection.getHeaderField("Location") ?: return null
                        current = current.resolve(location)
                        return@repeat
                    }
                    !in 200..299 -> return null
                }
                val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
                if (!contentType.startsWith("text/html") && !contentType.startsWith("application/xhtml")) return null
                val declaredLength = connection.contentLengthLong
                if (declaredLength > MAX_HTML_BYTES) return null
                val bytes = connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream(minOf(MAX_HTML_BYTES, declaredLength.coerceAtLeast(4096).toInt()))
                    val buffer = ByteArray(8_192)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_HTML_BYTES) return null
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                }
                val charset = contentType.substringAfter("charset=", "UTF-8").substringBefore(';').trim()
                val html = runCatching { bytes.toString(Charsets.UTF_8.takeIf { charset.equals("UTF-8", true) } ?: charset(charset)) }
                    .getOrElse { bytes.toString(Charsets.UTF_8) }
                return parse(html, current)
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private fun parse(html: String, base: URI): LinkMetadata? {
        var title: String? = null
        var siteName: String? = null
        var imageUrl: String? = null
        META_TAG.findAll(html).take(80).forEach { match ->
            val attributes = HashMap<String, String>(4)
            ATTRIBUTE.findAll(match.value).forEach { attribute ->
                attributes[attribute.groupValues[1].lowercase(Locale.ROOT)] = attribute.groupValues[3]
            }
            val property = (attributes["property"] ?: attributes["name"])?.lowercase(Locale.ROOT)
            val content = attributes["content"]?.decode()?.trim()?.take(MAX_TEXT_LENGTH)
            when (property) {
                "og:title", "twitter:title" -> if (title.isNullOrBlank()) title = content
                "og:site_name", "application-name" -> if (siteName.isNullOrBlank()) siteName = content
                "og:image", "twitter:image", "twitter:image:src" -> if (imageUrl.isNullOrBlank()) {
                    imageUrl = content?.let { runCatching { base.resolve(it).toString() }.getOrNull() }
                }
            }
        }
        if (title.isNullOrBlank()) title = TITLE.find(html)?.groupValues?.getOrNull(1)?.decode()?.trim()?.take(MAX_TEXT_LENGTH)
        if (title.isNullOrBlank() && siteName.isNullOrBlank() && imageUrl.isNullOrBlank()) return null
        return LinkMetadata(title?.ifBlank { null }, siteName?.ifBlank { null }, imageUrl?.takeIf { NetworkSafety.isPublicWebUri(runCatching { URI(it) }.getOrNull()) })
    }

    private fun String.decode(): String = Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()

    private companion object {
        const val CONNECT_TIMEOUT_MS = 4_000
        const val READ_TIMEOUT_MS = 5_000
        const val MAX_HTML_BYTES = 192 * 1024
        const val MAX_REDIRECTS = 3
        const val MAX_TEXT_LENGTH = 240
        val META_TAG = Regex("<meta\\s+[^>]*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val ATTRIBUTE = Regex("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(['\"])(.*?)\\2", setOf(RegexOption.DOT_MATCHES_ALL))
        val TITLE = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    }
}

class PreviewImageLoader {
    private val permits = Semaphore(2)
    private val failed = LinkedHashSet<String>()
    private val cache = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    suspend fun load(url: String): Bitmap? {
        synchronized(cache) {
            cache.get(url)?.let { return it }
            if (url in failed) return null
        }
        return permits.withPermit {
        withContext(Dispatchers.IO) {
            synchronized(cache) { cache.get(url) } ?: download(url).also { bitmap ->
                synchronized(cache) {
                    if (bitmap != null) {
                        cache.put(url, bitmap)
                    } else {
                        failed.add(url)
                        while (failed.size > MAX_FAILED_URLS) failed.remove(failed.first())
                    }
                }
            }
        }
        }
    }

    private fun download(value: String): Bitmap? {
        var current = runCatching { URI(value) }.getOrNull() ?: return null
        repeat(MAX_IMAGE_REDIRECTS + 1) {
            if (current.scheme != "https" || !NetworkSafety.isPublicWebUri(current)) return null
            val connection = (current.toURL().openConnection() as? HttpURLConnection) ?: return null
            try {
                connection.connectTimeout = 4_000
                connection.readTimeout = 5_000
                connection.instanceFollowRedirects = false
                when (connection.responseCode) {
                    in 300..399 -> {
                        current = current.resolve(connection.getHeaderField("Location") ?: return null)
                        return@repeat
                    }
                    !in 200..299 -> return null
                }
                if (!connection.contentType.orEmpty().lowercase(Locale.ROOT).startsWith("image/")) return null
                if (connection.contentLengthLong > MAX_IMAGE_BYTES) return null
                val bytes = connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8_192)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_IMAGE_BYTES) return null
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            } catch (_: Exception) {
                return null
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 1_500_000
        const val MAX_IMAGE_REDIRECTS = 3
        const val MAX_FAILED_URLS = 64
    }
}

private object NetworkSafety {
    fun isPublicWebUri(uri: URI?): Boolean {
        if (uri == null || (uri.scheme != "https" && uri.scheme != "http")) return false
        val host = uri.host ?: return false
        return runCatching {
            InetAddress.getAllByName(host).all { address ->
                !address.isAnyLocalAddress && !address.isLoopbackAddress && !address.isLinkLocalAddress &&
                    !address.isSiteLocalAddress && !address.isMulticastAddress
            }
        }.getOrDefault(false)
    }
}
