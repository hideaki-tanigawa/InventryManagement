package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.app.AlertDialog
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
    private val binding get() = _binding!!
    private lateinit var groupName:String
    private lateinit var databaseReference: DatabaseReference
    private var groupCount:Int = 0
//    private lateinit var groupArrayList: ArrayList<Inventry>

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
                                val userID = FirebaseAuth.getInstance().currentUser!!.uid
                                val groupRef = databaseReference.child(UsersPATH).child(userID).child("groupID")
                                groupRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val data = snapshot.value as Map<*, *>?
                                        val personGroup = data!!["person"].toString()
                                        groupInfoAdd(personGroup, uuid, groupName)
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                            }else{
                                Log.d("TEST",groupName)
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
    }

    override fun onResume() {
        super.onResume()

//        users = getLoginBoolean()
//
//        // ListViewの準備
//        adapter = InventryListAdapter(requireContext())
//        inventryArrayList = ArrayList()
//        adapter.notifyDataSetChanged()
//
//        if(!users){
//            val userID = FirebaseAuth.getInstance().currentUser!!.uid
//            val userRef = databaseReference.child(UsersPATH).child(userID)
//            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val data = snapshot.value as Map<*, *>?
//                    val data2 = data!!["groupID"] as Map<*,*>
//                    displayInventryListInfo(data2["person"].toString())
//                }
//
//                override fun onCancelled(firebaseError: DatabaseError) {}
//            })
//        }
    }

    /**
     * 作成したグループをDBに登録する処理
     */
    private fun groupInfoAdd(personGroup:String, uuid: String, groupName:String){
        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference
        val userID = FirebaseAuth.getInstance().currentUser!!.uid

        val groupRef = databaseReference.child(UsersPATH).child(userID).child("groupID")
        val invGroupRef = databaseReference.child(InventriesPATH).child(uuid)
        val invGroupNameRef = databaseReference.child(InventriesPATH).child(uuid).child("member")

        val addData = HashMap<String, String>()
        val invAddData = HashMap<String, String>()
        val groupNameAddData = HashMap<String, String>()

        addData["person"] = personGroup
        addData["group" + groupCount.toString()] = uuid
        groupRef.setValue(addData)

        groupNameAddData["groupName"] = groupName
        invGroupRef.setValue(groupNameAddData)

        invAddData["gest" + groupCount] = userID
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