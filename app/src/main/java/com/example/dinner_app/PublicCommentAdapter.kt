package com.example.dinner_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class PublicCommentAdapter(
    private val commentList: List<Pair<String, String>>
) : RecyclerView.Adapter<PublicCommentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val uidText: TextView = view.findViewById(R.id.uidText)
        val commentText: TextView = view.findViewById(R.id.commentText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_public_comment, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = commentList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (uid, comment) = commentList[position]
        holder.commentText.text = "💬 評論：$comment"
    }
}
