package com.example.dinner_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.absoluteValue


class PublicCommentAdapter(
    private val commentList: List<Pair<String, String>>
) : RecyclerView.Adapter<PublicCommentAdapter.ViewHolder>() {

    private val adjectives = listOf("神秘", "快樂", "溫柔", "勇敢", "活潑", "可愛", "聰明", "沉靜", "陽光", "冷靜")
    private val animals = listOf("貓咪", "松鼠", "狐狸", "熊熊", "兔子", "貓頭鷹", "刺蝟", "海豚", "狼", "麋鹿")

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
        holder.uidText.text = "🧑‍💻 ${generateAnonymousName(uid)}"
        holder.commentText.text = "💬 評論：$comment"
    }
    private fun generateAnonymousName(uid: String): String {
        val adjectiveIndex = (uid.hashCode().absoluteValue) % adjectives.size
        val animalIndex = (uid.reversed().hashCode().absoluteValue) % animals.size
        return adjectives[adjectiveIndex] + animals[animalIndex]
    }

}
