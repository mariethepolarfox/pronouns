package me.marie.pronouns.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object Multithreading {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun launch(run: suspend CoroutineScope.() -> Unit) {
        scope.launch(block = run)
    }
}