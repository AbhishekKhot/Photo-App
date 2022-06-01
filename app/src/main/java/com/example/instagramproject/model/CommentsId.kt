package com.example.instagramproject.model

import com.google.firebase.firestore.Exclude

open class CommentsId {
    @Exclude
    var CommentsId:String=""

    fun <T : CommentsId?> withId(id: String): T {
        this.CommentsId = id
        return this as T
    }
}