package com.watchinstead.app.data

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

fun interface Clock {
    fun now(): Long
}
