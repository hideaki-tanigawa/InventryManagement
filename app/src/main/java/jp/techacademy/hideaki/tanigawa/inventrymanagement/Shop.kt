package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ShopMainBinding

class Shop:Fragment() {
    private lateinit var _binding : ShopMainBinding
    private val binding get() = _binding!!

    private lateinit var databaseReference: DatabaseReference
    private lateinit var shopListArrayList: ArrayList<ShopInventory>
    private lateinit var searchInventory: ArrayList<ShopInventory>
    private lateinit var adapter: ShopListAdapter
    private lateinit var shopRef: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private lateinit var invRef:DatabaseReference
    private var shopListPrice = 0
    private var roopCount = 0
    private var invCount: Int = 0
    private var strQuery: String = ""

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

        binding.groupInventrySearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                strQuery = query!!
//                searchCount = 1
                refinedSearch(strQuery)
//                searchCount = 0
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        searchInventory = ArrayList()
    }

    override fun onResume() {
        super.onResume()
        invCount = 0
        displayTextView(invCount)

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
                    val str = map.keys.toString()
                    val newStr = str.removePrefix("[")
                    val dobleStr = newStr.removeSuffix("]")
                    val inventryIdList = dobleStr.split(", ")
                    inventryId = inventryIdList[0]
                    val invMap = map[inventryId] as Map<*,*>
                    for(list2 in invMap.keys){
                        if(list2!!.equals("buyPrice")){
                            price = invMap["buyPrice"] as? String ?: ""
                        }else{
                            count = invMap["buyCount"] as? String ?: ""
                        }
                    }
                }
            }

            val invRef = databaseReference.child(InventriesPATH).child(groupId).child(inventryId)
            invRef.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    invCount = 1
                    displayTextView(invCount)
                    val invMap = snapshot.value as Map<*,*>
                    val commodity = invMap["commodity"] as? String ?: ""
                    val uid = invMap["uid"] as? String ?: ""
                    val date = invMap["date"] as? String ?: ""
                    val addCount = invMap["count"] as? String ?: ""
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
                    val inventry = ShopInventory(
                        commodity, price, count, uid, inventryId,
                        genre, place, date, notice, groupId, addCount, bytes
                    )
                    if(!price.equals("")){
                        val mathPrice = price.toInt()
                        shopListPrice += mathPrice

                        binding.inventryAllPriceText.setText("合計金額："+shopListPrice+"円")

                        roopCount++
                        shopListArrayList.add(inventry)
                        adapter.notifyDataSetChanged()
                    }

                    binding.shopListView.setOnItemClickListener{parent, _, position, _ ->
                        // Inventryのインスタンスを渡して質問詳細画面を起動する
                        Log.d("NNNNNNN",shopListArrayList[position].commodity)
                        val intent = Intent(context, ShopListAddActivity::class.java)
                        intent.putExtra("inventry", shopListArrayList[position])
                        startActivity(intent)
                    }

                    // ListViewを長押しした時の処理
                    binding.shopListView.setOnItemLongClickListener{parent, _, position, _ ->
                        // 在庫品を削除する
                        val inventry = parent.adapter.getItem(position) as ShopInventory

                        // ダイアログを表示する
                        val builder = AlertDialog.Builder(context)

                        builder.setTitle("在庫登録 or 削除")
                        builder.setMessage(commodity + "を登録 or 削除しますか")
                        builder.setPositiveButton("削除") {_, _ ->
                            deleteInventryListInfo(groupId)
                        }

                        builder.setNegativeButton("登録"){_, _ ->
                            registerInventryListInfo(groupId, inventry)
                        }

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

    private fun registerInventryListInfo(groupID: String, inventry: ShopInventory){
        Log.d("aaa","qwertyuiop")
        shopRef = databaseReference.child(ShoppingPATH).child(groupID)
        invRef = databaseReference.child(InventriesPATH).child(groupID).child(inventry.inventryUid)

        var addCount = inventry.addCount
        var pulsCount = 0
        if(addCount != null){
            pulsCount = addCount.toInt() + inventry.count.toInt()
        }

        shopRef.removeValue()

        val map = HashMap<String,Any>()
        map["count"] = pulsCount
        map["price"] = inventry.price
        map["shopBoolean"] = "0"

        invRef.updateChildren(map).addOnSuccessListener {
            onResume()
        }
    }

    /**
     * 在庫が登録されているかによってTextViewを表示させるか
     * 判断する処理
     * @param invCount 在庫数
     */
    private fun displayTextView(invCount: Int){
        if(invCount == 0){
            binding.noInventoryListText.visibility = View.VISIBLE
        }else{
            binding.noInventoryListText.visibility = View.GONE
        }
    }

    /**
     * 在庫リストを絞り込む処理
     * @param query 検索ワード
     */
    private fun refinedSearch(query: String){
        // キーボードが出てたら閉じる
        val im = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(view?.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)
        
        searchInventory = shopListArrayList.clone() as ArrayList<ShopInventory>

        shopListArrayList.clear()
        // 在庫のリストをクinventryArrayList.clear()
        adapter.setShopListArrayList(shopListArrayList)
        binding.shopListView.adapter = adapter

        Log.d("配列数", searchInventory.size.toString())
        if(searchInventory.size == 1){
            if(searchInventory[0].commodity.equals(query)){
                Log.d("在庫名",query)
                shopListArrayList.add(searchInventory[0])
                adapter.notifyDataSetChanged()
            }else if(searchInventory[0].genre.equals(query)){
                shopListArrayList.add(searchInventory[0])
                adapter.notifyDataSetChanged()
            }else if(searchInventory[0].place.equals(query)){
                shopListArrayList.add(searchInventory[0])
                adapter.notifyDataSetChanged()
            }
        }else{
            for(count in 0..searchInventory.size - 1){
                if(searchInventory[count].commodity.equals(query)){
                    shopListArrayList.add(searchInventory[count])
                    adapter.notifyDataSetChanged()
                }else if(searchInventory[count].genre.equals(query)){
                    shopListArrayList.add(searchInventory[count])
                    adapter.notifyDataSetChanged()
                }else if(searchInventory[count].place.equals(query)){
                    shopListArrayList.add(searchInventory[count])
                    adapter.notifyDataSetChanged()
                }
            }
        }

//        searchCount++
    }
}