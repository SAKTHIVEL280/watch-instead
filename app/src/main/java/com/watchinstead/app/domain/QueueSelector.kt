package com.watchinstead.app.domain

/** Orders unwatched links so that the least recently shown comes first, oldest save breaking ties. */
object QueueSelector {
    fun order(links: List<SavedLink>): List<SavedLink> = links
        .filter { it.watchedAt == null }
        .sortedWith(compareBy({ it.lastShownAt ?: Long.MIN_VALUE }, { it.createdAt }, { it.id }))
}
