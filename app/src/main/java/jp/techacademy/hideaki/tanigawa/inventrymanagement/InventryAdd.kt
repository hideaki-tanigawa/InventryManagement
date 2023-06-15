package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ActivityInventryAddBinding
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class InventryAdd : AppCompatActivity(), View.OnClickListener,
    DatabaseReference.CompletionListener {
    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
    }

    private lateinit var binding: ActivityInventryAddBinding
    private lateinit var inventry: Inventry
    private var moveBoolean: Boolean = false
    private var pictureUri: Uri? = null
    private var calendar = Calendar.getInstance()
    private var noticeId: String = ""
    private var groupKindName: String = ""
    private var shopListFlag: Boolean = false
    private var noticeNo = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventryAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 渡ってきたQuestionのオブジェクトを保持する
        // API33以上でgetSerializableExtra(key)が非推奨となったため処理を分岐
        try {
            @Suppress("UNCHECKED_CAST", "DEPRECATION", "DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL")
            inventry = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getSerializableExtra("inventry", Inventry::class.java)!!
            else
                (intent.getSerializableExtra("inventry") as? Inventry)!!

            moveBoolean = true
        }catch (e: NullPointerException){
            moveBoolean = false
        }

        // 買い物一覧画面から飛ばれているかBooleanで判別
        shopListFlag = intent.getBooleanExtra("shopList",false)

        // true：グループ名を選択するSpinnerを表示
        // false：グループ名を選択するSpinnerを非表示
        if(shopListFlag){
            binding.shopListGroupCorrectWrong.visibility = View.VISIBLE
            binding.groupNameSpinner.visibility = View.VISIBLE
        }else{
            binding.shopListGroupCorrectWrong.visibility = View.GONE
            binding.groupNameSpinner.visibility = View.GONE
        }

        // UIの準備
        title = getString(R.string.inventry_send_title)
        binding.commodityAddButton.setOnClickListener(this)
        binding.commodityImage.setOnClickListener(this)
        binding.dateButton.setOnClickListener(this)

        // 在庫作成画面の要素に値を代入する
        if(moveBoolean){
            assignmentValue(inventry)
        }

        // Intentで送られてきた値の取得
        groupKindName = intent.getStringExtra("groupIdKind").toString()
        Log.d("TEST",groupKindName)
    }

    override fun onResume() {
        super.onResume()

        // Spinnerの表示
        val spinner = findViewById<Spinner>(R.id.noticeTimingSpinner)
        val adapter = ArrayAdapter.createFromResource(this, R.array.notice_time_array,android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // OnItemSelectedListenerの実装
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            // 項目が選択された時に呼ばれる
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val spinnerId = parent?.selectedItemId
                noticeId = spinnerId!!.toString()
            }

            // 基本的には呼ばれないが、何らかの理由で選択されることなく項目が閉じられたら呼ばれる
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinner.setSelection(noticeNo)
    }

    /**
     * 右上のメニューから買い物リスト追加アイコンを表示する
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_inventory_add, menu)

        val item = menu.findItem(R.id.action_shop_list_add)

        // 在庫新規登録と編集時でのアイコン表示・非表示
        if(moveBoolean){
            item.setVisible(true)
        }else{
            item.setVisible(false)
        }

        // アイコンの色を白に変更する
        val drawble = item!!.icon
        drawble!!.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP)
        item!!.setIcon(drawble)
        return true
    }

    /**
     * 右上のメニューから買い物リストに追加できるようにする
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId

        when(itemId){
            R.id.action_shop_list_add -> {
                val dataBaseReference = FirebaseDatabase.getInstance().reference
                val userID = FirebaseAuth.getInstance().currentUser!!.uid
                val userRef = dataBaseReference.child(UsersPATH).child(userID)
                var price = 0

                // 一つあたりの価格計算
                price = inventry.price.toInt() / inventry.count.toInt()

                val shopMap = HashMap<String,String>()
                shopMap["buyCount"] = "1"
                shopMap["buyPrice"] = price.toString()
                shopMap["inventryId"] = inventry.inventryUid

                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val data = snapshot.value as Map<*, *>?
                        val data2 = data!!["groupID"] as Map<*,*>
                        for(key in data2.keys){
                            val kindName = data2[key] as? String?: ""
                            if(kindName.equals(groupKindName)){
                                val shopRef = dataBaseReference.child(ShoppingPATH).child(key.toString())
                                shopRef.setValue(shopMap)
                                Toast.makeText(this@InventryAdd, "この在庫品を買い物リストに追加いたしました", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    override fun onCancelled(firebaseError: DatabaseError) {}
                })
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * このActivityに戻ってきた時の処理
     */
    private var launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultCode: Int = result.resultCode
        val data: Intent? = result.data

        if (resultCode != Activity.RESULT_OK) {
            if (pictureUri != null) {
                contentResolver.delete(pictureUri!!, null, null)
                pictureUri = null
            }
            return@registerForActivityResult
        }

        // 画像を取得
        val uri = if (data == null || data.data == null) pictureUri else data.data

        // URIからBitmapを取得する
        val image: Bitmap
        try {
            val contentResolver = contentResolver
            val inputStream = contentResolver.openInputStream(uri!!)
            image = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
        } catch (e: Exception) {
            return@registerForActivityResult
        }

        // 取得したBimapの長辺を500ピクセルにリサイズする
        val imageWidth = image.width
        val imageHeight = image.height
        val scale =
            (500.toFloat() / imageWidth).coerceAtMost(500.toFloat() / imageHeight) // (1)

        val matrix = Matrix()
        matrix.postScale(scale, scale)

        val resizedImage =
            Bitmap.createBitmap(image, 0, 0, imageWidth, imageHeight, matrix, true)

        // BitmapをImageViewに設定する
        binding.commodityImage.setImageBitmap(resizedImage)

        pictureUri = null
    }

    override fun onClick(v: View) {
        if (v === binding.commodityImage) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // 許可されている
                showChooser()
            } else {
                // パーミッションの許可状態を確認する
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // 許可されている
                    showChooser()
                } else {
                    // 許可されていないので許可ダイアログを表示する
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_CODE
                    )

                    return
                }
            }
        } else if (v === binding.commodityAddButton) {
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)

            val dataBaseReference = FirebaseDatabase.getInstance().reference

            val userID = FirebaseAuth.getInstance().currentUser!!.uid
            val userRef = dataBaseReference.child(UsersPATH).child(userID)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.value as Map<*, *>?
                    val data2 = data!!["groupID"] as Map<*,*>
                    for(key in data2.keys){
                        val kindName = data2[key] as? String?: ""
                        if(kindName.equals(groupKindName)){
                            registerInventryInfo(key.toString(), userID)
                        }
                    }
                }

                override fun onCancelled(firebaseError: DatabaseError) {}
            })
        } else if (v === binding.dateButton) {
            // 日付ダイアログを表示する
            /**
             * 日付選択ボタン
             */
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    setDateTimeButtonText()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ユーザーが許可したとき
                    showChooser()
                }
                return
            }
        }
    }

    private fun showChooser() {
        // ギャラリーから選択するIntent
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

        // カメラで撮影するIntent
        val filename = System.currentTimeMillis().toString() + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        pictureUri = contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri)

        // ギャラリー選択のIntentを与えてcreateChooserメソッドを呼ぶ
        val chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.get_image))

        // EXTRA_INITIAL_INTENTSにカメラ撮影のIntentを追加
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        launcher.launch(chooserIntent)
    }

    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        binding.progressBar.visibility = View.GONE

        if (databaseError == null) {
            finish()
        } else {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.inventry_send_error_message),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Firebaseに登録する処理
     */
    private fun registerInventryInfo(groupID: String, userID: String){
        val dataBaseReference = FirebaseDatabase.getInstance().reference

        val data = HashMap<String, String>()

        val genreRef = if(moveBoolean == true){
            dataBaseReference.child(InventriesPATH).child(groupID).child(inventry.inventryUid)
        }else{
            dataBaseReference.child(InventriesPATH).child(groupID)
        }

        // UID
        data["uid"] = userID

        // タイトルと本文を取得する
        val title = binding.commodityNameEdit.text.toString()
        val price = binding.commodityPriceEdit.text.toString()
        val count = binding.commodityCountEdit.text.toString()
        val genre = binding.commodityGenreEdit.text.toString()
        val place = binding.commodityPlaceEdit.text.toString()
        val notice = noticeId
        val date = binding.commodityDateText.text.toString()

        if (title.isEmpty()) {
            // タイトルが入力されていない時はエラーを表示するだけ
            Snackbar.make(binding.commodityAddButton, getString(R.string.input_title), Snackbar.LENGTH_LONG).show()
            return
        }

        if (price.isEmpty()) {
            // 質問が入力されていない時はエラーを表示するだけ
            Snackbar.make(binding.commodityAddButton, getString(R.string.price_message), Snackbar.LENGTH_LONG).show()
            return
        }

        data["commodity"] = title
        data["price"] = price
        data["count"] = count
        data["genre"] = genre
        data["place"] = place
        data["date"] = date
        data["notice"] = notice
        data["shopBoolean"] = "0"

        // 添付画像を取得する
        val drawable = binding.commodityImage.drawable as? BitmapDrawable

        // 添付画像が設定されていれば画像を取り出してBASE64エンコードする
        if (drawable != null) {
            val bitmap = drawable.bitmap
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val bitmapString =
                Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)

            data["image"] = bitmapString
        }

        Log.d("TEST", "qwertyuiop@")
        if(moveBoolean == true){
            genreRef.setValue(data, this)
        }else{
            genreRef.push().setValue(data, this)
        }
        binding.progressBar.visibility = View.VISIBLE
    }

    /**
     * 日付と時刻のボタンの表示を設定する
     */
    private fun setDateTimeButtonText() {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPANESE)
        binding.commodityDateText.text = dateFormat.format(calendar.time)
    }

    /**
     * 要素を代入する処理
     */
    private fun assignmentValue(inventry: Inventry){
        val bytes = inventry.imageBytes
        if (bytes.isNotEmpty()) {
            val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                .copy(Bitmap.Config.ARGB_8888, true)
            binding.commodityImage.setImageBitmap(image)
        }

        binding.commodityNameEdit.setText(inventry.commodity)
        binding.commodityCountEdit.setText(inventry.count)
        binding.commodityPriceEdit.setText(inventry.price)
        binding.commodityGenreEdit.setText(inventry.genre)
        binding.commodityPlaceEdit.setText(inventry.place)
        noticeNo = inventry.notice.toInt()

        // 在庫の日付をcalendarに反映
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPANESE)
        calendar.time = simpleDateFormat.parse(inventry.date) as Date

        setDateTimeButtonText()
    }

    /**
     * グループ名を表示するためのSpinnerの処理
     */
    private fun groupNameSpinner(){
        // Spinnerの表示
        val spinner = findViewById<Spinner>(R.id.groupNameSpinner)
        val adapter = ArrayAdapter.createFromResource(this, R.array.notice_time_array,android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // OnItemSelectedListenerの実装
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            // 項目が選択された時に呼ばれる
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val spinnerId = parent?.selectedItemId
                noticeId = spinnerId!!.toString()
            }

            // 基本的には呼ばれないが、何らかの理由で選択されることなく項目が閉じられたら呼ばれる
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinner.setSelection(noticeNo)
    }
}