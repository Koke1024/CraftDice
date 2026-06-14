package com.koke1024.craftdice.core

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

object AppLogger {

    private var initialized = false

    fun init() {
        if (!initialized) {
            Napier.base(DebugAntilog())
            initialized = true
        }
    }

    fun v(message: String, throwable: Throwable? = null) {
        Napier.v(message, throwable)
    }

    fun d(message: String, throwable: Throwable? = null) {
        Napier.d(message, throwable)
    }

    fun i(message: String, throwable: Throwable? = null) {
        Napier.i(message, throwable)
    }

    fun w(message: String, throwable: Throwable? = null) {
        Napier.w(message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Napier.e(message, throwable)
    }
}
