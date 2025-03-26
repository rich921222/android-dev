import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dinner_app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class FoodAdapter(
    private var foodList: MutableList<String>,
    private val userRef: DatabaseReference,  // ✅ 傳入 Firebase DatabaseReference
    private val database: DatabaseReference,  // Firebase DatabaseReference
    private val auth: FirebaseAuth,  // 傳入 FirebaseAuth 實例
    private val onDeleteClick: (String) -> Unit  // ✅ 讓 Adapter 支援刪除 Callback
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodTextView: TextView = itemView.findViewById(R.id.foodTextView)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.food_item, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val foodItem = foodList[position]
        holder.foodTextView.text = foodItem

        // 按下刪除按鈕後，從 Firebase 和本地刪除
        holder.deleteButton.setOnClickListener {
            removeFood(position)
        }
    }

    override fun getItemCount(): Int = foodList.size

    private fun removeFood(position: Int) {
        val foodToRemove = foodList[position]  // 取得要刪除的食物
        val updatedList = foodList.toMutableList()
        updatedList.removeAt(position)  // 從本地列表中刪除

        // 使用 Firebase 中的 orderByValue 查找要刪除的食物
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId).child("preferences").child("food")

        // 查找並刪除食物
        userRef.orderByValue().equalTo(foodToRemove).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // 刪除 Firebase 資料庫中的食物項目
                    for (child in snapshot.children) {
                        child.ref.removeValue().addOnCompleteListener {
                            // 成功刪除後，更新本地列表並刷新 RecyclerView
                            foodList = updatedList
                            notifyItemRemoved(position)
                            notifyItemRangeChanged(position, foodList.size)
                        }.addOnFailureListener {
                            Log.e("Firebase", "刪除 Firebase 資料時發生錯誤: ${it.message}")
                        }
                    }
                } else {
                    Log.e("Firebase", "未找到該食物項目：$foodToRemove")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "查詢 Firebase 時發生錯誤: ${error.message}")
            }
        })
    }

}
