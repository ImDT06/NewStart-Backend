package com.example.demo.controller

import com.google.firebase.cloud.FirestoreClient
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/friends")
class FriendController {

    @GetMapping
    fun getFriends(): List<Map<String, Any>> {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val friendships = db.collection("friendships")
            .whereArrayContains("userIds", uid)
            .get()
            .get()

        return friendships.documents.map { it.data + ("id" to it.id) }
    }

    @PostMapping("/request/{toUserId}")
    fun sendFriendRequest(@PathVariable toUserId: String) {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val requestData = mapOf(
            "fromUserId" to uid,
            "toUserId" to toUserId,
            "status" to "PENDING",
            "timestamp" to com.google.cloud.Timestamp.now()
        )
        db.collection("friendRequests").add(requestData).get()
    }

    @PostMapping("/accept/{requestId}")
    fun acceptFriendRequest(@PathVariable requestId: String) {
        val db = FirestoreClient.getFirestore()
        
        val docRef = db.collection("friendRequests").document(requestId)
        val request = docRef.get().get()
        
        if (request.exists()) {
            val fromUserId = request.getString("fromUserId")
            val toUserId = request.getString("toUserId")
            
            // 1. Cập nhật trạng thái lời mời
            docRef.update("status", "ACCEPTED").get()
            
            // 2. Tạo mối quan hệ bạn bè
            val friendshipData = mapOf(
                "userIds" to listOf(fromUserId, toUserId),
                "status" to "ACCEPTED",
                "createdAt" to com.google.cloud.Timestamp.now()
            )
            db.collection("friendships").add(friendshipData).get()
        }
    }
}
