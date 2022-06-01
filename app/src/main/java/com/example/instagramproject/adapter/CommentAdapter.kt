package com.example.instagramproject.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instagramproject.R
import com.example.instagramproject.model.Comments
import com.example.instagramproject.model.User
import kotlinx.android.synthetic.main.comment.view.*
import org.w3c.dom.Comment

class CommentAdapter(
    private val context:Context,
    private val userList:MutableList<User>,
    private val commentList: MutableList<Comments>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment,parent,false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.itemView.comment_tv.text=commentList[position].comment
        holder.itemView.comment_user.text=userList[position].name
        Glide.with(context).load(userList[position].image).into(holder.itemView.comment_Profile_pic)
    }

    override fun getItemCount(): Int {
       return commentList.size
    }
}