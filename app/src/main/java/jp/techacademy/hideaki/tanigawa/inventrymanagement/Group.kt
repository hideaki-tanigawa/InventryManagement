package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.GroupMainBinding
import java.util.*
import android.util.Base64
import androidx.appcompat.widget.SearchView
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Group:Fragment() {
    private lateinit var _binding : GroupMainBinding
    private val binding get() = _binding

    private lateinit var groupName:String
    private lateinit var memberId: String
    private var groupId: String = ""
    private var groupCount:Int = 1
    private var users:Boolean = true
    private var groupKindName = ""
    private var item: MenuItem? = null
    private var item2: MenuItem? = null
    private var invCount: Int = 0
    private lateinit var searchInventory: ArrayList<Inventry>

    private lateinit var databaseReference: DatabaseReference
    private lateinit var groupArrayList: ArrayList<GroupList>
    private lateinit var adapter: GroupListAdapter
    private lateinit var groupListRef: DatabaseReference

    private lateinit var groupInventryArrayList: ArrayList<Inventry>
    private lateinit var invAdapter: InventryListAdapter
    private lateinit var groupInventryListRef: DatabaseReference

    private val eventListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val groupValue = snapshot.value.toString()
            if(!groupValue.equals("person")){
                val groupID = snapshot.key as? String?: ""
                val groupKindName = snapshot.value as? String?: ""
                databaseReference = FirebaseDatabase.getInstance().reference
                groupListRef = databaseReference.child(InventriesPATH).child(groupID)
                groupListRef.addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(datasnapshot: DataSnapshot) {
                        invCount = 1
                        displayTextView(invCount)
                        val map = datasnapshot.value as Map<*,*>
                        val groupName = map["groupName"] as? String ?: ""

                        var memberCount = 0
                        val memberMap = map["member"] as Map<*,*>
                        for(key in memberMap.keys) {
                            memberCount++
                        }

                        val groupList = GroupList(
                            groupID, groupName, groupKindName, memberCount
                        )
                        groupArrayList.add(groupList)
                        adapter.notifyDataSetChanged()

                        groupCount++
                    }
                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })

            }
        }
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = GroupMainBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayTextView(invCount)

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        // fabが押された時の処理
        binding.groupFab.setOnClickListener {
            if (users) {
                val intent = Intent(context, LoginActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(context, InventryAdd::class.java)
                intent.putExtra("groupIdKind", groupKindName)
                startActivity(intent)
            }
        }

        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_group_create, menu)
                item = menu.findItem(R.id.action_group_create)
                item2 = menu.findItem(R.id.action_group_member)

                item2!!.isVisible = false

                val drawble = item!!.icon
                val drawble2 = item2!!.icon
                drawble!!.colorFilter = BlendModeColorFilterCompat
                    .createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP)
                drawble2!!.colorFilter = BlendModeColorFilterCompat
                    .createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP)
                item!!.setIcon(drawble)
                item2!!.setIcon(drawble2)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_group_create -> {
                        groupName = ""
                        val dialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_group_create,null)
                        val groupNameEdit = dialogLayout.findViewById<AppCompatEditText>(R.id.groupNameCreateEdit)
                        // ダイアログを表示する
                        val builder = AlertDialog.Builder(context)

                        builder.setTitle("グループ作成")
                        builder.setMessage("グループ名を入力して下さい")
                        builder.setView(dialogLayout)
                        builder.setPositiveButton("作成") { _, _ ->
                            if(!groupName.equals("")){
                                var uuid = UUID.randomUUID().toString()
                                uuid = uuid.replace("-", "")
                                groupInfoAdd(uuid, groupName)
                            }
                        }
                        builder.setNegativeButton("キャンセル",null)
                        val dialog = builder.create()
                        dialog.show()
                        groupNameEdit.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(
                                p0: CharSequence?,
                                p1: Int,
                                p2: Int,
                                p3: Int
                            ) {}
                            override fun onTextChanged(
                                p0: CharSequence?,
                                p1: Int,
                                p2: Int,
                                p3: Int
                            ) {}
                            override fun afterTextChanged(p0: Editable?) {
                                groupName = p0.toString()
                            }
                        })
                    }

                    R.id.action_group_member -> {
                        memberId = ""
                        val dialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_group_member_invite,null)
                        val memberIdInviteEdit = dialogLayout.findViewById<AppCompatEditText>(R.id.memberIdInviteEdit)
                        // ダイアログを表示する
                        val builder = AlertDialog.Builder(context)

                        builder.setTitle("メンバー招待")
                        builder.setMessage("メンバーのIDを入力して下さい")
                        builder.setView(dialogLayout)
                        builder.setPositiveButton("招待") { _, _ ->
                            if(!memberId.equals("")){
                                var uuid = UUID.randomUUID().toString()
                                uuid = uuid.replace("-", "")
                                groupMemberInvite(memberId, groupId)
                            }
                        }
                        builder.setNegativeButton("キャンセル",null)
                        val dialog = builder.create()
                        dialog.show()
                        memberIdInviteEdit.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(
                                p0: CharSequence?,
                                p1: Int,
                                p2: Int,
                                p3: Int
                            ) {}
                            override fun onTextChanged(
                                p0: CharSequence?,
                                p1: Int,
                                p2: Int,
                                p3: Int
                            ) {}
                            override fun afterTextChanged(p0: Editable?) {
                                memberId = p0.toString()
                            }
                        })
                    }
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // ListViewの準備
        adapter = GroupListAdapter(requireContext())
        binding.groupListView.adapter = adapter
        groupArrayList = ArrayList()
        adapter.setGroupArrayList(groupArrayList)
        adapter.notifyDataSetChanged()

        // グループ在庫リストのListViewの準備
        invAdapter = InventryListAdapter(requireContext())
        binding.groupListView.adapter = invAdapter
        groupInventryArrayList = ArrayList()
        invAdapter.setInventryArrayList(groupInventryArrayList)
        invAdapter.notifyDataSetChanged()

        binding.groupInventrySearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d("確認",query!!)
                refinedSearch(query!!)
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
        binding.groupFab.visibility = View.GONE

        users = getLoginBoolean()

        groupArrayList.clear()
        groupInventryArrayList.clear()
        adapter.setGroupArrayList(groupArrayList)
        binding.groupListView.adapter = adapter

        if(!users){
            val userID = FirebaseAuth.getInstance().currentUser!!.uid
            val userRef = databaseReference.child(UsersPATH).child(userID).child("groupID")
            userRef.addChildEventListener(eventListener)
        }

        // グループリストがタップされた時に発火
        binding.groupListView.setOnItemClickListener{ _, _, position, _ ->
            invCount = 0
            displayTextView(invCount)
            binding.groupFab.visibility = View.VISIBLE
            requireActivity().setTitle("在庫一覧(グループ)")

            item!!.isVisible = false
            item2!!.isVisible = true

            groupId = groupArrayList[position].groupId
            groupKindName = groupArrayList[position].groupKindName
            groupInventryListRef = databaseReference.child(InventriesPATH).child(groupId)
            groupInventryArrayList.clear()
            invAdapter.setInventryArrayList(groupInventryArrayList)
            binding.groupListView.adapter = invAdapter
            groupInventryListRef.addChildEventListener(groupInventryeventListener)
        }
    }

    /**
     * 作成したグループをDBに登録する処理
     */
    private fun groupInfoAdd(uuid: String, groupName:String){
        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference
        val userID = FirebaseAuth.getInstance().currentUser!!.uid

        val groupRef = databaseReference.child(UsersPATH).child(userID).child("groupID").child(uuid)
        val invGroupRef = databaseReference.child(InventriesPATH).child(uuid)
        val invGroupNameRef = databaseReference.child(InventriesPATH).child(uuid).child("member")

        val invAddData = HashMap<String, String>()
        val groupNameAddData = HashMap<String, String>()

        groupRef.setValue("group" + groupCount.toString())

        groupNameAddData["groupName"] = groupName
        invGroupRef.setValue(groupNameAddData)

        invAddData["master"] = userID
        invGroupNameRef.setValue(invAddData)
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

    /**
     * グループ一覧画面の在庫一覧を表示させる処理
     */
    private val groupInventryeventListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            if(!snapshot.key.toString().equals("groupName") && !snapshot.key.toString().equals("member")){
                invCount = 1
                displayTextView(invCount)
                val map = snapshot.value as Map<*,*>
                Log.d("在庫情報",snapshot.value.toString())
                val commodity = map["commodity"] as? String ?: ""
                val count = map["count"].toString()
                val uid = map["uid"] as? String ?: ""
                val date = map["date"] as? String ?: ""
                val genre = map["genre"] as? String ?: ""
                val notice = map["notice"] as? String ?: ""
                val place = map["place"] as? String ?: ""
                val price = map["price"] as? String ?: ""
                val imageString = map["image"] as? String ?: ""
                Log.d("COUNt",count)
                val bytes =
                    if (imageString.isNotEmpty()) {
                        Base64.decode(imageString, Base64.DEFAULT)
                    } else {
                        byteArrayOf()
                    }

                val inventry = Inventry(
                    commodity, price, count, uid, snapshot.key ?: "",
                    genre, place, date, notice, groupId, bytes
                )
                groupInventryArrayList.add(inventry)
                invAdapter.notifyDataSetChanged()

                binding.groupListView.setOnItemClickListener{ parent, _, position, _ ->
                    // Inventryのインスタンスを渡して質問詳細画面を起動する
                    try {
                        val intent = Intent(context, InventryAdd::class.java)
                        intent.putExtra("inventry", groupInventryArrayList[position])
                        intent.putExtra("groupIdKind", groupKindName)
                        startActivity(intent)
                    }catch (e: java.lang.IndexOutOfBoundsException){}
                }
            }
        }
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }

    /**
     * グループのメンバーに招待する処理
     */
    private fun groupMemberInvite(receiveId: String, groupID: String){
        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference
        val userID = FirebaseAuth.getInstance().currentUser!!.uid

        val memberInviteRef = databaseReference.child("invite").child(receiveId).child(groupID)

        memberInviteRef.setValue(userID)
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
        searchInventory = groupInventryArrayList.clone() as ArrayList<Inventry>

        groupInventryArrayList.clear()
        // 在庫のリストをクinventryArrayList.clear()
        invAdapter.setInventryArrayList(groupInventryArrayList)
        binding.groupListView.adapter = invAdapter

        Log.d("配列数", searchInventory.size.toString())
        if(searchInventory.size == 1){
            if(searchInventory[0].commodity.equals(query)){
                Log.d("在庫名",query)
                groupInventryArrayList.add(searchInventory[0])
                invAdapter.notifyDataSetChanged()
            }else if(searchInventory[0].genre.equals(query)){
                groupInventryArrayList.add(searchInventory[0])
                invAdapter.notifyDataSetChanged()
            }else if(searchInventory[0].place.equals(query)){
                groupInventryArrayList.add(searchInventory[0])
                invAdapter.notifyDataSetChanged()
            }
        }else{
            for(count in 0..searchInventory.size - 1){
                if(searchInventory[count].commodity.equals(query)){
                    groupInventryArrayList.add(searchInventory[count])
                    invAdapter.notifyDataSetChanged()
                }else if(searchInventory[count].genre.equals(query)){
                    groupInventryArrayList.add(searchInventory[count])
                    invAdapter.notifyDataSetChanged()
                }else if(searchInventory[count].place.equals(query)){
                    groupInventryArrayList.add(searchInventory[count])
                    invAdapter.notifyDataSetChanged()
                }
            }
        }

//        searchCount++
    }
}