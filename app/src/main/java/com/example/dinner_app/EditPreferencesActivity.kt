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
    private lateinit var foodRatingEditText: EditText
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
        foodRatingEditText = findViewById(R.id.foodRatingEditText)
        foodRecyclerView = findViewById(R.id.foodRecyclerView)

        allergyEditText = findViewById(R.id.allergiesEditText)
        allergyRecyclerView = findViewById(R.id.allergyRecyclerView)

        // 設定 RecyclerView
        foodAdapter = FoodAdapter(foodList,{ food -> removeFood(food) })
        foodRecyclerView.layoutManager = LinearLayoutManager(this)
        foodRecyclerView.adapter = foodAdapter

        allergyAdapter = FoodAdapter(allergyList,{ allergy -> removeAllergy(allergy) })
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


    private fun savePreferences() {
        val userId = auth.currentUser?.uid ?: return

        val foodText = foodEditText.text.toString().trim()
        val ratingText = foodRatingEditText.text.toString().trim()
        val allergyText = allergyEditText.text.toString().trim()

        val ratingValue = ratingText.toIntOrNull()

        if (foodText.isEmpty()) {
            Toast.makeText(this, "請輸入食物名稱", Toast.LENGTH_SHORT).show()
            return
        }

        if (ratingValue == null || ratingValue !in 1..5) {
            Toast.makeText(this, "請輸入 1 到 5 分的評分", Toast.LENGTH_SHORT).show()
            return
        }

        val foodRef = database.child("users").child(userId).child("preferences").child("food")
        val ratingRef = database.child("users").child(userId).child("preferences").child("ratings").child(foodText)

        foodList.add(foodText)
        foodRef.setValue(foodList)
            .addOnSuccessListener {
                ratingRef.setValue(ratingValue)
                    .addOnSuccessListener {
                        // 新增：累加 public_data 的 ratings
                        val publicRatingRef = database.child("public_data").child("ratings").child(foodText)
                        publicRatingRef.runTransaction(object : Transaction.Handler {
                            override fun doTransaction(currentData: MutableData): Transaction.Result {
                                val currentValue = currentData.getValue(Int::class.java) ?: 0
                                currentData.value = currentValue + ratingValue
                                return Transaction.success(currentData)
                            }

                            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                                if (error != null) {
                                    Log.e("PublicRating", "更新 public_data 失敗: ${error.message}")
                                } else {
                                    Log.d("PublicRating", "public_data 已更新：$foodText +$ratingValue")
                                }
                            }
                        })
                        // 接著儲存 allergy（如果有輸入）
                        if (allergyText.isNotEmpty()) {
                            val allergyRef = database.child("users").child(userId).child("preferences").child("allergies")
                            allergyList.add(allergyText)
                            allergyRef.setValue(allergyList)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "已新增所有資料", Toast.LENGTH_SHORT).show()
                                    foodEditText.text.clear()
                                    foodRatingEditText.text.clear()
                                    allergyEditText.text.clear()
                                    foodAdapter.notifyDataSetChanged()
                                    allergyAdapter.notifyDataSetChanged()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "過敏原儲存失敗", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "已新增食物與評分", Toast.LENGTH_SHORT).show()
                            foodEditText.text.clear()
                            foodRatingEditText.text.clear()
                            foodAdapter.notifyDataSetChanged()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "評分儲存失敗", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "食物儲存失敗", Toast.LENGTH_SHORT).show()
            }
    }

}
