package com.example.dinner_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import kotlin.random.Random

class RecommendActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var resultTextView: TextView
    private lateinit var drawButton: Button

    private val foodScores = mutableMapOf<String, Int>()
    private var totalScore = 0
    private var allEmails: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommend)

        database = FirebaseDatabase.getInstance().reference
        resultTextView = findViewById(R.id.resultTextView)
        drawButton = findViewById(R.id.drawButton)

        allEmails = intent.getStringArrayListExtra("all_emails") ?: emptyList()
        Log.d("Recommend", "收到 Email 列表: $allEmails")

        if (allEmails.isNotEmpty()) {
            loadAllUsersRatings()
        }

        drawButton.setOnClickListener {
            if (foodScores.isEmpty()) {
                resultTextView.text = "無法推薦：請先為你的喜好食物(至少有一項需要)設定星星評分 ⭐️"
            } else {
                val recommended = drawRecommendation()
                resultTextView.text = "今晚推薦：$recommended 🍽️"
            }
        }
        val backToMainButton: Button = findViewById(R.id.backToMainButton)
        backToMainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadAllUsersRatings() {
        var loadedCount = 0

        for (email in allEmails) {
            val emailKey = email.lowercase().replace(".", ",")
            database.child("emailToUid").child(emailKey)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val uid = snapshot.getValue(String::class.java)
                        if (uid != null) {
                            loadRatingsForUser(uid) {
                                loadedCount++
                                if (loadedCount == allEmails.size) {
                                    drawButton.isEnabled = true
                                    Log.d("Recommend", "所有評分資料已取得")
                                }
                            }
                        } else {
                            Log.w("Recommend", "查無 $email 的 UID")
                            loadedCount++
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Recommend", "email ➝ UID 查詢失敗: ${error.message}")
                        loadedCount++
                    }
                })
        }
    }

    private fun loadRatingsForUser(uid: String, onDone: () -> Unit) {
        database.child("users").child(uid).child("preferences").child("ratings")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val food = child.key ?: continue
                        val rating = child.getValue(Int::class.java) ?: 0
                        foodScores[food] = (foodScores[food] ?: 0) + rating
                        totalScore += rating
                    }
                    onDone()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Recommend", "讀取 $uid 評分失敗: ${error.message}")
                    onDone()
                }
            })
    }

    private fun drawRecommendation(): String {
        val rand = Random.nextDouble()
        var cumulative = 0.0

        for ((food, score) in foodScores) {
            val probability = score.toDouble() / totalScore
            cumulative += probability
            if (rand <= cumulative) {
                return food
            }
        }
        return foodScores.keys.random() // fallback：理論上不會進到這
    }
}
