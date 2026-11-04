package com.lockin.app.data

import com.lockin.app.data.backup.BackupCodec
import com.lockin.app.data.backup.BackupFile
import com.lockin.app.data.backup.BackupLink
import com.lockin.app.data.backup.BackupRepository
import com.lockin.app.data.backup.BackupSettings
import com.lockin.app.domain.Accent
import com.lockin.app.domain.Category
import com.lockin.app.domain.ParsedLink
import com.lockin.app.domain.Platform
import com.lockin.app.testing.FakeLinkRepository
import com.lockin.app.testing.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupRepositoryTest {
    private val links = FakeLinkRepository()
    private val settings = FakeSettingsRepository()
    private val backups = BackupRepository(links, settings)

    @Test fun `export then import restores videos and settings on a fresh phone`() = runTest {
        links.save(ParsedLink("https://example.com/a", Platform.OTHER), Category.CONSISTENCY, "note")
        settings.setGuardedPackages(setOf("insta"))
        settings.setCooldownMinutes(0)
        settings.setAccent(Accent.TEAL)
        val text = backups.export()

        val freshLinks = FakeLinkRepository()
        val freshSettings = FakeSettingsRepository()
        val result = BackupRepository(freshLinks, freshSettings).import(text)!!

        assertEquals(1, result.added)
        assertEquals(Category.CONSISTENCY, freshLinks.links.value.single().category)
        assertEquals("note", freshLinks.links.value.single().note)
        assertEquals(setOf("insta"), freshSettings.settings.value.guardedPackages)
        assertEquals(0, freshSettings.settings.value.cooldownMinutes)
        assertEquals(Accent.TEAL, freshSettings.appearance.value.accent)
    }

    @Test fun `import keeps existing videos and skips duplicates`() = runTest {
        links.save(ParsedLink("https://example.com/a", Platform.OTHER), Category.OTHER, "")
        val text = backups.export()
        val result = backups.import(text)!!
        assertEquals(0, result.added)
        assertEquals(1, result.skipped)
        assertEquals(1, links.links.value.size)
    }

    @Test fun `unsafe links and invalid settings in a file are dropped`() = runTest {
        val file = BackupFile(
            links = listOf(
                BackupLink("javascript:alert(1)", "OTHER", createdAt = 1),
                BackupLink("https://example.com/b", "NOT_A_CATEGORY", thumbnailUrl = "http://x/y.jpg", createdAt = 1),
            ),
            settings = BackupSettings(cooldownMinutes = 999, accent = "NOPE"),
        )
        val result = backups.import(BackupCodec.encode(file))!!
        assertEquals(1, result.added)
        assertEquals(1, result.skipped)
        val saved = links.links.value.single()
        assertEquals(Category.OTHER, saved.category)
        assertNull(saved.thumbnailUrl)
        assertEquals(15, settings.settings.value.cooldownMinutes)
    }

    @Test fun `files that are not backups are rejected`() = runTest {
        assertNull(backups.import("not json"))
        assertNull(backups.import("""{"app":"other"}"""))
        assertNull(backups.import("""{"app":"lockin","version":99}"""))
    }
}
