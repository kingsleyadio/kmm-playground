package com.example.demokmm

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
