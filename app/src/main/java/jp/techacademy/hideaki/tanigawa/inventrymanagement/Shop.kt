package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ContentMainBinding

class Shop:Fragment() {
    private lateinit var _binding : ContentMainBinding
    private val binding get() = _binding!!

    private lateinit var databaseReference: DatabaseReference
    private lateinit var shopListArrayList: ArrayList<Inventry>
    private lateinit var adapter: InventryListAdapter
    private lateinit var shopRef: DatabaseReference
    private lateinit var userRef: DatabaseReference

    private val groupList = arrayListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = ContentMainBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fab.setOnClickListener{
            val intent = Intent(context, InventryAdd::class.java)
            intent.putExtra("shopList",true)
            startActivity(intent)
        }

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        adapter = InventryListAdapter(requireContext())
        shopListArrayList = ArrayList()
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()

        shopListArrayList.clear()
        adapter.setInventryArrayList(shopListArrayList)
        binding.listView.adapter = adapter

        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        userRef = databaseReference.child(UsersPATH).child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.value as Map<*,*>
                val userData = data["groupID"] as Map<*,*>

                for (key in userData.keys){
                    groupList.add(key.toString())
                }
                shopRef = databaseReference.child(ShoppingPATH)
                shopRef.addChildEventListener(eventListener)
            }
            override fun onCancelled(error: DatabaseError) {}

        })
    }

    /**
     * 買い物一覧を表示する処理
     */
    private var eventListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val groupId = snapshot.key.toString()
            var inventryId = ""
            var price = ""
            var count = ""
            Log.d("Test",snapshot.value.toString())
            for(list in groupList){
                Log.d("Group-Test",list)
                if(groupId.equals(list)){
                    val map = snapshot.value as Map<*,*>
                    inventryId = map["inventryId"] as? String ?: ""
                    price = map["buyPrice"] as? String ?: ""
                    count = map["buyCount"] as? String ?: ""
                }
            }

            val invRef = databaseReference.child(InventriesPATH).child(groupId).child(inventryId)
            invRef.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val invMap = snapshot.value as Map<*,*>
                    val commodity = invMap["commodity"] as? String ?: ""
                    val uid = invMap["uid"] as? String ?: ""
                    val date = invMap["date"] as? String ?: ""
                    val genre = invMap["genre"] as? String ?: ""
                    val notice = invMap["notice"] as? String ?: ""
                    val place = invMap["place"] as? String ?: ""
                    val imageString = invMap["image"] as? String ?: ""
                    val bytes =
                        if (imageString.isNotEmpty()) {
                            Base64.decode(imageString, Base64.DEFAULT)
                        } else {
                            byteArrayOf()
                        }
                    val inventry = Inventry(
                        commodity, price, count, uid, inventryId,
                        genre, place, date, notice, bytes
                    )
                    shopListArrayList.add(inventry)
                    adapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}

            })
        }
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }
}