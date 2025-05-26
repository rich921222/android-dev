package com.example.dinner_app
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CommentListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CommentListAdapter
    private val foodList = mutableListOf<String>()
    private lateinit var database: DatabaseReference
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment_list)

        val backButton: Button = findViewById(R.id.backToMainButton)
        backButton.setOnClickListener {
            finish() // 返回上一個 Activity（MainActivity）
        }
        recyclerView = findViewById(R.id.commentRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CommentListAdapter(foodList) { foodName ->
            val intent = Intent(this, CommentEditActivity::class.java)
            intent.putExtra("foodName", foodName)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().reference
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        database.child("users").child(uid).child("preferences").child("food")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    foodList.clear()
                    snapshot.children.forEach {
                        it.getValue(String::class.java)?.let { food -> foodList.add(food) }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
