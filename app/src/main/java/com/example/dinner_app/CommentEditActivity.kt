package com.example.dinner_app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class CommentEditActivity : AppCompatActivity() {
    private lateinit var commentEditText: EditText
    private lateinit var location: String   // 新增地點
    private lateinit var foodName: String
    private lateinit var database: DatabaseReference
    private lateinit var uid: String
    private var originalComment: String = "" // 🔥 原本載入的內容，來比較有沒有修改

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment_edit)

        commentEditText = findViewById(R.id.commentEditText)
        val saveButton: Button = findViewById(R.id.saveCommentButton)
        val backButton: Button = findViewById(R.id.backButton)  // 🔥 新增返回按鈕

        foodName = intent.getStringExtra("foodName") ?: ""
        location = intent.getStringExtra("location") ?: ""   // 取得地點
        title = "編輯評論：$foodName"

        database = FirebaseDatabase.getInstance().reference
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // 🌟 組合完整的 foodName (含地點)
        val fullFoodName = "$foodName($location)"

        database.child("users").child(uid).child("preferences").child("comment").child(fullFoodName)
            .get().addOnSuccessListener {
                originalComment = it.getValue(String::class.java) ?: ""
                commentEditText.setText(it.getValue(String::class.java) ?: "")
            }

        saveButton.setOnClickListener {
            saveComment(fullFoodName){
                finish() // 儲存成功後，直接關掉回到上一頁
            }
        }

        backButton.setOnClickListener {
            checkBeforeExit(fullFoodName)
        }
    }

    private fun saveComment(fullFoodName: String, onSuccess: (() -> Unit)? = null) {
        val comment = commentEditText.text.toString().trim()

        if (comment.isEmpty()) {
            Toast.makeText(this, "請輸入評論內容再儲存哦！", Toast.LENGTH_SHORT).show()
            return
        }

        database.child("users").child(uid)
            .child("preferences").child("comment").child(fullFoodName)
            .setValue(comment)

        val publicCommentRef = database.child("public_data")
            .child(location)
            .child(foodName)
            .child("comment")
            .child(uid)
        publicCommentRef.setValue(comment)

        Toast.makeText(this, "評論已儲存", Toast.LENGTH_SHORT).show()

        // 🌟 存完通知外面
        onSuccess?.invoke()
    }

    private fun checkBeforeExit(fullFoodName: String) {
        val currentComment = commentEditText.text.toString().trim()

        if (currentComment != originalComment && currentComment.isNotEmpty()) {
            // 🔥 有修改過而且不是空的，彈出 Dialog
            AlertDialog.Builder(this)
                .setTitle("尚未儲存")
                .setMessage("是否要儲存評論？")
                .setPositiveButton("是") { dialog, _ ->
                    saveComment(fullFoodName) {
                        // 🌟 存完才 finish！
                        finish()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("否") { dialog, _ ->
                    finish()
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        } else {
            // 沒有改過，直接返回
            finish()
        }
    }
}
