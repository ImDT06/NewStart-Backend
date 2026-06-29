package com.example.demo.controller

import com.google.firebase.cloud.FirestoreClient
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/squads")
class SquadController {

    @GetMapping
    fun getMySquads(): List<Map<String, Any>> {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val squads = db.collection("squads")
            .whereArrayContains("members", uid)
            .get()
            .get()

        return squads.documents.map { it.data + ("id" to it.id) }
    }

    @PostMapping
    fun createSquad(@RequestBody squad: Map<String, Any>): Map<String, Any> {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val members = (squad["members"] as? List<String> ?: emptyList()) + uid
        val squadWithMetadata = squad + mapOf(
            "adminId" to uid,
            "members" to members.distinct(),
            "createdAt" to com.google.cloud.Timestamp.now()
        )
        
        val docRef = db.collection("squads").document()
        docRef.set(squadWithMetadata).get()
        
        return squadWithMetadata + ("id" to docRef.id)
    }

    @PostMapping("/{id}/join")
    fun joinSquad(@PathVariable id: String) {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val docRef = db.collection("squads").document(id)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef).get()
            val members = snapshot.get("members") as? MutableList<String> ?: mutableListOf()
            if (!members.contains(uid)) {
                members.add(uid)
                transaction.update(docRef, "members", members)
            }
        }.get()
    }

    @PostMapping("/{id}/leave")
    fun leaveSquad(@PathVariable id: String) {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val docRef = db.collection("squads").document(id)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef).get()
            val members = snapshot.get("members") as? MutableList<String> ?: mutableListOf()
            members.remove(uid)
            transaction.update(docRef, "members", members)
        }.get()
    }

    @GetMapping("/{id}/messages")
    fun getMessages(@PathVariable id: String): List<Map<String, Any>> {
        val db = FirestoreClient.getFirestore()
        val messages = db.collection("squads").document(id).collection("messages")
            .orderBy("timestamp")
            .get()
            .get()
        return messages.documents.map { it.data + ("id" to it.id) }
    }

    @PostMapping("/{id}/messages")
    fun sendMessage(@PathVariable id: String, @RequestBody message: Map<String, Any>) {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val messageWithMetadata = message + mapOf(
            "senderId" to uid,
            "timestamp" to com.google.cloud.Timestamp.now()
        )
        db.collection("squads").document(id).collection("messages").add(messageWithMetadata).get()
    }
}
