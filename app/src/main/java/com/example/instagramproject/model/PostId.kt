package com.example.instagramproject.model

import androidx.annotation.NonNull

import com.google.firebase.firestore.Exclude




open class PostId {
    @Exclude
    var PostId: String? = null

    fun <T : PostId?> withId(id: String): T {
        PostId = id
        return this as T
    }
}