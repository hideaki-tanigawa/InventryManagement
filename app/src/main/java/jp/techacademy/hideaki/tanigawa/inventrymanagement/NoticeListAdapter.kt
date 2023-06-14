package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ListNoticeBinding
import java.util.ArrayList

class NoticeListAdapter(context: Context) : BaseAdapter()  {
    private var layoutInflater: LayoutInflater
    private var noticeListArrayList = ArrayList<NoticeList>()
    private val context = context

    init {
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return noticeListArrayList.size
    }

    override fun getItem(position: Int): Any {
        return noticeListArrayList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // ViewBindingを使うための設定
        val binding = if (convertView == null) {
            ListNoticeBinding.inflate(layoutInflater, parent, false)
        } else {
            ListNoticeBinding.bind(convertView)
        }
        val view: View = convertView ?: binding.root

        binding.groupNameTextView.text = noticeListArrayList[position].groupName
        binding.sendUserNameText.text = noticeListArrayList[position].sendUserName

        /**
         * 拒否ボタンが押された時に発火する
         * グループの参加を拒否する
         */
        binding.inviteRefusalButton.setOnClickListener{ _ ->
            val groupId = noticeListArrayList[position].groupID
            val groupName = noticeListArrayList[position].groupName

            // ダイアログを表示する
            val builder = AlertDialog.Builder(context)
            builder.setTitle("グループ参加を拒否")
            builder.setMessage(groupName + "の参加を\n拒否しますか？")
            builder.setPositiveButton("拒否") { _, _ ->
                val databaseReference = FirebaseDatabase.getInstance().reference
                val userId = FirebaseAuth.getInstance().currentUser!!.uid
                val inviteRef = databaseReference.child(InvitePATH).child(userId).child(groupId)
                inviteRef.removeValue()
                Toast.makeText(context, "グループ招待を拒否いたしました", Toast.LENGTH_LONG).show()
                (context as NoticeListActivity).onResume()
            }
            builder.setNegativeButton("キャンセル",null)
            val dialog = builder.create()
            dialog.show()
        }

        /**
         * 参加ボタンが押された時に発火する
         * グループに参加する
         */
        binding.inviteParticipationButton.setOnClickListener{_, ->
            val groupId = noticeListArrayList[position].groupID
            val groupName = noticeListArrayList[position].groupName

            // ダイアログを表示する
            val builder = AlertDialog.Builder(context)
            builder.setTitle("グループ参加")
            builder.setMessage(groupName + "に参加しますか？")
            builder.setPositiveButton("参加") { _, _ ->
                val databaseReference = FirebaseDatabase.getInstance().reference
                val userId = FirebaseAuth.getInstance().currentUser!!.uid
                val inviteRef = databaseReference.child(InvitePATH).child(userId).child(groupId)
                inviteRef.removeValue()
                Toast.makeText(context, groupName + "に参加いたしました", Toast.LENGTH_LONG).show()
                (context as NoticeListActivity).groupInviteParticipation(userId, groupId, groupName)
            }
            builder.setNegativeButton("キャンセル",null)
            val dialog = builder.create()
            dialog.show()
        }

        return view
    }

    fun setNoticeListArrayList(noticeListArrayList: ArrayList<NoticeList>) {
        this.noticeListArrayList = noticeListArrayList
    }

    //OcyGvtoA4qRABLsfdS1KLrmnIG52
}