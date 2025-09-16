package com.example.dinner_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
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
    private var selectedCity: String? = null
    private lateinit var drawButton: Button
    private lateinit var auth: FirebaseAuth
    private var isPublicMode = false  // 🌟 預設是個人模式

    private val foodScores = mutableMapOf<String, Int>()
    private val publicFoodScores = mutableMapOf<String, Int>()    // 公共
    private var totalScore = 0
    private var publicTotalScore = 0
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

//        if (allEmails.isNotEmpty()) {
//            loadAllUsersRatings()
//        }
//        else{
//            allEmails = listOf(currentUserEmail)
//            loadAllUsersRatings()
//        }
        drawButton.setOnClickListener {
            val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
            allEmails = MainActivity.sessionPartnerEmails
            if (allEmails.isEmpty()) {
                allEmails = listOf(currentUserEmail)
            }

            loadAllUsersRatings {
                startRecommendation() // ⭐️ 資料載入完成後再抽獎
            }
        }

        val citySpinner: Spinner = findViewById(R.id.citySpinner)
        val cityList = listOf(
            "全部縣市",
            "台北市", "新北市", "桃園市", "台中市", "台南市", "高雄市",
            "基隆市", "新竹市", "嘉義市",
            "新竹縣", "苗栗縣", "彰化縣", "南投縣", "雲林縣", "嘉義縣",
            "屏東縣", "宜蘭縣", "花蓮縣", "台東縣",
            "澎湖縣", "金門縣", "連江縣"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cityList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        citySpinner.adapter = adapter
        citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCity = if (position == 0) null else cityList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedCity = null
            }
        }

        val publicRecommendButton: Button = findViewById(R.id.publicRecommendButton)
        publicRecommendButton.setOnClickListener {
            isPublicMode = true
            loadPublicRatings()
        }

        val backToMainButton: Button = findViewById(R.id.backToMainButton)
        backToMainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadAllUsersRatings(onDone: () -> Unit) {
        foodScores.clear()
        totalScore = 0
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
                                    Log.d("Recommend", "所有評分資料已取得")
                                    onDone()
                                }
                            }
                        } else {
                            loadedCount++
                            if (loadedCount == allEmails.size) onDone()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Recommend", "email ➝ UID 查詢失敗: ${error.message}")
                        loadedCount++
                        if (loadedCount == allEmails.size) onDone()
                    }
                })
        }
    }


    private fun loadRatingsForUser(uid: String, onDone: () -> Unit) {
        database.child("users").child(uid).child("preferences").child("ratings")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val fullName = child.key ?: continue  // e.g., "壽司郎(台北市)"
                        val rating = child.getValue(Int::class.java) ?: 0
                        // 🔍 抽取括號內的縣市名稱
                        val matchResult = Regex(".*\\((.+)\\)").find(fullName)
                        val cityInName = matchResult?.groups?.get(1)?.value

                        if (selectedCity != null && cityInName != selectedCity){
                            Log.d("CityFilter", "略過餐廳: $fullName，因為 cityInName=$cityInName 與 selectedCity=$selectedCity 不符")
                            continue
                        }
                        Log.d("CityFilter", "選擇餐廳: $fullName，cityInName=$cityInName 和 selectedCity=$selectedCity ")
                        foodScores[fullName] = (foodScores[fullName] ?: 0) + rating
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

    private fun loadPublicRatings() {
        publicFoodScores.clear()
        publicTotalScore = 0

        database.child("public_data")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (selectedCity == null)
                    {
                        for (locationSnapshot in snapshot.children) { // 地點 (縣市)
                            for (restaurantSnapshot in locationSnapshot.children) { // 餐廳
                                val foodName = restaurantSnapshot.key ?: continue
                                val score = restaurantSnapshot.child("rating").getValue(Int::class.java) ?: continue
                                publicFoodScores[foodName] = score
                                publicTotalScore += score
                            }
                        }
                    }
                    else
                    {
                        val locationSnapshot = snapshot.child(selectedCity!!)
                        for (restaurantSnapshot in locationSnapshot.children)
                        {
                            val foodName = restaurantSnapshot.key ?: continue
                            val score = restaurantSnapshot.child("rating").getValue(Int::class.java) ?: continue
                            publicFoodScores[foodName] = score
                            publicTotalScore += score
                        }
                    }

                    if (publicFoodScores.isEmpty()) {
                        resultTextView.text = "公共資料目前無法使用，請稍後再試。"
                    } else {
                        startRecommendation() // ✅ 直接啟動抽獎動畫
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PublicRecommend", "讀取公共推薦失敗: ${error.message}")
                }
            })
    }

    private fun startRecommendation() {
        if ((isPublicMode && publicFoodScores.isEmpty()) || (!isPublicMode && foodScores.isEmpty())) {
            resultTextView.text = "無法推薦：尚無符合地點的餐廳資料"
            return
        }

        slotMachineImage.setImageResource(R.drawable.slot_machine_pull)

        val foodList = if (isPublicMode) publicFoodScores.entries.toList() else foodScores.entries.toList()
        val scoreSum = if (isPublicMode) publicTotalScore else totalScore

        val rand = Random.nextDouble()
        var cumulative = 0.0
        var result = foodList.first().key
        for ((food, score) in foodList) {
            cumulative += score.toDouble() / scoreSum
            if (rand <= cumulative) {
                result = food
                break
            }
        }

        val displayOrder = foodList.map { it.key }.shuffled().toMutableList()
        if (!displayOrder.contains(result)) displayOrder.add(result)

        val totalCycles = 20
        var currentIndex = 0
        val handler = android.os.Handler()
        val delayStep = 30L

        fun animateStep(step: Int) {
            if (step >= totalCycles) {
                slotView.text = stripLocation(result)
                resultTextView.text = "今晚推薦：${stripLocation(result)} 🍽️"
                slotMachineImage.setImageResource(R.drawable.slot_machine_full)

                val uid = auth.currentUser?.uid ?: return
                val userHistoryRef = database.child("users").child(uid).child("user_history")

                val newHistoryRef = userHistoryRef.push()
                val historyData = mapOf(
                    "food" to result,
                    "timestamp" to System.currentTimeMillis()
                )
                newHistoryRef.setValue(historyData)

                userHistoryRef.orderByChild("timestamp")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val histories = snapshot.children.toList()
                            if (histories.size > 5) {
                                val excess = histories.size - 5
                                for (i in 0 until excess) {
                                    histories[i].ref.removeValue()
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("History", "歷史紀錄刪除失敗: ${error.message}")
                        }
                    })

                return
            }

            val food = stripLocation(displayOrder[currentIndex % displayOrder.size])
            slotView.text = food
            currentIndex++
            handler.postDelayed({ animateStep(step + 1) }, delayStep + (step * 8))
        }

        animateStep(0)
    }
    private fun stripLocation(fullName: String): String {
        return fullName.replace(Regex("\\s*\\(.*\\)"), "")
    }
}
