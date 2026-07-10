package me.marie.pronouns.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

object Multithreading {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun launch(run: suspend CoroutineScope.() -> Unit): Job = scope.launch(block = run)

    fun <T> async(run: suspend CoroutineScope.() -> T) = scope.async { run() }
}
