package com.example.dinner_app

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PartnerActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var inputEmail: EditText
    private lateinit var searchButton: Button
    private lateinit var resultContainer: LinearLayout
    private val selectedEmails = mutableListOf<String>()
    private lateinit var selectedPartnerList: LinearLayout
    private lateinit var selectedPartnersTitle: TextView
    private val emailToViewMap = mutableMapOf<String, View>()
    val currentEmail = FirebaseAuth.getInstance().currentUser?.email


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner)

        database = FirebaseDatabase.getInstance().reference
        inputEmail = findViewById(R.id.inputEmail)
        searchButton = findViewById(R.id.searchButton)
        resultContainer = findViewById(R.id.resultContainer)
        selectedPartnerList = findViewById(R.id.selectedPartnerList)
        selectedPartnersTitle = findViewById(R.id.selectedPartnersTitle)

        searchButton.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                val emailKey = email.replace(".", ",")
                findPartnerUid(emailKey, email)
            } else {
                Toast.makeText(this, "請輸入同行者 Email", Toast.LENGTH_SHORT).show()
            }
        }

        Log.d("Partner", "目前已選擇的同行者 email: $selectedEmails")
        val backToMainButton: Button = findViewById(R.id.backToMainButton)
        backToMainButton.setOnClickListener {
            Log.d("Partner", "即將回傳給 MainActivity: $selectedEmails")
            MainActivity.sessionPartnerEmails = selectedEmails.toMutableList()
            val resultIntent = Intent()
            resultIntent.putStringArrayListExtra("partner_emails", ArrayList(selectedEmails))
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        selectedEmails.addAll(MainActivity.sessionPartnerEmails)
        currentEmail?.let {
            if (!selectedEmails.contains(it)) {
                selectedEmails.add(it)
            }
        }
        refreshSelectedPartnerList()

        // 如果要的話，也可以自動幫這些 email 再抓一次偏好資料
        for (email in selectedEmails) {
            val emailKey = email.lowercase().replace(".", ",")
            findPartnerUid(emailKey, email)
        }
    }

    private fun refreshSelectedPartnerList() {
        selectedPartnerList.removeAllViews()

        if (selectedEmails.isEmpty()) {
            selectedPartnerList.visibility = LinearLayout.GONE
            selectedPartnersTitle.visibility = TextView.GONE
            return
        }

        selectedPartnerList.visibility = LinearLayout.VISIBLE
        selectedPartnersTitle.visibility = TextView.VISIBLE

        for (email in selectedEmails) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }

            val emailView = TextView(this).apply {
                text = email
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val removeButton = Button(this).apply {
                text = "❌"
                setOnClickListener {
                    selectedEmails.remove(email)
                    emailToViewMap[email]?.let { view: View ->
                        resultContainer.removeView(view)
                        emailToViewMap.remove(email)
                    }
                    refreshSelectedPartnerList()
                }
            }

            rowLayout.addView(emailView)
            rowLayout.addView(removeButton)
            selectedPartnerList.addView(rowLayout)
        }
    }


    private fun findPartnerUid(emailKey: String, rawEmail: String) {
        database.child("emailToUid").child(emailKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val uid = snapshot.getValue(String::class.java)
                    if (uid != null) {
                        loadPartnerPreferences(uid, rawEmail)
                        inputEmail.setText("") // ✅ 清空輸入欄位
                        if (!selectedEmails.contains(rawEmail)) {
                            selectedEmails.add(rawEmail)
                            refreshSelectedPartnerList()
                        }
                    } else {
                        Toast.makeText(this@PartnerActivity, "查無此使用者", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Partner", "查詢失敗: ${error.message}")
                }
            })
    }

    private fun loadPartnerPreferences(uid: String, email: String) {
        val prefRef = database.child("users").child(uid).child("preferences")
        prefRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // ✅ 不清除原本的內容，保留已查詢者的資料

                val personContainer = LinearLayout(this@PartnerActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 16, 0, 16)
                }

                // 存起來對應 email ➝ 該容器
                emailToViewMap[email] = personContainer
                resultContainer.addView(personContainer)
                val titleView = TextView(this@PartnerActivity).apply {
                    text = "👤 Partner：$email"
                    textSize = 17f
                    setPadding(0, 0, 0, 12)
                    setTypeface(null, Typeface.BOLD)
                }
                personContainer.addView(titleView)


                val ratingMap = snapshot.child("ratings").children.associate { child ->
                    val key = child.key ?: ""
                    val value = (child.getValue(Int::class.java) ?: 0)
                    key to value
                }

                val foodList = snapshot.child("food").children.mapNotNull { it.getValue(String::class.java) }
                val allergyList = snapshot.child("allergies").children.mapNotNull { it.getValue(String::class.java) }

                addSectionWithRatings("🍽 Favorite Restaurant", foodList, ratingMap,personContainer)
                addSection("🚫 Allergies", allergyList,personContainer)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Partner", "讀取失敗: ${error.message}")
            }
        })
    }

    private fun addSection(title: String, items: List<String>, container: LinearLayout = resultContainer) {
        val titleView = TextView(this).apply {
            text = title
            textSize = 18f
            setPadding(0, 8, 0, 8)
        }
        container.addView(titleView)

        for (item in items.ifEmpty { listOf("無") }) {
            val block = TextView(this).apply {
                text = item
                setPadding(24, 12, 24, 12)
                textSize = 16f
                setBackgroundResource(R.drawable.block_background)
                setTextColor(resources.getColor(android.R.color.black))
            }
            container.addView(block)
        }
    }

    private fun addSectionWithRatings(title: String, items: List<String>, ratings: Map<String, Int>, container: LinearLayout = resultContainer) {
        val titleView = TextView(this).apply {
            text = title
            textSize = 18f
            setPadding(0, 8, 0, 8)
        }
        container.addView(titleView)

        for (item in items.ifEmpty { listOf("無") }) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
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
            for (i in 1..5) {
                val star = ImageView(this).apply {
                    setImageResource(if (i <= currentRating) R.drawable.star_filled else R.drawable.star_empty)
                    setPadding(4, 0, 4, 0)
                }
                starsLayout.addView(star)
            }

            rowLayout.addView(nameView)
            rowLayout.addView(starsLayout)
            container.addView(rowLayout)
        }
    }
}
