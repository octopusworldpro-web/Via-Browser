package com.octopusworldpro.omega.browser

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

internal data class PreparedExtension(
    val stagingDirectory: File,
    val destinationDirectory: File,
    val name: String,
    val version: String,
    val manifestVersion: Int,
    val requestedPermissions: List<String>,
    val hostPatterns: List<String>,
    val previousVersion: String?,
    val addedPermissions: List<String>
) {
    val isUpdate: Boolean get() = previousVersion != null
}

internal object ExtensionInstaller {
    private const val MAX_FILES = 2000
    private const val MAX_TOTAL_BYTES = 20L * 1024L * 1024L
    private const val MAX_FILE_BYTES = 8L * 1024L * 1024L
    private const val MAX_MANIFEST_BYTES = 256L * 1024L

    fun prepare(input: InputStream, extensionsDirectory: File): PreparedExtension {
        require(extensionsDirectory.exists() || extensionsDirectory.mkdirs()) {
            "Cannot create extensions directory"
        }
        val staging = File(extensionsDirectory, "install-${System.nanoTime()}")
        require(staging.mkdirs()) { "Cannot create temporary install directory" }
        try {
            extract(input, staging)
            flattenSingleRoot(staging)
            val manifestFile = File(staging, "manifest.json")
            require(manifestFile.isFile) { "manifest.json not found in ZIP" }
            require(manifestFile.length() in 1..MAX_MANIFEST_BYTES) { "manifest.json is empty or exceeds 256 KB" }

            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            val manifestVersion = manifest.optInt("manifest_version", 0)
            require(manifestVersion == 2 || manifestVersion == 3) { "Only Manifest V2/V3 packages are supported" }

            val name = manifest.optString("name").trim()
            val version = manifest.optString("version").trim()
            require(name.isNotBlank() && name.length <= 128) { "Manifest name is missing or too long" }
            require(version.matches(Regex("[0-9]+(\\.[0-9]+){0,3}"))) { "Manifest version is invalid" }

            val permissions = readStringArray(manifest.optJSONArray("permissions"))
            val optionalPermissions = readStringArray(manifest.optJSONArray("optional_permissions"))
            val hostPermissions = mutableListOf<String>()
            permissions.filter { it == "<all_urls>" || it.contains("://") }.forEach { hostPermissions += it }
            hostPermissions += readStringArray(manifest.optJSONArray("host_permissions"))
            val allPermissions = (permissions + optionalPermissions).distinct()

            validateReferencedFiles(staging, manifest, manifestVersion)
            val id = stableId(name, manifest.optString("key"))
            val destination = File(extensionsDirectory, id)
            val previousManifest = File(destination, "manifest.json")
            val previousVersion = if (destination.isDirectory && previousManifest.isFile) {
                runCatching { JSONObject(previousManifest.readText()).optString("version").ifBlank { "?" } }.getOrDefault("?")
            } else null
            val oldPermissions = if (previousVersion != null) {
                runCatching {
                    val old = JSONObject(previousManifest.readText())
                    (readStringArray(old.optJSONArray("permissions")) +
                        readStringArray(old.optJSONArray("optional_permissions")) +
                        readStringArray(old.optJSONArray("host_permissions"))).toSet()
                }.getOrDefault(emptySet())
            } else emptySet()

            return PreparedExtension(
                staging, destination, name, version, manifestVersion,
                allPermissions, hostPermissions.distinct(), previousVersion,
                (allPermissions.toSet() - oldPermissions).sorted()
            )
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        }
    }

    fun commit(prepared: PreparedExtension) {
        val destination = prepared.destinationDirectory
        val parent = destination.parentFile ?: throw IllegalStateException("Invalid extension destination")
        val backup = File(parent, "backup-${destination.name}-${System.nanoTime()}")
        if (destination.exists() && !destination.renameTo(backup)) {
            throw IllegalStateException("Could not safely stage existing extension")
        }
        if (!prepared.stagingDirectory.renameTo(destination)) {
            if (backup.exists()) backup.renameTo(destination)
            throw IllegalStateException("Install failed; previous extension version was restored")
        }
        if (backup.exists()) backup.deleteRecursively()
    }

    fun discard(prepared: PreparedExtension) {
        prepared.stagingDirectory.deleteRecursively()
    }

    private fun extract(input: InputStream, staging: File) {
        var totalBytes = 0L
        var fileCount = 0
        val seen = mutableSetOf<String>()
        val files = mutableSetOf<String>()
        val directories = mutableSetOf<String>()

        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                fileCount++
                require(fileCount <= MAX_FILES) { "ZIP contains too many entries (max 2000)" }
                val rawName = entry.name
                require(rawName.isNotBlank() && !rawName.startsWith("/") && !rawName.startsWith("\\\\")) {
                    "ZIP contains an unsafe path"
                }
                require(!rawName.contains('\\')) { "ZIP contains an invalid path separator" }
                val normalized = rawName.removeSuffix("/")
                val segments = normalized.split('/')
                require(segments.none { it.isBlank() || it == "." || it == ".." } && !normalized.contains(':')) {
                    "ZIP contains an unsafe path"
                }
                val key = normalized.lowercase()
                require(seen.add(key)) { "ZIP contains duplicate paths" }
                val target = File(staging, normalized)
                require(target.canonicalPath.startsWith(staging.canonicalPath + File.separator)) {
                    "ZIP path escapes installation directory"
                }

                // ZIP entries may arrive in any order. Reject file/directory collisions
                // against both previously seen entries and implicit parent directories.
                val ancestors = segments.dropLast(1).indices.map { index ->
                    segments.take(index + 1).joinToString("/").lowercase()
                }
                if (entry.isDirectory) {
                    require(key !in files && ancestors.none { it in files }) {
                        "ZIP contains conflicting file paths"
                    }
                    directories += key
                    require(target.mkdirs() || target.isDirectory) { "Could not create ZIP directory" }
                } else {
                    require(key !in directories && ancestors.none { it in files } &&
                        files.none { it.startsWith("$key/") }) {
                        "ZIP contains conflicting file paths"
                    }
                    files += key
                    target.parentFile?.let { require(it.mkdirs() || it.isDirectory) { "Could not create ZIP directory" } }
                    var fileBytes = 0L
                    target.outputStream().buffered().use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            fileBytes += count
                            totalBytes += count
                            require(fileBytes <= MAX_FILE_BYTES) { "A ZIP file exceeds 8 MB" }
                            require(totalBytes <= MAX_TOTAL_BYTES) { "Extension ZIP exceeds 20 MB unpacked" }
                            output.write(buffer, 0, count)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        require(fileCount > 0) { "ZIP is empty" }
    }

    private fun flattenSingleRoot(staging: File) {
        if (File(staging, "manifest.json").isFile) return
        val children = staging.listFiles()?.toList().orEmpty()
        val nested = children.singleOrNull { it.isDirectory } ?: return
        require(children.size == 1 && File(nested, "manifest.json").isFile) {
            "manifest.json must be at ZIP root or inside one top-level folder"
        }
        nested.listFiles()?.forEach { child ->
            val target = File(staging, child.name)
            require(!target.exists() && child.renameTo(target)) { "Could not normalize ZIP folder layout" }
        }
        require(nested.delete()) { "Could not finish ZIP folder normalization" }
    }

    private fun validateReferencedFiles(root: File, manifest: JSONObject, manifestVersion: Int) {
        fun requireFile(path: String, label: String) {
            require(path.isNotBlank() && !path.startsWith("/") && !path.contains('\\') &&
                path.split('/').none { it == ".." || it.isBlank() }) { "$label has an unsafe path" }
            val file = File(root, path)
            require(file.canonicalPath.startsWith(root.canonicalPath + File.separator) && file.isFile) {
                "$label file not found: $path"
            }
        }

        val action = manifest.optJSONObject("action") ?: manifest.optJSONObject("browser_action")
        action?.optString("default_popup")?.takeIf { it.isNotBlank() }?.let { requireFile(it, "Popup") }
        manifest.optString("options_page").takeIf { it.isNotBlank() }?.let { requireFile(it, "Options page") }
        manifest.optJSONObject("options_ui")?.optString("page")?.takeIf { it.isNotBlank() }?.let { requireFile(it, "Options page") }

        val background = manifest.optJSONObject("background")
        background?.optString("page")?.takeIf { it.isNotBlank() }?.let { requireFile(it, "Background page") }
        readStringArray(background?.optJSONArray("scripts")).forEach { requireFile(it, "Background script") }
        background?.optString("service_worker")?.takeIf { it.isNotBlank() }?.let { requireFile(it, "Service worker") }

        val contentScripts = manifest.optJSONArray("content_scripts")
        if (contentScripts != null) for (i in 0 until contentScripts.length()) {
            val item = contentScripts.optJSONObject(i) ?: throw IllegalArgumentException("Invalid content_scripts entry")
            val matches = readStringArray(item.optJSONArray("matches"))
            require(matches.isNotEmpty()) { "Content script entry has no match patterns" }
            (readStringArray(item.optJSONArray("js")) + readStringArray(item.optJSONArray("css"))).forEach {
                requireFile(it, "Content script resource")
            }
        }

        val iconObjects = listOfNotNull(
            manifest.optJSONObject("icons"),
            action?.optJSONObject("default_icon")
        )
        iconObjects.forEach { icons ->
            val keys = icons.keys()
            while (keys.hasNext()) requireFile(icons.getString(keys.next()), "Icon")
        }
        if (manifestVersion == 3) {
            val resources = manifest.optJSONArray("web_accessible_resources")
            if (resources != null) for (i in 0 until resources.length()) {
                val item = resources.optJSONObject(i) ?: continue
                readStringArray(item.optJSONArray("resources")).forEach { resource ->
                    require(resource.isNotBlank() && !resource.startsWith("/") && !resource.split('/').contains("..")) {
                        "Web accessible resource path is unsafe"
                    }
                }
            }
        } else {
            readStringArray(manifest.optJSONArray("web_accessible_resources")).forEach { resource ->
                require(resource.isNotBlank() && !resource.startsWith("/") && !resource.split('/').contains("..")) {
                    "Web accessible resource path is unsafe"
                }
            }
        }
    }

    private fun readStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.optString(it).trim() }.filter { it.isNotBlank() }
    }

    private fun stableId(name: String, key: String): String {
        val normalized = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(36).ifBlank { "extension" }
        val seed = if (key.isNotBlank()) key else name.lowercase()
        if (key.isBlank()) return normalized
        val suffix = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray()).take(5)
            .joinToString("") { "%02x".format(it) }
        return "$normalized-$suffix"
    }
}
