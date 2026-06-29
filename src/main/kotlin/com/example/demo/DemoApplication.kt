package com.example.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    for (i in 1..20) println("==========================================")
    println(">>> HỆ THỐNG NEWSTART ĐANG BẮT ĐẦU CHẠY...")
    println(">>> PORT ĐANG DÙNG: " + System.getenv("PORT"))
    for (i in 1..20) println("==========================================")

    runApplication<DemoApplication>(*args)
}
