package com.example.dinner_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyContainer: LinearLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyContainer = findViewById(R.id.historyContainer)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        loadHistory()
        val backToMainButton: Button = findViewById(R.id.backToMainButton)
        backToMainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

    }

    private fun loadHistory() {
        val uid = auth.currentUser?.uid ?: return
        val ref = database.child("users").child(uid).child("user_history")

        ref.orderByChild("timestamp").limitToLast(5)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val historyList = findViewById<LinearLayout>(R.id.historyList)
                    historyList.removeAllViews()

                    if (!snapshot.exists()) {
                        val noData = TextView(this@HistoryActivity).apply {
                            text = "尚無推薦紀錄"
                            textSize = 16f
                        }
                        historyContainer.addView(noData)
                        return
                    }

                    val records = snapshot.children.toList().sortedByDescending {
                        it.child("timestamp").getValue(Long::class.java) ?: 0L
                    }

                    for (record in records) {
                        val food = record.child("food").getValue(String::class.java) ?: continue
                        val view = TextView(this@HistoryActivity).apply {
                            text = "🍽️ $food"
                            textSize = 18f
                            setPadding(16, 8, 16, 8)
                        }
                        historyContainer.addView(view)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("History", "讀取失敗: ${error.message}")
                }
            })
    }
}