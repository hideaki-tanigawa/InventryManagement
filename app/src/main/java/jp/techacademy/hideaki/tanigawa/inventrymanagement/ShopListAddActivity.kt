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
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Selection.setSelection
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ActivityShopListAddBinding
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ShopListAddActivity : AppCompatActivity(), View.OnClickListener,
    DatabaseReference.CompletionListener {
    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
    }

    private lateinit var binding: ActivityShopListAddBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var inviteRef: DatabaseReference
    private lateinit var shopRef: DatabaseReference
    private lateinit var inventry: Inventry
    private var inventryName = ""
    private var moveBoolean: Boolean = false
    private var pictureUri: Uri? = null
    private var calendar = Calendar.getInstance()
    private var noticeId: String = ""
    private var groupNameSpinnerValue: String = ""
    private var noticeNo = 0
    private var groupIdShoop: String = ""
    private var userHaveGroupIdArrayList = ArrayList<String>()
    private var participateGroupName = ArrayList<String>()
    private val userHaveGroupMap = HashMap<String, String>()
    private val shopListMap = HashMap<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopListAddBinding.inflate(layoutInflater)
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

        // UIの準備
        title = "買い物リスト作成"
        binding.commodityAddButton.setOnClickListener(this)
        binding.commodityImage.setOnClickListener(this)
        binding.dateButton.setOnClickListener(this)

        // 在庫作成画面の要素に値を代入する
        if(moveBoolean){
            assignmentValue(inventry)
        }

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference
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

        getuserHaveGropId()
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

            val userId = FirebaseAuth.getInstance().currentUser!!.uid

            val data = HashMap<String, String>()

            val title = binding.commodityNameEdit.text.toString()
            val price = binding.commodityPriceEdit.text.toString()
            val count = binding.commodityCountEdit.text.toString()
            val genre = binding.commodityGenreEdit.text.toString()
            val place = binding.commodityPlaceEdit.text.toString()
            val notice = noticeId
            val date = binding.commodityDateText.text.toString()
            val groupId = userHaveGroupMap[groupNameSpinnerValue]

            data["uid"] = userId
            data["commodity"] = title
            inventryName = title
            data["price"] = price
            data["count"] = count
            data["genre"] = genre
            data["place"] = place
            data["date"] = date
            data["notice"] = notice

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

            // 買い物リストの作成処理か編集処理かを分岐している
            if(moveBoolean && groupId != null){
                data["shopBoolean"] = "0"
                shoppingListEdit(data, groupId, inventry.inventryUid)
            }else{
                data["shopBoolean"] = "1"
                if (groupId != null) {
                    shopListAdd(data, groupId)
                }
            }
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
     * 日付と時刻のボタンの表示を設定する
     */
    private fun setDateTimeButtonText() {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPANESE)
        binding.commodityDateText.text = dateFormat.format(calendar.time)
    }

    /**
     * 要素を代入する処理
     * @param inventry: 買い物リストの商品情報
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
        groupIdShoop = inventry.groupId

        // 在庫の日付をcalendarに反映
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPANESE)
        calendar.time = simpleDateFormat.parse(inventry.date) as Date

        setDateTimeButtonText()
    }

    /**
     * ユーザーが参加しているグループのID一覧を取得する
     */
    private fun getuserHaveGropId(){
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val userRef = databaseReference.child(UsersPATH).child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val userMap = snapshot.value as Map<*,*>
                val groupIdMap = userMap["groupID"] as Map<*,*>
                for(key in groupIdMap.keys){
                    userHaveGroupIdArrayList.add(key.toString())
                }
                getuserHaveGroupName()
            }
            override fun onCancelled(error: DatabaseError) {}

        })
    }

    /**
     * ユーザーが所属しているグループの名前一覧を取得
     */
    private fun getuserHaveGroupName(){
        val inventoryRef = databaseReference
        inventoryRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val wholeMap = snapshot.value as Map<*,*>
                val groupMap = wholeMap[InventriesPATH] as Map<*,*>
                for(key in groupMap.keys){
                    for(arrayIndex in userHaveGroupIdArrayList.indices){
                        if(key.toString().equals(userHaveGroupIdArrayList[arrayIndex])){
                            val groupNameMap = groupMap[key] as Map<*,*>
                            val groupName = groupNameMap["groupName"] as? String ?: ""
                            if(groupName.equals("")){
                                participateGroupName.add("個人")
                            }else{
                                participateGroupName.add(groupName)
                            }
                        }
                    }
                }
                groupNameSpinner()
            }
            override fun onCancelled(error: DatabaseError) {}

        })
    }

    // Spinnerにグループ名を入れる
    private fun groupNameSpinner(){
        for (index in userHaveGroupIdArrayList.indices){
            userHaveGroupMap[participateGroupName[index]] = userHaveGroupIdArrayList[index]
        }

        val groupName = mapGetKey(userHaveGroupMap,groupIdShoop)

        // Spinnerの表示
        val spinner = findViewById<Spinner>(R.id.groupNameSpinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, participateGroupName)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // OnItemSelectedListenerの実装
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            // 項目が選択された時に呼ばれる
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val spinnerId = parent?.selectedItem
                groupNameSpinnerValue = spinnerId!!.toString()
            }

            // 基本的には呼ばれないが、何らかの理由で選択されることなく項目が閉じられたら呼ばれる
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinner.setSelection(setSelection(spinner, groupName))
    }

    /**
     * 買い物リストを作成する処理
     * Firebaseの在庫一覧ノードと買い物一覧ノードに商品情報を保存
     */
    private fun shopListAdd(groupMap: Map<String,String>, groupId: String){
        shopListMap["buyCount"] = groupMap["count"].toString()
        shopListMap["buyPrice"] = groupMap["price"].toString()

        inviteRef = databaseReference.child(InventriesPATH).child(groupId)
        inviteRef.push().setValue(groupMap)

        groupIdShoop = groupId
        inviteRef.addChildEventListener(eventListener)

        Log.d("TEST",inviteRef.key.toString())
    }

    /**
     * 買い物リストを編集する処理
     * Firebaseの買い物一覧ノードに保存する
     */
    private fun shoppingListEdit(groupMap: Map<String,String>, groupId: String, inventryId: String){
        val shopMap = HashMap<String,String>()
        shopMap["buyCount"] = groupMap["count"].toString()
        shopMap["buyPrice"] = groupMap["price"].toString()
        shopRef = databaseReference.child(ShoppingPATH).child(groupId).child(inventryId)
        shopRef.setValue(shopMap)
        finish()
    }

    /**
     * groupNameがキーでgroupIdが値のMapから
     * 値からキーを取得する処理
     * @param groupMap: グループ名とグループIDが格納されてあるMap
     * @param target: グループID
     */
    private fun mapGetKey(groupMap: Map<String,String>, target: String):String{
        for ((key, value) in groupMap)
        {
            if (target == value) {
                return key
            }
        }
        return ""
    }

    /**
     * Spinnerの初期値項目を文字列で選択する処理
     * @param spinner: グループ名または個人が選択できるspinner
     * @param item: 初期項目を指定する文字列
     */
    private fun setSelection(spinner: Spinner, item: String): Int {
        val adapter = spinner.adapter
        var index = 0
        for (i in 0 ..adapter.count -1) {
            if (adapter.getItem(i) == item) {
                index = i
            }
        }
        return index
    }

    private var eventListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val invMap = snapshot.value as Map<*,*>
            val inventryId = snapshot.key.toString()
            Log.d("TEST-AAAA",inventryId)
            if(!inventryId.equals("member")){
                val shopName = invMap["commodity"] as? String ?: ""
                if(shopName.equals(inventryName)){
                    shopRef = databaseReference.child(ShoppingPATH).child(groupIdShoop).child(inventryId)
                    shopRef.setValue(shopListMap)
                    finish()
                }
            }
        }
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }
}
// R55QVm4VfVZm3nO2epCzpvnAtPf2