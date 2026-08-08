package io.github.ktor_batterypack.validation.ksp

import com.github.jknack.handlebars.EscapingStrategy

class NoopEscapingStrategy : EscapingStrategy {
    override fun escape(value: CharSequence?): CharSequence? {
        return value
    }
}
