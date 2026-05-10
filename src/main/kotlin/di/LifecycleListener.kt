package io.github.kperczynski.di

interface LifecycleListener {
    fun onStop()
    fun onStart()
}