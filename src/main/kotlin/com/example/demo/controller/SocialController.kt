package com.example.demo.controller

import com.google.firebase.cloud.FirestoreClient
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/social")
class SocialController {

    @GetMapping("/feed")
    fun getSocialFeed(): List<Map<String, Any>> {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        // 1. Lấy danh sách bạn bè
        val friends = db.collection("friendships")
            .whereArrayContains("userIds", uid)
            .get()
            .get()
            .documents
            .flatMap { it.get("userIds") as List<String> }
            .filter { it != uid }

        // 2. Lấy các bài đăng của bản thân và bạn bè có quyền FRIENDS hoặc cộng đồng
        // Lưu ý: Firestore không hỗ trợ truy vấn phức tạp như "IN" quá nhiều giá trị, 
        // nên tạm thời lấy bài đăng của bạn bè
        val userIdsToFetch = friends + uid
        
        if (userIdsToFetch.isEmpty()) return emptyList()

        val entries = db.collection("journals")
            .whereIn("userId", userIdsToFetch.take(10)) // Giới hạn 10 người đầu tiên cho đơn giản
            .get()
            .get()

        return entries.documents
            .map { it.data + ("id" to it.id) }
            .sortedByDescending { (it["timestamp"] as? com.google.cloud.Timestamp)?.seconds ?: 0L }
    }

    @PostMapping("/react/{postId}")
    fun reactToPost(@PathVariable postId: String, @RequestParam emoji: String) {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val docRef = db.collection("journals").document(postId)
        val doc = docRef.get().get()
        
        if (doc.exists()) {
            val reactions = doc.get("reactions") as? MutableMap<String, String> ?: mutableMapOf()
            reactions[uid] = emoji
            docRef.update("reactions", reactions).get()
        }
    }
}
