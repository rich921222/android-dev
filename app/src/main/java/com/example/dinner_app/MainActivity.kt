package com.example.dinner_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var preferencesTextView: TextView
    private var preferencesListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        preferencesTextView = findViewById(R.id.preferencesTextView)

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
            auth.signOut() // 登出 Firebase
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // 修改按鈕
        val editPreferencesButton: Button = findViewById(R.id.editPreferencesButton)
        editPreferencesButton.setOnClickListener {
            startActivity(Intent(this, EditPreferencesActivity::class.java))
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
                if (snapshot.exists()) {
                    val foodList = snapshot.child("food").children.mapNotNull { it.getValue(String::class.java) }
                    val allergyList = snapshot.child("allergies").children.mapNotNull { it.getValue(String::class.java) }

                    val foodText = if (foodList.isNotEmpty()) "🍽 喜好食物:\n" + foodList.joinToString("\n") else "🍽 喜好食物:\n無"
                    val allergyText = if (allergyList.isNotEmpty()) "🚫 過敏原:\n" + allergyList.joinToString("\n") else "🚫 過敏原:\n無"

                    preferencesTextView.text = "$foodText\n\n$allergyText"
                } else {
                    preferencesTextView.text = "尚未設定喜好資料"
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
}
