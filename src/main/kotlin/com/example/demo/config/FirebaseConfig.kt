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
            // Đọc chìa khóa từ biến môi trường (Environment Variable)
            val firebaseKey = System.getenv("FIREBASE_KEY")
            
            if (firebaseKey == null) {
                println(">>> Lỗi: Không tìm thấy biến môi trường FIREBASE_KEY ❌")
                return
            }

            // Xử lý chuỗi JSON từ biến môi trường để đảm bảo định dạng đúng
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(ByteArrayInputStream(firebaseKey.toByteArray())))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println(">>> NewStart Backend: Firebase đã kết nối BẢO MẬT thành công! ✅")
            }
        } catch (e: Exception) {
            println(">>> Lỗi kết nối Firebase bảo mật: ${e.message} ❌")
            // Không in e.printStackTrace() để tránh lộ thông tin nhạy cảm trong log nếu có lỗi format
        }
    }
}
