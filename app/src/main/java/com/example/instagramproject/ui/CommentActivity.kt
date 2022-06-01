package com.example.instagramproject.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramproject.R
import com.example.instagramproject.adapter.CommentAdapter
import com.example.instagramproject.model.Comments
import com.example.instagramproject.model.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_comment.*

class CommentActivity : AppCompatActivity() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseFireStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var postId:String?=null
    private lateinit var recyclerView: RecyclerView
    private val currentUserId:String = firebaseAuth.currentUser!!.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        supportActionBar?.hide()
        recyclerView = comment_recyclerView
        val comments_list= mutableListOf<Comments>()
        val users_list= mutableListOf<User>()
        val adapter = CommentAdapter(this,users_list,comments_list)

        postId = getIntent().getStringExtra("postid")

        recyclerView.also {
            it.setHasFixedSize(true)
            it.layoutManager= LinearLayoutManager(this)
            it.adapter=adapter
        }

        firebaseFireStore.collection("Posts/" + postId + "/Comments").addSnapshotListener(this,object : EventListener<QuerySnapshot>{
            override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                val doc = value?.getDocumentChanges()
                for(x in doc!!){
                    if(x.type == DocumentChange.Type.ADDED){
                        val comments:Comments = x.document.toObject(Comments::class.java).withId(x.document.id)
                        val userId = x.document.getString("user")

                        firebaseFireStore.collection("Users").document(userId!!).get()
                            .addOnCompleteListener(object :OnCompleteListener<DocumentSnapshot>{
                                override fun onComplete(task: Task<DocumentSnapshot>) {
                                    if(task.isSuccessful){
                                        val users = task.result.toObject(User::class.java)
                                        users?.let { users_list.add(it) }
                                        comments_list.add(comments)
                                        adapter.notifyDataSetChanged()
                                    }
                                    else{
                                        Toast.makeText(this@CommentActivity,task.exception?.message.toString(),Toast.LENGTH_SHORT).show()
                                    }
                                }
                            })
                    }
                    else{
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        })

        add_commentButton.setOnClickListener {
            val comment = post_commentEditText.text.toString()
            if(comment.isNotEmpty()){
                val commentMap:HashMap<String,Any> = HashMap()
                commentMap["comment"]=comment
                commentMap["time"]=FieldValue.serverTimestamp()
                commentMap["user"]=currentUserId

                firebaseFireStore.collection("Posts/" + postId + "/Comments").add(commentMap)
                    .addOnCompleteListener(object :OnCompleteListener<DocumentReference>{
                        override fun onComplete(task: Task<DocumentReference>) {
                            if(task.isSuccessful){
                                Toast.makeText(this@CommentActivity,"Comment added successfully",Toast.LENGTH_SHORT).show()
                            }
                            else{
                                Toast.makeText(this@CommentActivity,task.exception?.message.toString(),Toast.LENGTH_SHORT).show()
                            }
                        }
                    })

            }
            else{
                Toast.makeText(this,"Empty fields are not allowed",Toast.LENGTH_SHORT).show()
            }
        }
    }
}