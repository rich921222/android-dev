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
    private val onDeleteClick: (String) -> Unit,  // ✅ 讓 Adapter 支援刪除 Callback
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

        // 🔧 只有在 foodList 不為空時才顯示刪除按鈕
        holder.deleteButton.visibility = if (foodList.isNotEmpty()) View.VISIBLE else View.GONE

        // 按下刪除按鈕後，從 Firebase 和本地刪除
        holder.deleteButton.setOnClickListener {
            onDeleteClick(foodItem)
        }
    }

    override fun getItemCount(): Int = foodList.size
}
