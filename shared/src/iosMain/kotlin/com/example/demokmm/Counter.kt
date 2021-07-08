package com.example.demokmm

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class CompositeCloseable : Closeable {
    private var closeables = mutableSetOf<Closeable>()

    fun add(closeable: Closeable) {
        closeables.add(closeable)
    }

    override fun close() {
        closeables.forEach { it.close() }
    }
}

fun Closeable.closedBy(composite: CompositeCloseable) {
    composite.add(this)
}

fun interface Closeable {
    fun close()
}

class CFlow<T>(
    private val delegate: Flow<T>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : Flow<T> by delegate {

    fun collect(onEach: (T) -> Unit): Closeable {
        val scope = CoroutineScope(Job() + dispatcher)
        delegate
            .onEach { onEach(it) }
            .launchIn(scope)

        return Closeable { scope.cancel() }
    }
}

class Suspend<T>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val action: suspend () -> T
) {

    fun run(onSuccess: (T) -> Unit, onError: (Throwable) -> Unit): Closeable {
        val scope = CoroutineScope(Job() + dispatcher)
        scope.launch {
            try {
                onSuccess(action())
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                onError(t)
            }
        }

        return Closeable { scope.cancel() }
    }
}

fun FlowCounter.wrap(): CFlow<Int> = CFlow(state())

fun FlowCounter.wrapLastState(): Suspend<Int> = Suspend { lastState() }

