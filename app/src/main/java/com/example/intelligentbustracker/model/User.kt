package com.example.intelligentbustracker.model

import com.google.firebase.firestore.Exclude

data class User(
    var bus: Int = 0,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var direction: Int = 0
) {
    private var _id: String = ""
    var id: String
        @Exclude get() {
            return _id
        }
        set(value) {
            _id = value
        }
    
    fun withId(id: String): User {
        this.id = id
        return this
    }
}