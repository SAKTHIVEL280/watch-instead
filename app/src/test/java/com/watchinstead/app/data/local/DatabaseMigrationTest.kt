package com.lockin.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Builds a real version 1 database from the exported schema, so an upgrade is tested as users get it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class DatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val schemaFile = File("schemas/com.lockin.app.data.local.LockinDatabase/1.json")
        .takeIf { it.exists() } ?: File("app/schemas/com.lockin.app.data.local.LockinDatabase/1.json")

    private fun createVersion1(file: File) {
        val database = Json.parseToJsonElement(schemaFile.readText()).jsonObject["database"]!!.jsonObject
        val sql = database["entities"]!!.jsonArray.flatMap { entity ->
            val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
            val create = entity.jsonObject["createSql"]!!.jsonPrimitive.content
            val indices = entity.jsonObject["indices"]!!.jsonArray.map { it.jsonObject["createSql"]!!.jsonPrimitive.content }
            (listOf(create) + indices).map { it.replace("\${TABLE_NAME}", table) }
        }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            sql.forEach(db::execSQL)
            db.execSQL(
                "INSERT INTO saved_links (id, url, platform, category, note, createdAt) " +
                    "VALUES (7, 'https://example.com/a', 'OTHER', 'MOTIVATION', 'keep me', 123)",
            )
            db.version = 1
        }
    }

    @Test fun `upgrading from version 1 keeps saved links and adds working stats`() = runTest {
        val file = context.getDatabasePath("migration-test.db").apply { parentFile?.mkdirs() }
        createVersion1(file)

        val room = Room.databaseBuilder(context, LockinDatabase::class.java, file.absolutePath)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val link = room.savedLinkDao().observeAll().first().single()
        assertEquals(7L, link.id)
        assertEquals("keep me", link.note)

        val event = PromptEventEntity(
            timestamp = 1, packageName = "a", outcome = com.lockin.app.domain.PromptOutcome.WATCHED, reason = null,
        )
        room.promptEventDao().insert(event)
        assertEquals(-1L, room.promptEventDao().insert(event))
        room.close()
    }
}
