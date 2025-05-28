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
        val signupButton: Button = findViewById(R.id.signupButton)

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(email, password)
            } else {
                Toast.makeText(this, "請輸入有效的 email 和密碼", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        // 初始化資料庫位置，只記錄基本節點
                        val userRef = FirebaseDatabase.getInstance().getReference("users").child(it.uid)
                        userRef.child("preferences").setValue(mapOf<String, Any>()) // 空 preferences，可選

                        // 建立 email → uid 映射
                        val emailKey = email.lowercase().replace(".", ",")
                        FirebaseDatabase.getInstance().getReference("emailToUid")
                            .child(emailKey)
                            .setValue(user.uid)

                        // 跳轉
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(this, "註冊失敗: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
