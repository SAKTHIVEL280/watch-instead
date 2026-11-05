package com.watchinstead.app.feature.backup

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.watchinstead.app.R
import java.time.LocalDate
import kotlinx.coroutines.launch

class BackupActions(val export: () -> Unit, val import: () -> Unit)

/** Uses the system file picker, so Watch Instead needs no storage permission and the user picks the location. */
@Composable
fun rememberBackupActions(viewModel: BackupViewModel): BackupActions {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = viewModel.export(context.contentResolver, uri)
                val message = if (saved) R.string.backup_exported else R.string.backup_export_failed
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = viewModel.import(context.contentResolver, uri)
                val message = if (result == null) {
                    resources.getString(R.string.backup_import_failed)
                } else {
                    resources.getString(R.string.backup_imported, result.added, result.skipped)
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    return remember(exporter, importer) {
        BackupActions(
            export = { exporter.launch("watch-instead-backup-${LocalDate.now()}.json") },
            // Cloud providers often report .json files as a generic type.
            import = { importer.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
        )
    }
}
