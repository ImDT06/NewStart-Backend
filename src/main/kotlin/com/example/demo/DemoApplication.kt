package com.example.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    println(">>> NewStart Backend đang khởi động...")
    runApplication<DemoApplication>(*args)
}
