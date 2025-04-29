package com.example.dinner_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexboxLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.JustifyContent

class MainActivity : AppCompatActivity() {

    //跨畫面同行者暫存器
    companion object {
        var sessionPartnerEmails: MutableList<String> = mutableListOf()
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var preferencesContainer: LinearLayout
    private var preferencesListener: ValueEventListener? = null
    private var partnerEmailList: List<String> = emptyList()

    private val partnerActivityLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            partnerEmailList = data?.getStringArrayListExtra("partner_emails") ?: emptyList()
            Log.d("Main", "收到同行者名單: $partnerEmailList")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        preferencesContainer = findViewById(R.id.preferencesContainer)

        // 檢查使用者是否已經登入
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // 結束當前 Activity，防止回退
            return
        }

        // 登出按鈕
        val logoutButton: Button = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val userRef = database.child("users").child(userId).child("preferences")
                preferencesListener?.let { userRef.removeEventListener(it) } // ✅ 先移除監聽器
            }
            auth.signOut() // 登出 Firebase
            startActivity(Intent(this, LoginActivity::class.java))
            MainActivity.sessionPartnerEmails.clear()
            finish()
        }

        // 修改按鈕
        val editPreferencesButton: Button = findViewById(R.id.editPreferencesButton)
        editPreferencesButton.setOnClickListener {
            startActivity(Intent(this, EditPreferencesActivity::class.java))
        }
        //鈕跳轉到 PartnerActivity
        val partnerButton: Button = findViewById(R.id.partnerButton)
        partnerButton.setOnClickListener {
            partnerActivityLauncher.launch(Intent(this, PartnerActivity::class.java))
        }

        //推薦按鈕
        val recommendButton: Button = findViewById(R.id.recommendButton)
        recommendButton.setOnClickListener {
            val intent = Intent(this, RecommendActivity::class.java)

//            // 傳遞自己 email + 所有 partner email（用來查資料）
//            val currentEmail = auth.currentUser?.email ?: ""
//            val allEmails = ArrayList<String>().apply {
//                add(currentEmail)
//                addAll(partnerEmailList)
//            }
//
//            intent.putStringArrayListExtra("all_emails", allEmails)
            startActivity(intent)
        }
        //歷史紀錄按鈕
        val historyButton: Button = findViewById(R.id.historyButton)
        historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId).child("preferences")

        // 移除舊監聽器，避免重複綁定
        preferencesListener?.let { userRef.removeEventListener(it) }

        // 監聽 Firebase 變更
        preferencesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                preferencesContainer.removeAllViews()

                // ⭐️ 先抓 ratings，不論 snapshot.exists()
                val ratingMap = snapshot.child("ratings").children.associate { child ->
                    val key = child.key ?: ""
                    val value = (child.getValue(Int::class.java) ?: 0)
                    key to value
                }
                Log.d("Firebase", "ratings raw: ${snapshot.child("ratings").value}")

                val foodList = snapshot.child("food").children.mapNotNull { it.getValue(String::class.java) }
                val allergyList = snapshot.child("allergies").children.mapNotNull { it.getValue(String::class.java) }

                if (snapshot.exists()) {
                    addSectionWithRatings("🍽 喜好食物", foodList, ratingMap)
                    addSection("🚫 過敏原", allergyList)
                } else {
                    // 也可加一點 fallback
                    addSection("🍽 喜好食物", listOf("尚未設定"))
                    addSection("🚫 過敏原", listOf("尚未設定"))
                }
            }


            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "讀取失敗: ${error.message}")
            }
        }
        userRef.addValueEventListener(preferencesListener!!)
    }

    override fun onStop() {
        super.onStop()
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId).child("preferences")

        // 移除監聽器，避免內存洩漏
        preferencesListener?.let { userRef.removeEventListener(it) }
    }
    private fun addSection(title: String, items: List<String>) {
        val titleView = TextView(this).apply {
            text = title
            textSize = 18f
            setPadding(0, 16, 0, 8)
        }
        preferencesContainer.addView(titleView)

        val blockContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL // 每個 block 一行
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        for (item in items.ifEmpty { listOf("無") }) {
            val block = TextView(this).apply {
                text = item
                setPadding(24, 12, 24, 12)
                textSize = 16f
                setBackgroundResource(R.drawable.block_background)
                setTextColor(resources.getColor(android.R.color.black))
                val params = FlexboxLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,  // ✅ 撐滿整行
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(8, 8, 8, 8)
                layoutParams = params
            }
            blockContainer.addView(block)
        }

        preferencesContainer.addView(blockContainer)
    }
    private fun addSectionWithRatings(title: String, items: List<String>, ratings: Map<String, Int>) {
        val userId = auth.currentUser?.uid ?: return

        val titleView = TextView(this).apply {
            text = title
            textSize = 18f
            setPadding(0, 16, 0, 8)
        }
        preferencesContainer.addView(titleView)

        for (item in items.ifEmpty { listOf("無") }) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 8, 0, 8)
            }

            val nameView = TextView(this).apply {
                text = item
                textSize = 16f
                setPadding(16, 8, 16, 8)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val starsLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val currentRating = ratings[item] ?: 0
            val starViews = mutableListOf<ImageView>()

            for (i in 1..5) {
                val star = ImageView(this).apply {
                    setImageResource(if (i <= currentRating) R.drawable.star_filled else R.drawable.star_empty)
                    setPadding(4, 0, 4, 0)
                    setOnClickListener {
                        // 更新所有星星狀態
                        starViews.forEachIndexed { index, imageView ->
                            imageView.setImageResource(
                                if (index < i) R.drawable.star_filled else R.drawable.star_empty
                            )
                        }
                        // 寫入 Firebase
                        val ratingRef = database.child("users").child(userId)
                            .child("preferences").child("ratings").child(item)
                        ratingRef.setValue(i)
                    }
                }
                starViews.add(star)
                starsLayout.addView(star)
            }

            rowLayout.addView(nameView)
            rowLayout.addView(starsLayout)
            preferencesContainer.addView(rowLayout)
        }
    }


}
