package com.example.dinner_app
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CommentListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CommentListAdapter
    private lateinit var locationSpinner: Spinner
    private val foodList = mutableListOf<String>()
    private lateinit var database: DatabaseReference
    private lateinit var uid: String

    private val taiwanCities = listOf(
        "全部縣市",
        "台北市", "新北市", "桃園市", "台中市", "台南市", "高雄市",
        "基隆市", "新竹市", "嘉義市",
        "新竹縣", "苗栗縣", "彰化縣", "南投縣", "雲林縣", "嘉義縣",
        "屏東縣", "宜蘭縣", "花蓮縣", "台東縣",
        "澎湖縣", "金門縣", "連江縣"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment_list)

        val backButton: Button = findViewById(R.id.backToMainButton)
        backButton.setOnClickListener {
            finish() // 返回上一個 Activity（MainActivity）
        }

        locationSpinner = findViewById(R.id.locationSpinner)
        recyclerView = findViewById(R.id.commentRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CommentListAdapter(foodList) { location, foodName ->
            val intent = Intent(this, CommentEditActivity::class.java)
            intent.putExtra("location", location)   // 加地點
            intent.putExtra("foodName", foodName)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().reference
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        setupLocationSpinner()
    }
    private fun setupLocationSpinner() {
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, taiwanCities)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        locationSpinner.adapter = spinnerAdapter

        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLocation = taiwanCities[position]
                if (selectedLocation == "全部縣市") {
                    loadAllRestaurants()
                } else {
                    loadRestaurantsForLocation(selectedLocation)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    private fun loadAllRestaurants() {
        database.child("public_data")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    foodList.clear()

                    snapshot.children.forEach { locationSnapshot ->
                        val location = locationSnapshot.key ?: return@forEach
                        locationSnapshot.children.forEach { restaurantSnapshot ->
                            val foodName = restaurantSnapshot.key ?: return@forEach
                            foodList.add("$location|$foodName")
                        }
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
    private fun loadRestaurantsForLocation(location: String) {
        database.child("public_data")
            .child(location) // 只查特定縣市
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    foodList.clear()

                    snapshot.children.forEach { restaurantSnapshot ->
                        val foodName = restaurantSnapshot.key ?: return@forEach
                        foodList.add("$location|$foodName")
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
