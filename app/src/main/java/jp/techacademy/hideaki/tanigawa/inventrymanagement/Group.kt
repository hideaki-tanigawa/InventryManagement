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
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Group:Fragment() {
    private lateinit var _binding : GroupMainBinding
    private val binding get() = _binding

    private lateinit var groupName:String
    private var groupCount:Int = 1
    private var users:Boolean = true
    private var visibleView = 0

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
                val groupID = snapshot.key.toString()
                databaseReference = FirebaseDatabase.getInstance().reference
                groupListRef = databaseReference.child(InventriesPATH).child(groupID)
                groupListRef.addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(datasnapshot: DataSnapshot) {
                        val map = datasnapshot.value as Map<*,*>
                        val groupName = map["groupName"] as? String ?: ""

                        var memberCount = 0
                        val memberMap = map["member"] as Map<*,*>
                        for(key in memberMap.keys) {
                            memberCount++
                        }

                        val groupList = GroupList(
                            groupName, memberCount
                        )
                        groupArrayList.add(groupList)
                        adapter.notifyDataSetChanged()

                        groupCount++

                        // グループリストがタップされた時に発火
                        binding.groupListView.setOnItemClickListener{ _, _, position, _ ->
                            val groupInvRef = databaseReference.child(InventriesPATH).child(groupID)
                        }
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

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        if(visibleView == 0){
            // fabの表示非表示
            binding.groupFab.visibility = View.GONE
        }

        // fabが押された時の処理
        binding.groupFab.setOnClickListener {
            if (users) {
                val intent = Intent(context, LoginActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(context, InventryAdd::class.java)
                startActivity(intent)
            }
        }

        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_group_create, menu)
                val item = menu.findItem(R.id.action_group_create)

                val drawble = item.icon
                drawble!!.colorFilter = BlendModeColorFilterCompat
                    .createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP)
                item.setIcon(drawble)
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
    }

    override fun onResume() {
        super.onResume()

        users = getLoginBoolean()

        groupArrayList.clear()
        adapter.setGroupArrayList(groupArrayList)
        binding.groupListView.adapter = adapter

        if(!users){
            val userID = FirebaseAuth.getInstance().currentUser!!.uid
            val userRef = databaseReference.child(UsersPATH).child(userID).child("groupID")
            userRef.addChildEventListener(eventListener)
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
}