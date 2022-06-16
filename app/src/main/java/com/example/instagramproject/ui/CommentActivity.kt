package com.example.instagramproject.ui

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagramproject.R
import com.example.instagramproject.adapter.CommentAdapter
import com.example.instagramproject.model.Comments
import com.example.instagramproject.model.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_comment.*

class CommentActivity : AppCompatActivity() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseFireStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var postId: String? = null
    private val currentUserId: String = firebaseAuth.currentUser?.uid.toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        supportActionBar?.hide()


        val comments_list = mutableListOf<Comments>()
        val users_list = mutableListOf<User>()
        val adapter = CommentAdapter(this, users_list, comments_list)

        postId = intent.getStringExtra("postid")

        comment_recyclerView.also {
            it.setHasFixedSize(true)
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = adapter
        }

        firebaseFireStore.collection("Posts/" + postId + "/Comments")
            .addSnapshotListener(this
            ) { value, error ->
                val doc = value?.documentChanges
                for (x in doc!!) {
                    if (x.type == DocumentChange.Type.ADDED) {
                        val comments: Comments =
                            x.document.toObject(Comments::class.java).withId(x.document.id)
                        val userId = x.document.getString("user")

                        firebaseFireStore.collection("Users").document(userId!!).get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val users = task.result.toObject(User::class.java)
                                    users?.let { users_list.add(it) }
                                    comments_list.add(comments)
                                    adapter.notifyDataSetChanged()
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        Snackbar.make(requireViewById(R.id.parent),
                                            task.exception?.message as CharSequence,
                                            Snackbar.LENGTH_SHORT).show()
                                    }
                                }
                            }
                    } else {
                        adapter.notifyDataSetChanged()
                    }
                }
            }

        add_commentButton.setOnClickListener { v ->
            val comment = post_commentEditText.text.toString()
            if (comment.isNotEmpty()) {
                val commentMap: HashMap<String, Any> = HashMap()
                commentMap["comment"] = comment
                commentMap["time"] = FieldValue.serverTimestamp()
                commentMap["user"] = currentUserId

                firebaseFireStore.collection("Posts/" + postId + "/Comments").add(commentMap)
                    .addOnCompleteListener(object : OnCompleteListener<DocumentReference> {
                        override fun onComplete(task: Task<DocumentReference>) {
                            if (task.isSuccessful) {
                                Snackbar.make(v,
                                    "Comment added successfully",
                                    Snackbar.LENGTH_SHORT).show()
                            } else {
                                Snackbar.make(v,
                                    task.exception?.message.toString(),
                                    Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    })

            } else {
                Snackbar.make(v, "Empty fields are not allowed", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}