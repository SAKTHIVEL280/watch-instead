package com.watchinstead.app.feature.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.watchinstead.app.data.backup.BackupRepository
import com.watchinstead.app.data.backup.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class BackupViewModel @Inject constructor(private val backups: BackupRepository) : ViewModel() {
    suspend fun export(resolver: ContentResolver, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val text = backups.export()
        try {
            resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } != null
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    suspend fun import(resolver: ContentResolver, uri: Uri): ImportResult? = withContext(Dispatchers.IO) {
        val text = try {
            resolver.openInputStream(uri)?.use { it.readNBytesCompat(MAX_BACKUP_BYTES) }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
        text?.let { backups.import(it.decodeToString()) }
    }

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray? {
        val bytes = readBytesUpTo(limit + 1)
        return bytes.takeIf { it.size <= limit }
    }

    private fun java.io.InputStream.readBytesUpTo(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (output.size() < limit) {
            val read = read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private companion object {
        const val MAX_BACKUP_BYTES = 5 * 1024 * 1024
    }
}
