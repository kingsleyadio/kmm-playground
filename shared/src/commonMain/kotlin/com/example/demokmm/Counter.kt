package com.example.demokmm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * @author Kingsley Adio
 * @since 11 May, 2021
 */
class Counter {

    private var value = 0

    fun increment() {
        value++
    }

    fun decrement() {
        value--
    }

    fun state() = value
}

class FlowCounter {
    private val state = MutableStateFlow(0)

    fun increment() {
        state.value++
    }

    fun decrement() {
        state.value--
    }

    fun state(): StateFlow<Int> = state.asStateFlow()

    suspend fun lastState(): Int = state.first()
}
