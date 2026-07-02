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
            val adminId = snapshot.getString("adminId") ?: ""
            
            members.remove(uid)
            transaction.update(docRef, "members", members)
            
            if (adminId == uid) {
                if (members.isNotEmpty()) {
                    val newAdminId = members.random()
                    transaction.update(docRef, "adminId", newAdminId)
                } else {
                    transaction.update(docRef, "adminId", "")
                }
            }
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
        
        // Execute database writes and notification asynchronously to allow the HTTP response to return immediately
        java.util.concurrent.CompletableFuture.runAsync {
            try {
                val db = FirestoreClient.getFirestore()
                val senderDoc = db.collection("users").document(uid).get().get()
                val senderName = senderDoc.getString("name") ?: "Người dùng"
                
                val messageWithMetadata = message + mapOf(
                    "senderId" to uid,
                    "senderName" to senderName,
                    "timestamp" to com.google.cloud.Timestamp.now()
                )
                db.collection("squads").document(id).collection("messages").add(messageWithMetadata).get()

                val squadDoc = db.collection("squads").document(id).get().get()
                if (squadDoc.exists()) {
                    val squadName = squadDoc.getString("name") ?: "Nhóm"
                    val members = squadDoc.get("members") as? List<String> ?: emptyList()
                    val recipientIds = members.filter { it != uid }
                    
                    if (recipientIds.isNotEmpty()) {
                        val querySnapshot = db.collection("users")
                            .whereIn(com.google.cloud.firestore.FieldPath.documentId(), recipientIds)
                            .get()
                            .get()
                        
                        val text = message["text"] as? String ?: "Đã gửi một tin nhắn"
                        val fcm = com.google.firebase.messaging.FirebaseMessaging.getInstance()
                        
                        for (doc in querySnapshot.documents) {
                            val fcmToken = doc.getString("fcmToken")
                            if (!fcmToken.isNullOrEmpty()) {
                                val msg = com.google.firebase.messaging.Message.builder()
                                    .setToken(fcmToken)
                                    .setNotification(
                                        com.google.firebase.messaging.Notification.builder()
                                            .setTitle(squadName)
                                            .setBody("$senderName: $text")
                                            .build()
                                    )
                                    .putData("squadId", id)
                                    .build()
                                fcm.send(msg)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println(">>> Lỗi xử lý tin nhắn nhóm: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
