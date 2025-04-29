package com.example.dinner_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.random.Random

class RecommendActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var slotView: TextView
    private lateinit var slotMachineImage: ImageView
    private lateinit var resultTextView: TextView
    private lateinit var drawButton: Button
    private lateinit var auth: FirebaseAuth

    private val foodScores = mutableMapOf<String, Int>()
    private var totalScore = 0
    private var allEmails: List<String> = emptyList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommend)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        slotView = findViewById(R.id.slotView)
        slotMachineImage = findViewById(R.id.slotMachineImage)
        resultTextView = findViewById(R.id.resultTextView)
        drawButton = findViewById(R.id.drawButton)

        val currentUserEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
        allEmails = MainActivity.sessionPartnerEmails
        Log.d("Recommend", "收到 Email 列表: $allEmails")

        if (allEmails.isNotEmpty()) {
            loadAllUsersRatings()
        }
        else{
            allEmails = listOf(currentUserEmail)
            loadAllUsersRatings()
        }

        drawButton.setOnClickListener {
            if (foodScores.isEmpty()) {
                resultTextView.text = "無法推薦：請先為你的喜好食物(至少有一項需要)設定星星評分 ⭐️"
                return@setOnClickListener
            }
            slotMachineImage.setImageResource(R.drawable.slot_machine_pull) // 換成拉下圖片
            drawButton.isEnabled = false // 防止重複點擊

            val foodList = foodScores.entries.toList()
            val rand = Random.nextDouble()
            var cumulative = 0.0
            var result = foodList.first().key
            for ((food, score) in foodList) {
                cumulative += score.toDouble() / totalScore
                if (rand <= cumulative) {
                    result = food
                    break
                }
            }

            // 動畫用
            val displayOrder = foodList.map { it.key }.shuffled().toMutableList()
            if (!displayOrder.contains(result)) displayOrder.add(result)

            val totalCycles = 20 // 總共變換幾次
            var currentIndex = 0
            val handler = android.os.Handler()
            val delayStep = 30L

            fun animateStep(step: Int) {
                if (step >= totalCycles) {
                    slotView.text = result
                    resultTextView.text = "今晚推薦：$result 🍽️"
                    drawButton.isEnabled = true
                    slotMachineImage.setImageResource(R.drawable.slot_machine_full)

                    val uid = auth.currentUser?.uid ?: return
                    val userHistoryRef = database.child("user_history").child(uid)

                    // 1. 先 push 一筆新的推薦
                    val newHistoryRef = userHistoryRef.push()
                    val historyData = mapOf(
                        "food" to result,
                        "timestamp" to System.currentTimeMillis()
                    )
                    newHistoryRef.setValue(historyData)

                    // 2. 然後檢查是否超過5筆，刪除舊的
                    userHistoryRef.orderByChild("timestamp")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val histories = snapshot.children.toList()
                                if (histories.size > 5) {
                                    val excess = histories.size - 5
                                    for (i in 0 until excess) {
                                        histories[i].ref.removeValue() // ❌ 刪掉最舊的
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("History", "歷史紀錄刪除失敗: ${error.message}")
                            }
                        })

                    return
                }

                val food = displayOrder[currentIndex % displayOrder.size]
                slotView.text = food
                currentIndex++
                handler.postDelayed({ animateStep(step + 1) }, delayStep + (step * 8)) // 每輪變慢一點
            }

            animateStep(0)
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
