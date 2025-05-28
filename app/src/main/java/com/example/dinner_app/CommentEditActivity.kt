package com.example.dinner_app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class CommentEditActivity : AppCompatActivity() {
    private lateinit var commentEditText: EditText
    private lateinit var foodName: String
    private lateinit var database: DatabaseReference
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment_edit)

        commentEditText = findViewById(R.id.commentEditText)
        val saveButton: Button = findViewById(R.id.saveCommentButton)

        foodName = intent.getStringExtra("foodName") ?: ""
        title = "編輯評論：$foodName"

        database = FirebaseDatabase.getInstance().reference
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        database.child("users").child(uid).child("preferences").child("comment").child(foodName)
            .get().addOnSuccessListener {
                commentEditText.setText(it.getValue(String::class.java) ?: "")
            }

        saveButton.setOnClickListener {
            val comment = commentEditText.text.toString()
            // 1️⃣ 儲存使用者自己的評論
            database.child("users").child(uid)
                .child("preferences").child("comment").child(foodName)
                .setValue(comment)

            // 2️⃣ 儲存至公共評論區
            val publicCommentRef = database.child("public_data")
                .child("comment").child(foodName).child(uid)
            publicCommentRef.setValue(comment)

            Toast.makeText(this, "評論已儲存", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
