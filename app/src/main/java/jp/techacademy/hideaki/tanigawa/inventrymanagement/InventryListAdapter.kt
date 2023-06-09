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
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ListInventryBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class InventryListAdapter(context: Context) : BaseAdapter() {
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

        binding.inventryTitleText.text = inventryArrayList[position].commodity
        binding.inventryCountText.text = inventryArrayList[position].count
        val date1 = LocalDate.now()
        val invDate = inventryArrayList[position].date.split("/")
        val date2 = LocalDate.of(invDate[0].toInt(),invDate[1].toInt(),invDate[2].toInt())
        val increment = ChronoUnit.DAYS.between(date1,date2)
        binding.consumptionUnitText.text = increment.toString()
        if(increment <= 5){
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
}