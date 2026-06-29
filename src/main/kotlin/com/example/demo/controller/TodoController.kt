package com.example.demo.controller

import com.google.firebase.cloud.FirestoreClient
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/todos")
class TodoController {

    @GetMapping
    fun getTodos(): List<Map<String, Any>> {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val todos = db.collection("todos")
            .whereEqualTo("userId", uid)
            .get()
            .get()

        return todos.documents.map { it.data + ("id" to it.id) }
    }

    @PostMapping
    fun createTodo(@RequestBody todo: Map<String, Any>): Map<String, Any> {
        val uid = SecurityContextHolder.getContext().authentication!!.principal as String
        val db = FirestoreClient.getFirestore()
        
        val todoWithUser = todo + ("userId" to uid)
        val docRef = db.collection("todos").document()
        docRef.set(todoWithUser).get()
        
        return todoWithUser + ("id" to docRef.id)
    }

    @PutMapping("/{id}")
    fun updateTodo(@PathVariable id: String, @RequestBody todo: Map<String, Any>): Map<String, Any> {
        val db = FirestoreClient.getFirestore()
        db.collection("todos").document(id).set(todo).get()
        return todo + ("id" to id)
    }

    @DeleteMapping("/{id}")
    fun deleteTodo(@PathVariable id: String) {
        val db = FirestoreClient.getFirestore()
        db.collection("todos").document(id).delete().get()
    }
}
