package com.example.demo.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import jakarta.annotation.PostConstruct

@Configuration
class FirebaseConfig {

    @PostConstruct
    fun initialize() {
        try {
            val resource = ClassPathResource("service-account.json")
            if (!resource.exists()) {
                println(">>> Lỗi: Không tìm thấy file service-account.json trong src/main/resources ❌")
                return
            }
            
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(resource.inputStream))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println(">>> NewStart Backend: Firebase đã kết nối thành công! ✅")
            }
        } catch (e: Exception) {
            println(">>> Lỗi kết nối Firebase: ${e.message} ❌")
            e.printStackTrace()
        }
    }
}
