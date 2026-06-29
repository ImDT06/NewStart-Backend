package com.example.demo.controller

import com.google.firebase.cloud.FirestoreClient
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import com.google.cloud.Timestamp

@RestController
@RequestMapping("/api/journal")
class JournalController {

    @GetMapping
    fun getJournalEntries(): List<Map<String, Any>> {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val entries = db.collection("journals")
            .whereEqualTo("userId", uid)
            .get()
            .get()

        return entries.documents.map { it.data + ("id" to it.id) }
    }

    @PostMapping
    fun createJournalEntry(@RequestBody entry: Map<String, Any>): Map<String, Any> {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        // Sửa kiểu dữ liệu Timestamp cho đúng môi trường Server
        val entryWithUser = entry + ("userId" to uid) + ("timestamp" to Timestamp.now())
        val docRef = db.collection("journals").document()
        docRef.set(entryWithUser).get()
        
        return entryWithUser + ("id" to docRef.id)
    }

    @DeleteMapping("/{id}")
    fun deleteJournalEntry(@PathVariable id: String) {
        val db = FirestoreClient.getFirestore()
        db.collection("journals").document(id).delete().get()
    }
}
