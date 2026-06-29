package com.example.demo.controller

import com.google.firebase.cloud.FirestoreClient
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/habits")
class HabitController {

    @GetMapping
    fun getHabits(@RequestParam date: String): List<Map<String, Any>> {
        // Thêm dấu !! sau authentication để sửa lỗi của bạn
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        println(">>> Lấy dữ liệu cho User: $uid vào ngày: $date")
        
        val habits = db.collection("habits")
            .whereEqualTo("userId", uid)
            .whereEqualTo("date", date)
            .get()
            .get()

        return habits.documents.map { it.data + ("id" to it.id) }
    }

    @PostMapping
    fun createHabit(@RequestBody habit: Map<String, Any>): Map<String, Any> {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val habitWithUser = habit + ("userId" to uid)
        val docRef = db.collection("habits").document()
        docRef.set(habitWithUser).get()
        
        return habitWithUser + ("id" to docRef.id)
    }
}
