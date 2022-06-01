package com.example.instagramproject.adapter

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import android.view.LayoutInflater
import com.bumptech.glide.Glide
import android.text.format.DateFormat
import androidx.appcompat.app.AlertDialog
import com.example.instagramproject.R
import com.example.instagramproject.model.Post
import com.example.instagramproject.model.User
import com.example.instagramproject.ui.CommentActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.post.view.*
import kotlin.collections.HashMap
import java.util.Date;

class PostAdapter(
    private val context: Context,
    private val postList: ArrayList<Post>,
    private val usersList: ArrayList<User>,
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null

    inner class PostViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.post, parent, false)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        Glide.with(context).load(post.image).into(holder.itemView.user_post)

        holder.itemView.caption_tv.text = post.caption

        val milliseconds = post.time!!.time
        val date = DateFormat.format("MM/dd/yyyy", Date(milliseconds)).toString()
        holder.itemView.date_tv.text=date

        var username:String?=null
        var image:String?=null

        usersList[position].name?.let {
            username=it;
        }
        usersList[position].image?.let {
            image=it
        }

        Glide.with(context).load(image).into(holder.itemView.profile_pic)

        holder.itemView.username_tv.text=username

        val postId = postList[position].PostId
        val currentUserId = auth!!.currentUser!!.uid

        holder.itemView.like_btn.setOnClickListener {
            firestore!!.collection("Posts/" + postId+ "/Likes").document(currentUserId).get()
                .addOnCompleteListener { task ->
                    if (!task.result.exists()) {
                        val likesMap: MutableMap<String, Any> = HashMap()
                        likesMap["timestamp"] = FieldValue.serverTimestamp()
                        firestore!!.collection("Posts/" + postId + "/Likes").document(currentUserId).set(likesMap)
                    } else {
                        firestore!!.collection("Posts/" + postId + "/Likes").document(currentUserId).delete()
                    }
                }
        }

        firestore!!.collection("Posts/" + postId + "/Likes").document(currentUserId)
            .addSnapshotListener { value, error ->
                if (error == null) {
                    if (value!!.exists()) {
                        holder.itemView.like_btn
                        holder.itemView.like_btn.setImageDrawable(context.getDrawable(R.drawable.ic_after_like))
                    } else {
                        holder.itemView.like_btn.setImageDrawable(context.getDrawable(R.drawable.ic_before_like))
                    }
                }
            }

        firestore!!.collection("Posts/" + postId + "/Likes").addSnapshotListener { value, error ->
            if (error == null) {
                if (!value!!.isEmpty) {
                    val count = value.size()
                    holder.itemView.like_count_tv.text=count.toString()
                } else {
                    holder.itemView.like_count_tv.text="0"
                }
            }
        }

        holder.itemView.comments_post.setOnClickListener {
            val commentIntent = Intent(context, CommentActivity::class.java)
            commentIntent.putExtra("postid", postId)
            context.startActivity(commentIntent)
        }

        if (currentUserId == post.user) {
            holder.itemView.delete_btn.visibility=View.VISIBLE
            holder.itemView.delete_btn.isClickable=true
            holder.itemView.delete_btn.setOnClickListener {
                val alert = AlertDialog.Builder(context)
                alert.setTitle("DELETE")
                    .setMessage("DO YOU WANT TO DELETE THIS POST ?")
                    .setNegativeButton("NO", null)
                    .setPositiveButton("YES") { dialog, which ->
                        firestore!!.collection("Posts/" + postId + "/Comments").get()
                            .addOnCompleteListener { task ->
                                for (snapshot in task.result) {
                                    firestore!!.collection("Posts/" + postId + "/Comments").document(snapshot.id).delete()
                                }
                            }
                        firestore!!.collection("Posts/" + postId + "/Likes").get()
                            .addOnCompleteListener { task ->
                                for (snapshot in task.result) {
                                    firestore!!.collection("Posts/" + postId + "/Likes").document(snapshot.id).delete()
                                }
                            }
                        firestore!!.collection("Posts").document(postId!!).delete()
                        postList.drop(position)
                        notifyDataSetChanged()
                    }
                alert.show()
            }
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }
}