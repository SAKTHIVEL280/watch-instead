package com.lockin.app.data

import com.lockin.app.data.backup.BackupCodec
import com.lockin.app.data.backup.BackupEvent
import com.lockin.app.data.backup.BackupFile
import com.lockin.app.data.backup.BackupLink
import com.lockin.app.data.backup.BackupRepository
import com.lockin.app.data.backup.BackupSettings
import com.lockin.app.domain.Accent
import com.lockin.app.domain.Category
import com.lockin.app.domain.FocusSchedule
import com.lockin.app.domain.OpenReason
import com.lockin.app.domain.ParsedLink
import com.lockin.app.domain.Platform
import com.lockin.app.domain.PromptOutcome
import com.lockin.app.testing.FakeLinkRepository
import com.lockin.app.testing.FakeSettingsRepository
import com.lockin.app.testing.FakeStatsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupRepositoryTest {
    private val links = FakeLinkRepository()
    private val settings = FakeSettingsRepository()
    private val stats = FakeStatsRepository()
    private val backups = BackupRepository(links, settings, stats)

    @Test fun `export then import restores videos and settings on a fresh phone`() = runTest {
        links.save(ParsedLink("https://example.com/a", Platform.OTHER), Category.CONSISTENCY, "note")
        settings.setGuardedPackages(setOf("insta"))
        settings.setCooldownMinutes(0)
        settings.setAccent(Accent.TEAL)
        val text = backups.export()

        val freshLinks = FakeLinkRepository()
        val freshSettings = FakeSettingsRepository()
        val freshStats = FakeStatsRepository()
        val result = BackupRepository(freshLinks, freshSettings, freshStats).import(text)!!

        assertEquals(1, result.added)
        assertEquals(Category.CONSISTENCY, freshLinks.links.value.single().category)
        assertEquals("note", freshLinks.links.value.single().note)
        assertEquals(setOf("insta"), freshSettings.settings.value.guardedPackages)
        assertEquals(0, freshSettings.settings.value.cooldownMinutes)
        assertEquals(Accent.TEAL, freshSettings.appearance.value.accent)
    }

    @Test fun `stats, messages and schedules survive a backup, and importing twice adds nothing`() = runTest {
        val schedule = FocusSchedule("id", "Study", setOf(1), 540, 720)
        stats.record("insta", PromptOutcome.WENT_HOME, OpenReason.BORED)
        settings.setPersonalMessages(setOf("Exams soon"))
        settings.setFocusSchedules(listOf(schedule))
        val text = backups.export()

        val freshStats = FakeStatsRepository()
        val freshSettings = FakeSettingsRepository()
        val fresh = BackupRepository(FakeLinkRepository(), freshSettings, freshStats)
        repeat(2) { fresh.import(text) }

        assertEquals(OpenReason.BORED, freshStats.events.value.single().reason)
        assertEquals(setOf("Exams soon"), freshSettings.settings.value.personalMessages)
        assertEquals(listOf(schedule), freshSettings.settings.value.focusSchedules)
    }

    @Test fun `version 1 backups from Lockin 1_0 still import`() = runTest {
        val v1 = """{"version":1,"app":"lockin","links":[{"url":"https://example.com/a","category":"OTHER","createdAt":1}]}"""
        assertEquals(1, backups.import(v1)!!.added)
    }

    @Test fun `corrupt schedules, reasons and outcomes in a file are dropped`() = runTest {
        val file = BackupFile(
            settings = BackupSettings(
                focusSchedules = listOf(FocusSchedule("bad", "x", setOf(9), 0, 5000)),
                personalMessages = setOf("   "),
            ),
            events = listOf(BackupEvent(1, "a", "NOPE"), BackupEvent(2, "a", "CONTINUED", "NOPE")),
        )
        backups.import(BackupCodec.encode(file))
        assertEquals(emptyList<FocusSchedule>(), settings.settings.value.focusSchedules)
        assertEquals(emptySet<String>(), settings.settings.value.personalMessages)
        assertEquals(null, stats.events.value.single().reason)
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
