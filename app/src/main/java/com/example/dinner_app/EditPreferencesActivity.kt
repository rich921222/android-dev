package com.example.dinner_app

import FoodAdapter
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EditPreferencesActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var foodEditText: EditText
    private lateinit var foodRecyclerView: RecyclerView
    private lateinit var foodAdapter: FoodAdapter
    private val foodList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_preferences)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // 取得當前使用者的 UID
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId).child("preferences")

        foodEditText = findViewById(R.id.foodEditText)
        foodRecyclerView = findViewById(R.id.foodRecyclerView)

        // 設定 RecyclerView
        foodAdapter = FoodAdapter(foodList,userRef,database,auth) { food -> removeFood(food) }
        foodRecyclerView.layoutManager = LinearLayoutManager(this)
        foodRecyclerView.adapter = foodAdapter

        // 禁用 RecyclerView 內部滾動，避免與 NestedScrollView 衝突
        foodRecyclerView.isNestedScrollingEnabled = false

        val saveButton: Button = findViewById(R.id.saveButton)
        val cancelButton: Button = findViewById(R.id.cancelButton)

        saveButton.setOnClickListener { savePreferences() }
        cancelButton.setOnClickListener { finish() }

        loadUserPreferences()  // 讀取已儲存的食物
    }

    // 讀取使用者的偏好設定
    private fun loadUserPreferences() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId).child("preferences")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodList.clear()  // 清空舊資料
                snapshot.child("food").children.forEach {
                    it.getValue(String::class.java)?.let { food -> foodList.add(food) }
                }
                foodAdapter.notifyDataSetChanged()  // 更新 RecyclerView
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "讀取失敗: ${error.message}")
            }
        })
    }

    // 按下 ❌ 刪除某個食物
    private fun removeFood(food: String) {
        val userId = auth.currentUser?.uid ?: return
        foodList.remove(food)
        foodAdapter.notifyDataSetChanged()

        val userRef = database.child("users").child(userId).child("preferences").child("food")
        userRef.setValue(foodList)  // 更新 Firebase
            .addOnFailureListener { Toast.makeText(this, "刪除失敗", Toast.LENGTH_SHORT).show() }
    }

    // 儲存偏好設定
    private fun savePreferences() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId).child("preferences").child("food")

        // 先讀取 Firebase 最新資料，確保不會寫回被刪除的項目
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firebaseFoodList = mutableListOf<String>()

                // 讀取 Firebase 最新的 food 資料
                snapshot.children.forEach {
                    it.getValue(String::class.java)?.let { food -> firebaseFoodList.add(food) }
                }

                // **將 UI 上的輸入加入 Firebase 最新的食物清單**
                val foodText = foodEditText.text.toString().trim()
                val newFoodList = foodText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                firebaseFoodList.addAll(newFoodList)

                // **寫回 Firebase**
                userRef.setValue(firebaseFoodList)
                    .addOnSuccessListener {
                        Toast.makeText(this@EditPreferencesActivity, "偏好資料已更新！", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@EditPreferencesActivity, "更新失敗，請稍後再試", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "讀取失敗: ${error.message}")
            }
        })
    }

}
