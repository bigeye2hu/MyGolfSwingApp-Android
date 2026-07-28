package com.mygolfswing.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 轻量导航栈：子页返回回到上一页，栈空则回 Tab 根。
 */
class V2AppNavigation<D : Any>(private val root: D) {
    var current by mutableStateOf(root)
        private set

    private var backStack: List<D> = emptyList()

    fun navigateTo(next: D) {
        if (current != root) {
            backStack = backStack + current
        }
        current = next
    }

    fun navigateBack() {
        current = backStack.lastOrNull() ?: root
        if (backStack.isNotEmpty()) {
            backStack = backStack.dropLast(1)
        }
    }

    fun navigateBackSkipping(predicate: (D) -> Boolean) {
        var nextStack = backStack
        while (nextStack.lastOrNull()?.let(predicate) == true) {
            nextStack = nextStack.dropLast(1)
        }
        current = nextStack.lastOrNull() ?: root
        backStack = if (nextStack.isNotEmpty()) nextStack.dropLast(1) else emptyList()
    }

    fun resetToRoot() {
        current = root
        backStack = emptyList()
    }

    fun resetTo(destination: D) {
        current = destination
        backStack = emptyList()
    }

    fun returnTo(destination: D) {
        if (destination == root) {
            resetToRoot()
            return
        }
        val index = backStack.indexOfLast { it == destination }
        if (index >= 0) {
            current = destination
            backStack = backStack.take(index)
        } else {
            resetTo(destination)
        }
    }

    val canGoBack: Boolean get() = current != root
}

@Composable
fun <D : Any> rememberV2AppNavigation(root: D): V2AppNavigation<D> {
    return remember(root) { V2AppNavigation(root) }
}
