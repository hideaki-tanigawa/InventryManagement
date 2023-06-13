package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.firebase.auth.FirebaseAuth
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ContentMainBinding
import android.util.Base64
import android.util.Log
import com.google.firebase.database.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class Person : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var _binding : ContentMainBinding
    private val binding get() = _binding!!
    private val groupExit:Int = 0
    private lateinit var databaseReference: DatabaseReference
    private lateinit var inventryArrayList: ArrayList<Inventry>
    private lateinit var adapter: InventryListAdapter
    private var users:Boolean = true

    private var invRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

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

        binding.fab.setOnClickListener {
            if (users) {
                val intent = Intent(context, LoginActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(context, InventryAdd::class.java)
                intent.putExtra("groupIdKind","person")
                startActivity(intent)
            }
        }

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference
    }

    override fun onResume() {
        super.onResume()
        users = getLoginBoolean()

        // ListViewの準備
        adapter = InventryListAdapter(requireContext())
        inventryArrayList = ArrayList()
        adapter.notifyDataSetChanged()

        if(!users){
            val userID = FirebaseAuth.getInstance().currentUser!!.uid
            val userRef = databaseReference.child(UsersPATH).child(userID)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.value as Map<*, *>?
                    val data2 = data!!.get("groupID") as Map<*,*>
                    for(key in data2.keys){
                        val kindName = data2[key] as? String?: ""
                        if(kindName.equals("person")){
                            displayInventryListInfo(key.toString())
                        }
                    }
                }

                override fun onCancelled(firebaseError: DatabaseError) {}
            })
        }
    }

    /**
     * ユーザーがログインしているかどうかを判定する関数
     */
    private fun getLoginBoolean():Boolean{
        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser
        // ログイン済み：False、ログインしていない：True
        if(user == null){
            return true
        }else{
            return false
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Home.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Person().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    /**
     * Firebaseから在庫情報を取得し、ArrayListに追加する処理
     */
    private val eventListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            if(!snapshot.key.toString().equals("member")){
                val map = snapshot.value as Map<*,*>
                val commodity = map["commodity"] as? String ?: ""
                val count = map["count"] as? String ?: ""
                val uid = map["uid"] as? String ?: ""
                val date = map["date"] as? String ?: ""
                val genre = map["genre"] as? String ?: ""
                val notice = map["notice"] as? String ?: ""
                val place = map["place"] as? String ?: ""
                val price = map["price"] as? String ?: ""
                val imageString = map["image"] as? String ?: ""
                val bytes =
                    if (imageString.isNotEmpty()) {
                        Base64.decode(imageString, Base64.DEFAULT)
                    } else {
                        byteArrayOf()
                    }

                val inventry = Inventry(
                    commodity, price, count, uid, snapshot.key ?: "",
                    genre, place, date, notice, bytes
                )
                inventryArrayList.add(inventry)
                adapter.notifyDataSetChanged()

                binding.listView.setOnItemClickListener{ parent, _, position, _ ->
                    // Inventryのインスタンスを渡して質問詳細画面を起動する
                    val intent = Intent(context, InventryAdd::class.java)
                    intent.putExtra("inventry", inventryArrayList[position])
                    intent.putExtra("groupIdKind","person")
                    startActivity(intent)
                }

                // ListViewを長押しした時の処理
                binding.listView.setOnItemLongClickListener{parent, _, position, _ ->
                    // 在庫品を削除する
                    val inventry = parent.adapter.getItem(position) as Inventry

                    // ダイアログを表示する
                    val builder = AlertDialog.Builder(context)

                    builder.setTitle("削除")
                    builder.setMessage(commodity + "を削除しますか")
                    builder.setPositiveButton("OK") {_, _ ->
                        val userID = FirebaseAuth.getInstance().currentUser!!.uid
                        val userRef = databaseReference.child(UsersPATH).child(userID)
                        val inventryId = inventryArrayList[position].inventryUid
                        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val data = snapshot.value as Map<*, *>?
                                val data2 = data!!["groupID"] as Map<*,*>
                                for(key in data2.keys){
                                    val kindName = data2[key] as? String?: ""
                                    if(kindName.equals("person")){
                                        deleteInventryListInfo(key.toString(), inventryId)
                                    }
                                }
                            }

                            override fun onCancelled(firebaseError: DatabaseError) {}
                        })
                    }

                    builder.setNegativeButton("キャンセル", null)

                    val dialog = builder.create()
                    dialog.show()
                    true
                }
            }
        }
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }

    /**
     * 在庫リストを表示させるLisnerに送る処理
     */
    private fun displayInventryListInfo(groupID:String){
        inventryArrayList.clear()
        // 在庫のリストをクinventryArrayList.clear()
        adapter.setInventryArrayList(inventryArrayList)
        binding.listView.adapter = adapter
        // 選択したリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        // ジャンルにリスナーを登録する
        if (invRef != null) {
            invRef!!.removeEventListener(eventListener)
        }
        invRef = databaseReference.child(InventriesPATH).child(groupID)
        invRef!!.addChildEventListener(eventListener)
    }

    /**
     * Firebaseから在庫データを削除する処理
     */
    private fun deleteInventryListInfo(groupID: String, inventryID:String){
        invRef = databaseReference.child(InventriesPATH).child(groupID).child(inventryID)
        invRef!!.removeValue()
        inventryArrayList.clear()
        adapter.setInventryArrayList(inventryArrayList)
        binding.listView.adapter = adapter
        invRef = databaseReference.child(InventriesPATH).child(groupID)
        invRef!!.addChildEventListener(eventListener)
    }
}