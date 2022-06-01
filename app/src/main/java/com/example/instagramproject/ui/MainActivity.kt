package com.example.instagramproject.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramproject.R
import com.example.instagramproject.adapter.PostAdapter
import com.example.instagramproject.model.Post
import com.example.instagramproject.model.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseFireStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var mRecyclerView: RecyclerView
    private var query: Query? = null
    private var listenerRegistration: ListenerRegistration? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()


        val post_list= ArrayList<Post>()
        val users_list = ArrayList<User>()
        val adapter = PostAdapter(this,post_list,users_list)

        mRecyclerView = recyclerViewHome
        mRecyclerView.also {
            it.setHasFixedSize(true)
            it.layoutManager= LinearLayoutManager(this)
            it.adapter=adapter
        }

        floatingActionButton.setOnClickListener {
            val intent = Intent(this,PostActivity::class.java)
            startActivity(intent)
        }

        if(firebaseAuth.currentUser!=null){

            query = firebaseFireStore.collection("Posts").orderBy("time",Query.Direction.DESCENDING)

            listenerRegistration = query?.addSnapshotListener(this, object : EventListener<QuerySnapshot>{
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    val doc = value?.getDocumentChanges()
                    for(x in doc!!){
                        if(x.type == DocumentChange.Type.ADDED){
                            val postId = x.document.id
                            val post: Post = x.document.toObject(Post::class.java).withId(postId)
                            val postUserId = x.document.getString("user")
                            firebaseFireStore.collection("Users").document(postUserId!!).get()
                                .addOnCompleteListener( object : OnCompleteListener<DocumentSnapshot>{
                                    override fun onComplete(task: Task<DocumentSnapshot>) {
                                        if(task.isSuccessful){
                                            val users = task.result.toObject(User::class.java)
                                            users?.let { users_list.add(it) }
                                            post_list.add(post)
                                            adapter.notifyDataSetChanged()
                                        }
                                        else{
                                          Toast.makeText(this@MainActivity,task.exception?.message.toString(),Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                })
                        }
                        else{
                            adapter.notifyDataSetChanged()
                        }
                    }
                    listenerRegistration?.remove()
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if(currentUser==null){
            startActivity(Intent(this,SignInActivity::class.java))
            finish()
        }
        else{
            val currentUserId = firebaseAuth.currentUser?.uid
            currentUserId?.let { firebaseFireStore.collection("Users").document(it).get().addOnCompleteListener {
                if(it.isSuccessful){
                    if(!it.result.exists()){
                        startActivity(Intent(this,ProfileActivity::class.java))
                        finish()
                    }
                }
            }
            }
        }
    }
}