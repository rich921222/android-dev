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

    private lateinit var allergyEditText: EditText
    private lateinit var allergyRecyclerView: RecyclerView
    private lateinit var allergyAdapter: FoodAdapter
    private val allergyList = mutableListOf<String>()


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

        allergyEditText = findViewById(R.id.allergiesEditText)
        allergyRecyclerView = findViewById(R.id.allergyRecyclerView)

        // 設定 RecyclerView
        foodAdapter = FoodAdapter(foodList,userRef,database,auth,{ food -> removeFood(food) },"food")
        foodRecyclerView.layoutManager = LinearLayoutManager(this)
        foodRecyclerView.adapter = foodAdapter

        allergyAdapter = FoodAdapter(allergyList, userRef, database, auth,{ allergy -> removeAllergy(allergy) }, "allergies")
        allergyRecyclerView.layoutManager = LinearLayoutManager(this)
        allergyRecyclerView.adapter = allergyAdapter


        // 禁用 RecyclerView 內部滾動，避免與 NestedScrollView 衝突
        foodRecyclerView.isNestedScrollingEnabled = false
        allergyRecyclerView.isNestedScrollingEnabled = false

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
                allergyList.clear()
                snapshot.child("food").children.forEach {
                    it.getValue(String::class.java)?.let { food -> foodList.add(food) }
                }
                snapshot.child("allergies").children.forEach {
                    it.getValue(String::class.java)?.let { allergy -> allergyList.add(allergy) }
                }
                foodAdapter.notifyDataSetChanged()  // 更新 RecyclerView
                allergyAdapter.notifyDataSetChanged()
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

        val foodRef = database.child("users").child(userId).child("preferences").child("food")
        val ratingRef = database.child("users").child(userId).child("preferences").child("ratings").child(food)

        // 刪除 ratings 中對應的評價（如果有）
        ratingRef.removeValue()

        // 同時更新 food 清單
        foodRef.setValue(foodList)
            .addOnFailureListener {
                Toast.makeText(this, "刪除失敗", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeAllergy(allergy: String) {
        val userId = auth.currentUser?.uid ?: return
        allergyList.remove(allergy)
        allergyAdapter.notifyDataSetChanged()

        val allergyRef = database.child("users").child(userId).child("preferences").child("allergies")

        allergyRef.setValue(allergyList)
            .addOnFailureListener {
                Toast.makeText(this, "刪除失敗", Toast.LENGTH_SHORT).show()
            }
    }


    // 儲存偏好設定
    private fun savePreferences() {
        val userId = auth.currentUser?.uid ?: return

        val foodRef = database.child("users").child(userId).child("preferences").child("food")
        val allergyRef = database.child("users").child(userId).child("preferences").child("allergies")

        // Step 1: 儲存 Food
        foodRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firebaseFoodList = mutableListOf<String>()

                snapshot.children.forEach {
                    it.getValue(String::class.java)?.let { food -> firebaseFoodList.add(food) }
                }

                // 解析 EditText 的新食物
                val foodText = foodEditText.text.toString().trim()
                val newFoodList = foodText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                // 合併
                firebaseFoodList.addAll(newFoodList)

                foodRef.setValue(firebaseFoodList)
                    .addOnSuccessListener {
                        // Step 2: 儲存 Allergies
                        allergyRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val firebaseAllergyList = mutableListOf<String>()

                                snapshot.children.forEach {
                                    it.getValue(String::class.java)?.let { allergy -> firebaseAllergyList.add(allergy) }
                                }

                                val allergyText = allergyEditText.text.toString().trim()
                                val newAllergyList = allergyText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                                firebaseAllergyList.addAll(newAllergyList)

                                allergyRef.setValue(firebaseAllergyList)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@EditPreferencesActivity, "偏好資料已更新！", Toast.LENGTH_SHORT).show()
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@EditPreferencesActivity, "過敏原儲存失敗", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("Firebase", "讀取過敏原失敗: ${error.message}")
                            }
                        })
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@EditPreferencesActivity, "食物儲存失敗", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "讀取食物失敗: ${error.message}")
            }
        })
    }

}
