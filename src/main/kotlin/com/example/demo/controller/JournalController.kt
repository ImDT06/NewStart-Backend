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
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val docRef = db.collection("journals").document(id)
        val doc = docRef.get().get()
        if (doc.exists()) {
            val postUserId = doc.getString("userId") ?: ""
            
            // Check if requester is Admin
            val userDoc = db.collection("users").document(uid).get().get()
            val userEmail = if (userDoc.exists()) userDoc.getString("email") else ""
            
            if (userEmail == "admin@gmail.com") {
                // Admin deletes all posts of this user
                if (postUserId.isNotEmpty()) {
                    val posts = db.collection("journals").whereEqualTo("userId", postUserId).get().get()
                    for (post in posts.documents) {
                        post.reference.delete().get()
                    }
                } else {
                    docRef.delete().get()
                }
            } else {
                // Normal user only deletes their own post
                if (postUserId == uid) {
                    docRef.delete().get()
                } else {
                    throw org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, 
                        "Bạn không có quyền thực hiện hành động này."
                    )
                }
            }
        }
    }
}
