package com.example.demo.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct
import java.io.ByteArrayInputStream

@Configuration
class FirebaseConfig {

    @PostConstruct
    fun initialize() {
        try {
            val firebaseKey = System.getenv("FIREBASE_KEY")
            
            if (firebaseKey.isNullOrEmpty()) {
                println(">>> CẢNH BÁO: Không tìm thấy FIREBASE_KEY. Server sẽ chạy không có kết nối Database. ⚠️")
                return
            }

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(ByteArrayInputStream(firebaseKey.toByteArray())))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println(">>> NewStart Backend: Firebase đã kết nối BẢO MẬT thành công! ✅")
            }
        } catch (e: Exception) {
            println(">>> Lỗi kết nối Firebase: ${e.message}")
        }
    }
}
