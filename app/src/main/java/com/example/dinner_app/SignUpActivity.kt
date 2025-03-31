package com.example.dinner_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()

        val emailEditText: EditText = findViewById(R.id.emailEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val foodEditText: EditText = findViewById(R.id.foodEditText)
        val allergiesEditText: EditText = findViewById(R.id.allergiesEditText)
        val signupButton: Button = findViewById(R.id.signupButton)

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val food = foodEditText.text.toString().split(",").map { it.trim() }
            val allergies = allergiesEditText.text.toString().split(",").map { it.trim() }

            if (email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(email, password, food, allergies)
            } else {
                Toast.makeText(this, "請輸入有效的 email 和密碼", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(email: String, password: String, food: List<String>, allergies: List<String>) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("SignUp", "註冊成功")
                    val user = auth.currentUser

                    user?.let {
                        // 註冊成功後將初始資料寫入 Firebase
                        val userRef = FirebaseDatabase.getInstance().getReference("users").child(it.uid)
                        val preferences = mapOf(
                            "food" to food,
                            "allergies" to allergies
                        )
                        userRef.child("preferences").setValue(preferences)
                            .addOnSuccessListener {
                                Log.d("Signup", "使用者喜好資料已更新")

                                // ✅ 儲存 email → UID 映射（這裡才安全！）
                                val emailKey = email.lowercase().replace(".", ",")
                                FirebaseDatabase.getInstance().getReference("emailToUid")
                                    .child(emailKey)
                                    .setValue(user.uid)

                                // 註冊成功後跳轉到 MainActivity
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e("Signup", "寫入資料失敗: ${e.message}")
                            }
                    }
                } else {
                    Log.e("SignUp", "註冊失敗: ${task.exception?.message}")
                    Toast.makeText(this, "註冊失敗: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
