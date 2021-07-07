package com.example.demokmm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun interface Closeable {
    fun close()
}

class CFlow<T>(private val flow: Flow<T>) : Flow<T> by flow {

    fun collect(onEach: (T) -> Unit): Closeable {
        val scope = CoroutineScope(Job() + Dispatchers.Main)
        flow
            .onEach { onEach(it) }
            .launchIn(scope)

        return Closeable { scope.cancel() }
    }
}

fun FlowCounter.wrap(): CFlow<Int> = CFlow(state())

