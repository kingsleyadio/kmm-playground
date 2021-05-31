package com.example.demokmm

class Greeting {
    fun greeting(name: String): String {
        return "Hello, ${Platform().platform} $name!"
    }
}
