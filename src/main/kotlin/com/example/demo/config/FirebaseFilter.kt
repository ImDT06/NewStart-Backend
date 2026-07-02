package com.example.demo.config

import com.google.firebase.auth.FirebaseAuth
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class FirebaseFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val idToken = header.substring(7)
            try {
                val decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken)
                val uid = decodedToken.uid
                
                // Kiểm tra xem người dùng có bị khóa tài khoản không
                val db = com.google.firebase.cloud.FirestoreClient.getFirestore()
                val blockDoc = db.collection("blocked_users").document(uid).get().get()
                if (blockDoc.exists() && blockDoc.getBoolean("blocked") == true) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản của bạn đã bị khóa.")
                    return
                }
                
                val authentication = UsernamePasswordAuthenticationToken(uid, null, emptyList())
                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: Exception) {
                println(">>> Token không hợp lệ hoặc người dùng bị khóa: ${e.message}")
            }
        }
        filterChain.doFilter(request, response)
    }
}
