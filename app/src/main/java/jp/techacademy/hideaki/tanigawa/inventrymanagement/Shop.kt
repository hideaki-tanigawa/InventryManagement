package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.app.AlertDialog
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
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ShopMainBinding

class Shop:Fragment() {
    private lateinit var _binding : ShopMainBinding
    private val binding get() = _binding!!

    private lateinit var databaseReference: DatabaseReference
    private lateinit var shopListArrayList: ArrayList<Inventry>
    private lateinit var adapter: ShopListAdapter
    private lateinit var shopRef: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private lateinit var invRef:DatabaseReference
    private var shopListPrice = 0

    private val groupList = arrayListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = ShopMainBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.shopFab.setOnClickListener{
            val intent = Intent(context, ShopListAddActivity::class.java)
            startActivity(intent)
        }

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        adapter = ShopListAdapter(requireContext())
        shopListArrayList = ArrayList()
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()

        shopListPrice = 0

        shopListArrayList.clear()
        adapter.setShopListArrayList(shopListArrayList)
        binding.shopListView.adapter = adapter

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
            for(list in groupList){
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
                        genre, place, date, notice, groupId, bytes
                    )
                    if(!price.equals("")){
                        val mathPrice = price.toInt()
                        shopListPrice += mathPrice

                        binding.inventryAllPriceText.setText("合計金額："+shopListPrice+"円")

                        shopListArrayList.add(inventry)
                        adapter.notifyDataSetChanged()
                    }

                    binding.shopListView.setOnItemClickListener{parent, _, position, _ ->
                        // Inventryのインスタンスを渡して質問詳細画面を起動する
                        val intent = Intent(context, ShopListAddActivity::class.java)
                        intent.putExtra("inventry", shopListArrayList[position])
                        startActivity(intent)
                    }

                    // ListViewを長押しした時の処理
                    binding.shopListView.setOnItemLongClickListener{parent, _, position, _ ->
                        // 在庫品を削除する
                        val inventry = parent.adapter.getItem(position) as Inventry

                        // ダイアログを表示する
                        val builder = AlertDialog.Builder(context)

                        builder.setTitle("在庫登録 or 削除")
                        builder.setMessage(commodity + "を登録 or 削除しますか")
                        builder.setPositiveButton("削除") {_, _ ->
                            deleteInventryListInfo(groupId)
                        }

                        builder.setNegativeButton("登録", null)

                        builder.setNeutralButton("キャンセル",null)

                        val dialog = builder.create()
                        dialog.show()
                        true
                    }
                }
                override fun onCancelled(error: DatabaseError) {}

            })

            // リストが長押しされた際に発火

        }
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }

    /**
     * Firebaseから在庫データを削除する処理
     */
    private fun deleteInventryListInfo(groupID: String){
        invRef = databaseReference.child(ShoppingPATH).child(groupID)
        invRef!!.removeValue()
        onResume()
    }
}