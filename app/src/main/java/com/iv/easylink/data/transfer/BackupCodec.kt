package com.iv.easylink.data.transfer

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.iv.easylink.model.LinkCategory
import com.iv.easylink.utils.LinkExtractor
import java.io.Reader
import java.io.Writer
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.FilterInputStream

object BackupCodec {
    const val MIME_TYPE = "application/json"
    const val FILE_SUFFIX = ".easylink.json"
    const val MAX_FILE_BYTES = 8L * 1024 * 1024
    const val MAX_LINKS = 10_000
    private const val MAX_COLLECTIONS = 1_000
    private const val MAX_RULES = 1_000
    private const val MAX_COLLECTIONS_PER_LINK = 1_000
    private const val FORMAT = "easylink"
    private const val VERSION = 1

    fun write(backup: EasyLinkBackup, output: OutputStream) =
        write(backup, OutputStreamWriter(output, Charsets.UTF_8))

    fun read(input: InputStream): EasyLinkBackup =
        read(InputStreamReader(SizeLimitedInputStream(input, MAX_FILE_BYTES), Charsets.UTF_8))

    fun write(backup: EasyLinkBackup, output: Writer) {
        JsonWriter(output).use { writer ->
            writer.setIndent("  ")
            writer.beginObject()
            writer.name("format").value(FORMAT)
            writer.name("version").value(VERSION)
            writer.name("kind").value(backup.kind.wireName)
            writer.name("exportedAt").value(backup.exportedAt)
            backup.name?.let { writer.name("name").value(it) }
            writer.name("collections").beginArray()
            backup.collections.forEach { collection ->
                writer.beginObject()
                writer.name("ref").value(collection.ref)
                writer.name("name").value(collection.name)
                writer.name("colorToken").value(collection.colorToken)
                writer.name("createdAt").value(collection.createdAt)
                writer.name("pinned").value(collection.isPinned)
                writer.endObject()
            }
            writer.endArray()
            writer.name("links").beginArray()
            backup.links.forEach { link -> writeLink(writer, link) }
            writer.endArray()
            writer.name("rules").beginArray()
            backup.rules.forEach { rule ->
                writer.beginObject()
                writer.name("name").value(rule.name)
                writer.name("hostPattern").value(rule.hostPattern)
                nullable(writer, "pathContains", rule.pathContains)
                rule.category?.let { writer.name("category").value(it) }
                nullable(writer, "collectionRef", rule.collectionRef)
                writer.name("enabled").value(rule.enabled)
                writer.name("priority").value(rule.priority)
                writer.endObject()
            }
            writer.endArray()
            writer.endObject()
        }
    }

    fun read(input: Reader): EasyLinkBackup {
        var format: String? = null
        var version = -1
        var kind: BackupKind? = null
        var exportedAt = 0L
        var name: String? = null
        val collections = ArrayList<BackupCollection>()
        val links = ArrayList<BackupLink>()
        val rules = ArrayList<BackupRule>()
        JsonReader(input).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "format" -> format = reader.safeString(40)
                    "version" -> version = reader.nextInt()
                    "kind" -> kind = BackupKind.fromWireName(reader.safeString(24))
                    "exportedAt" -> exportedAt = reader.nextLong()
                    "name" -> name = reader.nullableString(80)
                    "collections" -> readCollections(reader, collections)
                    "links" -> readLinks(reader, links)
                    "rules" -> readRules(reader, rules)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }
        if (format != FORMAT || version != VERSION || kind == null) throw InvalidBackupException("This is not a supported EasyLink file")
        if (links.size > MAX_LINKS) throw InvalidBackupException("This file contains too many links")
        val collectionRefs = collections.map { it.ref }
        val collectionRefSet = collectionRefs.toHashSet()
        if (collectionRefSet.size != collectionRefs.size) throw InvalidBackupException("Collection references are duplicated")
        val linkRefs = links.map { it.ref }
        if (linkRefs.toSet().size != linkRefs.size) throw InvalidBackupException("Link references are duplicated")
        if (links.any { link -> link.collectionRefs.any { it !in collectionRefSet } } || rules.any { it.collectionRef != null && it.collectionRef !in collectionRefSet }) {
            throw InvalidBackupException("The file contains a broken collection reference")
        }
        return EasyLinkBackup(kind, exportedAt.coerceAtLeast(0), name, collections, links, rules)
    }

    private fun writeLink(writer: JsonWriter, link: BackupLink) {
        writer.beginObject()
        writer.name("ref").value(link.ref)
        writer.name("url").value(link.url)
        nullable(writer, "originalUrl", link.originalUrl)
        nullable(writer, "sourcePackage", link.sourcePackage)
        writer.name("sourceApp").value(link.sourceApp)
        nullable(writer, "senderName", link.senderName)
        nullable(writer, "conversationName", link.conversationName)
        writer.name("timestamp").value(link.timestamp)
        writer.name("category").value(link.category)
        writer.name("suspicious").value(link.isSuspicious)
        writer.name("favorite").value(link.isFavorite)
        writer.name("readLater").value(link.isReadLater)
        writer.name("cleaned").value(link.wasCleaned)
        nullable(writer, "title", link.title)
        nullable(writer, "siteName", link.siteName)
        link.archivedAt?.let { writer.name("archivedAt").value(it) }
        link.deletedAt?.let { writer.name("deletedAt").value(it) }
        writer.name("collections").beginArray()
        link.collectionRefs.forEach(writer::value)
        writer.endArray()
        writer.endObject()
    }

    private fun readCollections(reader: JsonReader, output: MutableList<BackupCollection>) {
        reader.beginArray()
        while (reader.hasNext()) {
            if (output.size >= MAX_COLLECTIONS) throw InvalidBackupException("Too many collections")
            var ref = ""
            var name = ""
            var color = 0
            var createdAt = 0L
            var pinned = false
            reader.beginObject()
            while (reader.hasNext()) when (reader.nextName()) {
                "ref" -> ref = reader.safeString(80)
                "name" -> name = reader.safeString(40)
                "colorToken" -> color = reader.nextInt().coerceIn(0, 7)
                "createdAt" -> createdAt = reader.nextLong()
                "pinned" -> pinned = reader.nextBoolean()
                else -> reader.skipValue()
            }
            reader.endObject()
            if (ref.isBlank() || name.isBlank()) throw InvalidBackupException("Collection data is incomplete")
            output.add(BackupCollection(ref, name, color, createdAt.coerceAtLeast(0), pinned))
        }
        reader.endArray()
    }

    private fun readLinks(reader: JsonReader, output: MutableList<BackupLink>) {
        reader.beginArray()
        while (reader.hasNext()) {
            if (output.size >= MAX_LINKS) throw InvalidBackupException("This file contains too many links")
            output.add(readLink(reader))
        }
        reader.endArray()
    }

    private fun readLink(reader: JsonReader): BackupLink {
        var ref = ""
        var url = ""
        var originalUrl: String? = null
        var sourcePackage: String? = null
        var sourceApp = "Imported"
        var senderName: String? = null
        var conversationName: String? = null
        var timestamp = 0L
        var category = 0
        var suspicious = false
        var favorite = false
        var readLater = false
        var cleaned = false
        var title: String? = null
        var siteName: String? = null
        var archivedAt: Long? = null
        var deletedAt: Long? = null
        val collectionRefs = ArrayList<String>(2)
        reader.beginObject()
        while (reader.hasNext()) when (reader.nextName()) {
            "ref" -> ref = reader.safeString(80)
            "url" -> url = reader.safeString(4_096)
            "originalUrl" -> originalUrl = reader.nullableString(4_096)
            "sourcePackage" -> sourcePackage = reader.nullableString(160)
            "sourceApp" -> sourceApp = reader.safeString(80)
            "senderName" -> senderName = reader.nullableString(80)
            "conversationName" -> conversationName = reader.nullableString(80)
            "timestamp" -> timestamp = reader.nextLong()
            "category" -> category = reader.nextInt()
            "suspicious" -> suspicious = reader.nextBoolean()
            "favorite" -> favorite = reader.nextBoolean()
            "readLater" -> readLater = reader.nextBoolean()
            "cleaned" -> cleaned = reader.nextBoolean()
            "title" -> title = reader.nullableString(240)
            "siteName" -> siteName = reader.nullableString(240)
            "archivedAt" -> archivedAt = reader.nullableLong()
            "deletedAt" -> deletedAt = reader.nullableLong()
            "collections" -> {
                reader.beginArray()
                while (reader.hasNext()) {
                    if (collectionRefs.size >= MAX_COLLECTIONS_PER_LINK) throw InvalidBackupException("A link belongs to too many collections")
                    collectionRefs.add(reader.safeString(80))
                }
                reader.endArray()
            }
            else -> reader.skipValue()
        }
        reader.endObject()
        val processed = LinkExtractor.process(url) ?: throw InvalidBackupException("A link is invalid")
        if (ref.isBlank()) throw InvalidBackupException("A link reference is missing")
        val safeOriginal = originalUrl?.let(LinkExtractor::process)?.let { it.originalUrl ?: it.url }
            ?.takeIf { it != processed.url }
        return BackupLink(
            ref = ref,
            url = processed.url,
            sourcePackage = sourcePackage,
            sourceApp = sourceApp.ifBlank { "Imported" },
            senderName = senderName,
            conversationName = conversationName,
            timestamp = timestamp.coerceAtLeast(0),
            category = LinkCategory.fromCode(category).code,
            isSuspicious = suspicious,
            isFavorite = favorite,
            wasCleaned = cleaned || processed.wasCleaned || safeOriginal != null,
            title = title,
            siteName = siteName,
            archivedAt = archivedAt?.coerceAtLeast(0),
            deletedAt = deletedAt?.coerceAtLeast(0),
            collectionRefs = collectionRefs.distinct(),
            originalUrl = safeOriginal ?: processed.originalUrl,
            isReadLater = readLater,
        )
    }

    private fun readRules(reader: JsonReader, output: MutableList<BackupRule>) {
        reader.beginArray()
        while (reader.hasNext()) {
            if (output.size >= MAX_RULES) throw InvalidBackupException("Too many automation rules")
            var name = ""
            var host = ""
            var path: String? = null
            var category: Int? = null
            var collectionRef: String? = null
            var enabled = true
            var priority = 0
            reader.beginObject()
            while (reader.hasNext()) when (reader.nextName()) {
                "name" -> name = reader.safeString(50)
                "hostPattern" -> host = reader.safeString(253)
                "pathContains" -> path = reader.nullableString(240)
                "category" -> category = LinkCategory.fromCode(reader.nextInt()).code
                "collectionRef" -> collectionRef = reader.nullableString(80)
                "enabled" -> enabled = reader.nextBoolean()
                "priority" -> priority = reader.nextInt().coerceAtLeast(0)
                else -> reader.skipValue()
            }
            reader.endObject()
            if (name.isNotBlank() && host.isNotBlank() && (category != null || collectionRef != null)) {
                output.add(BackupRule(name, host, path, category, collectionRef, enabled, priority))
            }
        }
        reader.endArray()
    }

    private fun nullable(writer: JsonWriter, name: String, value: String?) {
        writer.name(name)
        if (value == null) writer.nullValue() else writer.value(value)
    }

    private fun JsonReader.safeString(maxLength: Int): String {
        if (peek() != JsonToken.STRING) throw InvalidBackupException("Expected text")
        val value = nextString()
        if (value.length > maxLength) throw InvalidBackupException("A text value is too long")
        return value
    }

    private fun JsonReader.nullableString(maxLength: Int): String? = if (peek() == JsonToken.NULL) {
        nextNull(); null
    } else safeString(maxLength).ifBlank { null }

    private fun JsonReader.nullableLong(): Long? = if (peek() == JsonToken.NULL) {
        nextNull(); null
    } else nextLong()
}

private class SizeLimitedInputStream(input: InputStream, private val limit: Long) : FilterInputStream(input) {
    private var total = 0L

    override fun read(): Int = super.read().also { if (it >= 0) account(1) }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        super.read(buffer, offset, length).also { if (it > 0) account(it.toLong()) }

    private fun account(count: Long) {
        total += count
        if (total > limit) throw InvalidBackupException("This file is larger than 8 MB")
    }
}
