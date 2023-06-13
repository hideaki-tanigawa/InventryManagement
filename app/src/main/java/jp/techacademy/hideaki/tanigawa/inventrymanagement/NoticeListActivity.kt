package jp.techacademy.hideaki.tanigawa.inventrymanagement

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ActivityNoticeListBinding

class NoticeListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoticeListBinding

    private lateinit var noticeRef: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private lateinit var groupRef: DatabaseReference
    private lateinit var databaseReference: DatabaseReference
    private lateinit var noticeArrayList: ArrayList<NoticeList>
    private lateinit var adapter: NoticeListAdapter

    /**
     * グループ招待をしてくれたグループ名と送信者のリストを表示する処理
     */
    private val eventListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val sendId = snapshot.value as? String ?: ""
            val groupId = snapshot.key.toString()

            Log.d("TEST",sendId)

            databaseReference = FirebaseDatabase.getInstance().reference
            userRef = databaseReference.child(UsersPATH).child(sendId)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.value as Map<*,*>
                    val sendName = data["name"] as? String ?: ""
                    setGropNameAdapter(sendName,groupId)
                }
                override fun onCancelled(error: DatabaseError) {}

            })
        }
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoticeListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // タイトルを通知管理画面に変更
        title = getString(R.string.menu_group_notice_label)

        // ListViewの準備
        adapter = NoticeListAdapter(this)
        binding.noticeListView.adapter = adapter
        noticeArrayList = ArrayList()
        adapter.setNoticeListArrayList(noticeArrayList)
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        noticeArrayList.clear()
        adapter.setNoticeListArrayList(noticeArrayList)
        binding.noticeListView.adapter = adapter

        databaseReference = FirebaseDatabase.getInstance().reference
        noticeRef = databaseReference.child(InvitePATH).child(userId)
        noticeRef.addChildEventListener(eventListener)
    }

    /**
     * グループ名を取得し、Adapterに送る処理
     */
    private fun setGropNameAdapter(sendName: String, groupId: String){
        Log.d("TEST",sendName)
        databaseReference = FirebaseDatabase.getInstance().reference
        groupRef = databaseReference.child(InventriesPATH).child(groupId)
        groupRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.value as Map<*,*>
                val groupName = data["groupName"] as? String ?: ""
                val noticeList = NoticeList(
                    groupName, sendName
                )
                noticeArrayList.add(noticeList)
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}

        })
    }
}