package com.example.dinner_app
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
    private val onEditClick: (String) -> Unit
) : RecyclerView.Adapter<CommentListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val foodName: TextView = view.findViewById(R.id.foodNameText)
        val editButton: Button = view.findViewById(R.id.editCommentButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = foodList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val food = foodList[position]
        holder.foodName.text = food
        holder.editButton.setOnClickListener { onEditClick(food) }
    }
}
