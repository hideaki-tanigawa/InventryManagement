package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.RequiresApi
import com.google.firebase.database.*
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ListGroupBinding
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ListInventryBinding
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

class GroupListAdapter(context: Context) : BaseAdapter() {
    private var layoutInflater: LayoutInflater
    private var groupListArrayList = ArrayList<GroupList>()

    init {
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return groupListArrayList.size
    }

    override fun getItem(position: Int): Any {
        return groupListArrayList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // ViewBindingを使うための設定
        val binding = if (convertView == null) {
            ListGroupBinding.inflate(layoutInflater, parent, false)
        } else {
            ListGroupBinding.bind(convertView)
        }
        val view: View = convertView ?: binding.root
        val maxCharacters = 14
        val groupName = groupListArrayList[position].groupName

        if (groupName.length > maxCharacters){
            val truncatedText = groupName.substring(0, maxCharacters) + "..." // 制限を超えた場合に「...」を追加
            binding.groupNameText.text = truncatedText
        }else{
            binding.groupNameText.text = groupName
        }

        binding.groupMemberCount.text = groupListArrayList[position].groups.toString()

        return view
    }

    fun setGroupArrayList(groupListArrayList: ArrayList<GroupList>) {
        this.groupListArrayList = groupListArrayList
    }
}