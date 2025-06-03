package com.example.dinner_app
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CommentListAdapter(
    private val foodList: List<String>,
    private val onEditClick: (String, String) -> Unit  // 接兩個 String
) : RecyclerView.Adapter<CommentListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val foodName: TextView = view.findViewById(R.id.foodNameText)
        val editButton: Button = view.findViewById(R.id.editCommentButton)
        val viewButton: Button = view.findViewById(R.id.viewCommentButton)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = foodList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val food = foodList[position]
        val parts = food.split("|")
        val location = parts[0]
        val foodName = parts[1]
        holder.foodName.text = "$foodName ($location)"  // 🔥 顯示 餐廳名 (地區)
        holder.editButton.setOnClickListener { onEditClick(location, foodName) }
        holder.viewButton.setOnClickListener {
            val intent = Intent(holder.itemView.context, PublicCommentActivity::class.java)
            intent.putExtra("location", location)
            intent.putExtra("foodName", foodName)
            holder.itemView.context.startActivity(intent)
        }
    }
}
