package jp.techacademy.hideaki.tanigawa.inventrymanagement

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
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

            databaseReference = FirebaseDatabase.getInstance().reference
            userRef = databaseReference.child(UsersPATH).child(sendId)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.value as Map<*,*>
                    val sendName = data["name"] as? String ?: ""
                    setGropNameAdapter(sendName,groupId, sendId)
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

    public override fun onResume() {
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
    private fun setGropNameAdapter(sendName: String, groupId: String, sendId: String){
        databaseReference = FirebaseDatabase.getInstance().reference
        groupRef = databaseReference.child(InventriesPATH).child(groupId)
        groupRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.value as Map<*,*>
                val groupName = data["groupName"] as? String ?: ""
                val noticeList = NoticeList(
                    groupName, sendName, groupId
                )
                noticeArrayList.add(noticeList)
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}

        })
    }

    /**
     * グループに参加するボタンを押された際に
     * Firebaseに参加したグループのメンバーにユーザーIDを登録する
     */
    public fun groupInviteParticipation(userId: String, groupId: String, groupName: String){
        val inviteRef = databaseReference.child(InventriesPATH).child(groupId)
        val inviteAddRef = databaseReference.child(InventriesPATH).child(groupId).child("member")
        val userRef = databaseReference.child(UsersPATH).child(userId).child("groupID").child(groupId)
        val memberMap = HashMap<String, String>()
        var memberCount = 0
        inviteRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.value as Map<*,*>
                val memberData = data["member"] as Map<*,*>
                for(key in memberData.keys){
                    memberMap[key.toString()] = memberData[key] as? String ?: ""
                    memberCount++
                }
                memberMap["gest" + memberCount] = userId
                inviteAddRef.setValue(memberMap)

                groupParticipationAdd(userRef, userId)
            }
            override fun onCancelled(error: DatabaseError) {}

        })
    }

    /**
     * グループ参加によるDBのデータ変更
     * FirebaseのUserノードにあるgroupIDノードにグループIDとグループ番号を登録
     */
    private fun groupParticipationAdd(userRef: DatabaseReference, userId: String){
        var membarCount = 0
        val userCountRef = databaseReference.child(UsersPATH).child(userId)
        userCountRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.value as Map<*,*>
                val data2 = data["groupID"] as Map<*,*>
                for(key in data2.keys){
                    membarCount++
                }
                userRef.setValue("group" + membarCount)
                onResume()
            }
            override fun onCancelled(error: DatabaseError) {}

        })
    }
}