package com.merak.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import timber.log.Timber

class Navigator(
    val backStack: MutableList<NavKey>
) {
    private var lastPopTime = 0L

    fun push(key: NavKey) {
        if (backStack.lastOrNull() == key) {
            Timber.tag("Navigator").i("Trying push current page to backStack again, ignore.")
            return
        }
        backStack.add(key)
    }

    fun replace(key: NavKey) {
        if (backStack.isNotEmpty()) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    fun replaceAll(keys: List<NavKey>) {
        if (keys.isEmpty()) return
        backStack.clear()
        backStack.addAll(keys)
    }

    fun pop() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPopTime < 100) {
            Timber.tag("Navigator").i("pop call more than 1 times in 100ms, ignore.")
            return
        }
        if (backStack.size <= 1) return

        lastPopTime = currentTime
        backStack.removeLastOrNull()
    }

    fun current(): NavKey? = backStack.lastOrNull()

    fun backStackSize(): Int = backStack.size
}

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("LocalNavigator not provided")
}
