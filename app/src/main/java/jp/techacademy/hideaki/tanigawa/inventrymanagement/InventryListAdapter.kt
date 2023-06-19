package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import com.google.firebase.database.FirebaseDatabase
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ListInventryBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.coroutines.coroutineContext

class InventryListAdapter(context: Context) : BaseAdapter() {
    val context:Context = context
    private var layoutInflater: LayoutInflater
    private var inventryArrayList = ArrayList<Inventry>()

    init {
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return inventryArrayList.size
    }

    override fun getItem(position: Int): Any {
        return inventryArrayList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // ViewBindingを使うための設定
        val binding = if (convertView == null) {
            ListInventryBinding.inflate(layoutInflater, parent, false)
        } else {
            ListInventryBinding.bind(convertView)
        }
        val view: View = convertView ?: binding.root

        val groupId = inventryArrayList[position].groupId
        val commodity = inventryArrayList[position].commodity
        val genre = inventryArrayList[position].genre
        val count = inventryArrayList[position].count
        val invetoryId = inventryArrayList[position].inventryUid
//        Log.d("COUNT",count)
        var invCount = count.toInt()

        binding.inventryTitleText.text = commodity

        val date1 = LocalDate.now()
        val invDate = inventryArrayList[position].date.split("/")
        val date2 = LocalDate.of(invDate[0].toInt(),invDate[1].toInt(),invDate[2].toInt())
        val increment = ChronoUnit.DAYS.between(date1,date2)
        var dateIncrement = ""

        if (increment.toInt() == 0 || invCount == 0){
            if(invCount > 0){
                invCount = invCount - 1
            }

            countEachNotificationChannel(context, commodity)

            if(invCount > 0){
                dateIncrement = dateDiff(groupId,commodity,genre,invetoryId,date2,invCount)
                binding.consumptionUnitText.text = dateIncrement
                binding.inventryCountText.text = invCount.toString()
            }else{
                binding.consumptionUnitText.text = invCount.toString()
                binding.inventryCountText.text = invCount.toString()
            }
        }else{
//            dateDiff(groupId,commodity,genre,invetoryId,date2,invCount)
            binding.consumptionUnitText.text = increment.toString()
            binding.inventryCountText.text = count
        }
        if(increment <= 5 || invCount == 0){
            binding.consumptionUnitText.setTextColor(Color.parseColor("#FF0000"))
        }

        val bytes = inventryArrayList[position].imageBytes
        if (bytes.isNotEmpty()) {
            val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                .copy(Bitmap.Config.ARGB_8888, true)
            binding.inventryImageView.setImageBitmap(image)
        }

        return view
    }

    fun setInventryArrayList(inventryArrayList: ArrayList<Inventry>) {
        this.inventryArrayList = inventryArrayList
    }

    /**
     * 在庫登録した日とどれだけ差分があるのか
     * @param groupId グループID
     * @param commodity 在庫名
     * @param genre ジャンル
     * @param invetoryId 在庫ID
     * @param inventoryDate 一個あたりの消費期日
     * @param count 在庫数
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun dateDiff(
        groupId:String,
        commodity:String,
        genre:String,
        invetoryId: String,
        inventoryDate: LocalDate,
        count: Int
    ): String{
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val uniqueKey = groupId + commodity + genre
        val date = sp.getString(uniqueKey, "")
//        Log.d("SSSSSS",date.toString())
//        val date = "2023/6/1"
        var increment: Long = 0

        if(!date.equals("") && date != null){
            val invDate = date.split("/")
            val date2 = LocalDate.of(invDate[0].toInt(),invDate[1].toInt(),invDate[2].toInt())
            increment = ChronoUnit.DAYS.between(date2,inventoryDate)

            // 日付を初期値に戻す
            val daysLater = inventoryDate.plusDays(increment)
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            val formattedDate = daysLater.format(formatter)

            // firebaseに登録
            val databaseReference = FirebaseDatabase.getInstance().reference
            val invRef = databaseReference.child(InventriesPATH).child(groupId).child(invetoryId)
            val map = HashMap<String, Any>()
            map["date"] = formattedDate
            map["count"] = count
            invRef.updateChildren(map)

            return increment.toString()
        }

        return increment.toString()
    }
}