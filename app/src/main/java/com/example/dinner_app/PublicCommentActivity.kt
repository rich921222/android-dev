package com.example.dinner_app

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*


class PublicCommentActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PublicCommentAdapter
    private val commentList = mutableListOf<Pair<String, String>>() // uid to comment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_public_comment)

        val foodName = intent.getStringExtra("foodName") ?: return
        title = "評論：$foodName"

        recyclerView = findViewById(R.id.publicCommentRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PublicCommentAdapter(commentList)
        recyclerView.adapter = adapter

        val ref = FirebaseDatabase.getInstance().reference
            .child("public_data").child("comment").child(foodName)

        val backButton: Button = findViewById(R.id.backToCommentListButton)
        backButton.setOnClickListener {
            finish() // 回到評論清單
        }

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentList.clear()
                for (child in snapshot.children) {
                    val uid = child.key ?: "未知"
                    val comment = child.getValue(String::class.java) ?: "（無內容）"
                    commentList.add(uid to comment)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
