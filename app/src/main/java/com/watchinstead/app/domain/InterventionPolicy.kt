package com.watchinstead.app.domain

/** Decides whether opening [packageName] should bring up the prompt. Free of Android types. */
object InterventionPolicy {
    /** Accessibility events arrive in bursts; a single open must only produce one prompt. */
    const val DEBOUNCE_MILLIS = 1_500L

    /** Covers the guarded app resurfacing while the launcher animates in after "go home". */
    const val GO_HOME_GRACE_MILLIS = 3_000L

    /** Longest pass we honour, so a clock moved backwards cannot disable protection indefinitely. */
    private const val MAX_PASS_MILLIS = 2 * 60 * 60 * 1_000L

    fun shouldIntervene(
        packageName: String,
        ownPackage: String,
        settings: GuardSettings,
        now: Long,
        lastPromptAt: Long,
        focusActive: Boolean = false,
    ): Boolean {
        if (!settings.protectionEnabled) return false
        if (packageName == ownPackage || packageName !in settings.guardedPackages) return false
        val debounced = now >= lastPromptAt && now - lastPromptAt < DEBOUNCE_MILLIS
        if (debounced) return false
        // Without a cooldown the user is still let through once, or the prompt would reopen forever.
        val promptsEveryOpen = focusActive || settings.cooldownMinutes == GuardSettings.NO_COOLDOWN
        if (promptsEveryOpen && settings.sessionOpen) return false
        // Modes that prompt every open must not be undone by a cooldown started before they applied.
        val longestPass = if (promptsEveryOpen) GO_HOME_GRACE_MILLIS else MAX_PASS_MILLIS
        val passActive = settings.passUntil > now && settings.passUntil - now <= longestPass
        return !passActive
    }
}
