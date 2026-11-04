package com.lockin.app.domain

/** Decides whether opening [packageName] should bring up the prompt. Free of Android types. */
object InterventionPolicy {
    /** Accessibility events arrive in bursts; a single open must only produce one prompt. */
    const val DEBOUNCE_MILLIS = 1_500L

    /** Longest pass we honour, so a clock moved backwards cannot disable protection indefinitely. */
    private const val MAX_PASS_MILLIS = 2 * 60 * 60 * 1_000L

    fun shouldIntervene(
        packageName: String,
        ownPackage: String,
        settings: GuardSettings,
        now: Long,
        lastPromptAt: Long,
    ): Boolean {
        if (!settings.protectionEnabled) return false
        if (packageName == ownPackage || packageName !in settings.guardedPackages) return false
        val debounced = now >= lastPromptAt && now - lastPromptAt < DEBOUNCE_MILLIS
        if (debounced) return false
        val passActive = settings.passUntil > now && settings.passUntil - now <= MAX_PASS_MILLIS
        return !passActive
    }
}
