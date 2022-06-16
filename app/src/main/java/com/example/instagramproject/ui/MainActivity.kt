package com.example.instagramproject.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagramproject.R
import com.example.instagramproject.adapter.PostAdapter
import com.example.instagramproject.model.Post
import com.example.instagramproject.model.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseFireStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var query: Query
    private lateinit var listenerRegistration: ListenerRegistration
    private val post_list: MutableList<Post> = mutableListOf()
    private val users_list: MutableList<User> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        val adapter = PostAdapter(this, post_list, users_list)
        recyclerViewHome.also {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = adapter
        }

        floatingActionButton.setOnClickListener {
            val intent = Intent(this, PostActivity::class.java)
            startActivity(intent)
        }

        if (firebaseAuth.currentUser != null) {

            query = firebaseFireStore.collection("Posts").orderBy("time", Query.Direction.DESCENDING)
            listenerRegistration = query.addSnapshotListener(this@MainActivity,
                object : EventListener<QuerySnapshot?> {
                    override fun onEvent(
                        value: QuerySnapshot?,
                        error: FirebaseFirestoreException?,
                    ) {
                        for (doc in value!!.documentChanges) {
                            if (doc.type === DocumentChange.Type.ADDED) {
                                val postId = doc.document.id
                                val post: Post =
                                    doc.document.toObject(Post::class.java).withId(postId)
                                val postUserId = doc.document.getString("user")
                                firebaseFireStore.collection("Users")
                                    .document(postUserId.toString()).get()
                                    .addOnCompleteListener(OnCompleteListener<DocumentSnapshot?> { task ->
                                        if (task.isSuccessful) {
                                            val users = task.result.toObject(User::class.java)
                                            users?.let { users_list.add(it) }
                                            post_list.add(post)
                                            adapter.notifyDataSetChanged()
                                        } else {
                                            Toast.makeText(this@MainActivity,
                                                task.exception?.message,
                                                Toast.LENGTH_SHORT).show()
                                        }
                                    })
                            } else {
                                adapter.notifyDataSetChanged()
                            }
                        }
                        listenerRegistration.remove()
                    }
                })
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        } else {
            val currentUserId = firebaseAuth.currentUser?.uid
            currentUserId?.let {
                firebaseFireStore.collection("Users").document(it).get().addOnCompleteListener {
                    if (it.isSuccessful) {
                        if (!it.result.exists()) {
                            startActivity(Intent(this, ProfileActivity::class.java))
                            finish()
                        }
                    }
                }
            }
        }
    }
}