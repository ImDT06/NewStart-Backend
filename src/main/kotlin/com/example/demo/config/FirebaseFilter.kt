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
                val authentication = UsernamePasswordAuthenticationToken(decodedToken.uid, null, emptyList())
                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: Exception) {
                println(">>> Token không hợp lệ: ${e.message}")
            }
        }
        filterChain.doFilter(request, response)
    }
}
