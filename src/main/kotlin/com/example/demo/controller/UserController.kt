package com.example.demo.controller

import com.google.firebase.cloud.FirestoreClient
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController {

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: String): Map<String, Any>? {
        val db = FirestoreClient.getFirestore()
        val doc = db.collection("users").document(id).get().get()
        return if (doc.exists()) {
            doc.data + ("id" to doc.id)
        } else {
            null
        }
    }

    @PutMapping("/profile")
    fun updateProfile(@RequestBody updates: Map<String, Any>): Map<String, Any> {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val docRef = db.collection("users").document(uid)
        
        // Merge updates
        docRef.set(updates, com.google.cloud.firestore.SetOptions.merge()).get()
        
        return updates + ("id" to uid)
    }

    @GetMapping("/search")
    fun searchUsers(@RequestParam query: String): List<Map<String, Any>> {
        val queryClean = query.trim()
        if (queryClean.isEmpty()) return emptyList()
        val db = FirestoreClient.getFirestore()

        val capitalizedQuery = queryClean.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() 
        }
        val lowercaseQuery = queryClean.lowercase()

        val nameQuery1 = db.collection("users")
            .whereGreaterThanOrEqualTo("name", queryClean)
            .whereLessThanOrEqualTo("name", queryClean + "\uf8ff")
            .limit(20)
            .get()

        val nameQuery2 = if (capitalizedQuery != queryClean) {
            db.collection("users")
                .whereGreaterThanOrEqualTo("name", capitalizedQuery)
                .whereLessThanOrEqualTo("name", capitalizedQuery + "\uf8ff")
                .limit(20)
                .get()
        } else null

        val emailQuery1 = db.collection("users")
            .whereGreaterThanOrEqualTo("email", queryClean)
            .whereLessThanOrEqualTo("email", queryClean + "\uf8ff")
            .limit(20)
            .get()

        val emailQuery2 = if (lowercaseQuery != queryClean) {
            db.collection("users")
                .whereGreaterThanOrEqualTo("email", lowercaseQuery)
                .whereLessThanOrEqualTo("email", lowercaseQuery + "\uf8ff")
                .limit(20)
                .get()
        } else null

        val snapshot1 = nameQuery1.get()
        val snapshot2 = nameQuery2?.get()
        val snapshot3 = emailQuery1.get()
        val snapshot4 = emailQuery2?.get()

        val users = mutableListOf<Map<String, Any>>()
        
        users.addAll(snapshot1.documents.map { it.data + ("id" to it.id) })
        snapshot2?.let { s -> users.addAll(s.documents.map { it.data + ("id" to it.id) }) }
        users.addAll(snapshot3.documents.map { it.data + ("id" to it.id) })
        snapshot4?.let { s -> users.addAll(s.documents.map { it.data + ("id" to it.id) }) }

        return users.distinctBy { it["id"] as? String }
    }
}
