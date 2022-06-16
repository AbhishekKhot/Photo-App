package com.example.instagramproject.adapter

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instagramproject.R
import com.example.instagramproject.model.Post
import com.example.instagramproject.model.User
import com.example.instagramproject.ui.CommentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.post.view.*
import java.util.*
import kotlin.collections.HashMap

class PostAdapter(
     val context: Context,
     val postList: MutableList<Post>,
     val usersList: MutableList<User>,
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var firestore = FirebaseFirestore.getInstance()
    private var auth = FirebaseAuth.getInstance()

    inner class PostViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(LayoutInflater.from(context).inflate(R.layout.post, parent, false))
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        Glide.with(context).load(post.image).into(holder.itemView.user_post)

        holder.itemView.caption_tv.text = post.caption

        val milliseconds = post.time!!.time
        val date = DateFormat.format("MM/dd/yyyy", Date(milliseconds)).toString()
        holder.itemView.date_tv.text = date

        val user_name=usersList[position].name
        val user_image=usersList[position].image

        Glide.with(context).load(user_image).into(holder.itemView.profile_pic)

        holder.itemView.username_tv.text = user_name

        val postId = postList[position].PostId
        val currentUserId = auth!!.currentUser!!.uid

        holder.itemView.like_btn.setOnClickListener {
            firestore!!.collection("Posts/" + postId + "/Likes").document(currentUserId).get()
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
                        holder.itemView.like_btn.setImageDrawable(ContextCompat.getDrawable(context,
                            R.drawable.ic_after_like))
                    } else {
                        holder.itemView.like_btn.setImageDrawable(ContextCompat.getDrawable(context,
                            R.drawable.ic_before_like))
                    }
                }
            }

        firestore!!.collection("Posts/" + postId + "/Likes").addSnapshotListener { value, error ->
            if (error == null) {
                if (!value!!.isEmpty) {
                    val count = value.size()
                    holder.itemView.like_count_tv.text = count.toString()
                } else {
                    holder.itemView.like_count_tv.text = "0"
                }
            }
        }

        holder.itemView.comments_post.setOnClickListener {
            val commentIntent = Intent(context, CommentActivity::class.java)
            commentIntent.putExtra("postid", postId)
            context.startActivity(commentIntent)
        }

        if (currentUserId == post.user) {
            holder.itemView.delete_btn.visibility = View.VISIBLE
            holder.itemView.delete_btn.isClickable = true
            holder.itemView.delete_btn.setOnClickListener {
                val alert = AlertDialog.Builder(context)
                alert.setTitle("DELETE")
                    .setMessage("DO YOU WANT TO DELETE THIS POST ?")
                    .setNegativeButton("NO", null)
                    .setPositiveButton("YES") { dialog, which ->
                        firestore!!.collection("Posts/" + postId + "/Comments").get()
                            .addOnCompleteListener { task ->
                                for (snapshot in task.result) {
                                    firestore!!.collection("Posts/" + postId + "/Comments")
                                        .document(snapshot.id).delete()
                                }
                            }
                        firestore!!.collection("Posts/" + postId + "/Likes").get()
                            .addOnCompleteListener { task ->
                                for (snapshot in task.result) {
                                    firestore!!.collection("Posts/" + postId + "/Likes")
                                        .document(snapshot.id).delete()
                                }
                            }
                        firestore!!.collection("Posts").document(postId!!).delete()
                        postList.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, postList.size)
                    }
                alert.show()
            }
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }
}