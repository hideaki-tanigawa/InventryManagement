package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.RequiresApi
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ListNoticeBinding
import java.util.ArrayList

class NoticeListAdapter(context: Context) : BaseAdapter()  {
    private var layoutInflater: LayoutInflater
    private var noticeListArrayList = ArrayList<NoticeList>()

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

        return view
    }

    fun setNoticeListArrayList(noticeListArrayList: ArrayList<NoticeList>) {
        this.noticeListArrayList = noticeListArrayList
    }
}