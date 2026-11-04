package com.lockin.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterventionPolicyTest {
    private val now = 1_000_000_000L
    private val settings = GuardSettings(guardedPackages = setOf("insta"))

    private fun decide(
        pkg: String = "insta",
        settings: GuardSettings = this.settings,
        lastPromptAt: Long = 0,
    ) = InterventionPolicy.shouldIntervene(pkg, "lockin", settings, now, lastPromptAt)

    @Test fun `guarded app with no pass is intercepted`() = assertTrue(decide())

    @Test fun `unguarded and own packages are ignored`() {
        assertFalse(decide(pkg = "calculator"))
        assertFalse(decide(pkg = "lockin", settings = settings.copy(guardedPackages = setOf("lockin"))))
    }

    @Test fun `disabled protection never intercepts`() =
        assertFalse(decide(settings = settings.copy(protectionEnabled = false)))

    @Test fun `active pass suppresses and expired pass does not`() {
        assertFalse(decide(settings = settings.copy(passUntil = now + 60_000)))
        assertTrue(decide(settings = settings.copy(passUntil = now - 1)))
    }

    @Test fun `pass implausibly far ahead is ignored`() =
        assertTrue(decide(settings = settings.copy(passUntil = now + 24 * 60 * 60 * 1000L)))

    @Test fun `no cooldown lets the user through once then prompts after the session ends`() {
        val noCooldown = settings.copy(cooldownMinutes = GuardSettings.NO_COOLDOWN)
        assertTrue(decide(settings = noCooldown))
        assertFalse(decide(settings = noCooldown.copy(sessionOpen = true)))
    }

    @Test fun `stale session is ignored once a cooldown is set`() =
        assertTrue(decide(settings = settings.copy(sessionOpen = true)))

    @Test fun `event bursts only prompt once`() {
        assertFalse(decide(lastPromptAt = now - 100))
        assertTrue(decide(lastPromptAt = now - InterventionPolicy.DEBOUNCE_MILLIS))
    }

    @Test fun `clock moved backwards does not debounce forever`() =
        assertTrue(decide(lastPromptAt = now + 100_000))
}
