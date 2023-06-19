package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.RequiresApi
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ListShopMemoBinding

class ShopListAdapter(context: Context) : BaseAdapter()  {
    private var layoutInflater: LayoutInflater
    private var shopListArrayList = ArrayList<ShopInventory>()

    init {
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return shopListArrayList.size
    }

    override fun getItem(position: Int): Any {
        return shopListArrayList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // ViewBindingを使うための設定
        val binding = if (convertView == null) {
            ListShopMemoBinding.inflate(layoutInflater, parent, false)
        } else {
            ListShopMemoBinding.bind(convertView)
        }
        val view: View = convertView ?: binding.root

        binding.inventryTitleText.text = shopListArrayList[position].commodity
        binding.inventryCountText.text = shopListArrayList[position].count

        val bytes = shopListArrayList[position].imageBytes
        if (bytes.isNotEmpty()) {
            val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                .copy(Bitmap.Config.ARGB_8888, true)
            binding.inventryImageView.setImageBitmap(image)
        }

        return view
    }

    fun setShopListArrayList(shopListArrayList: ArrayList<ShopInventory>) {
        this.shopListArrayList = shopListArrayList
    }
}