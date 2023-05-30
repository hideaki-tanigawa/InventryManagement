package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding

    private var genre = 0  // 追加

    private lateinit var databaseReference: DatabaseReference
    private lateinit var inventryArrayList: ArrayList<Inventry>
    private lateinit var adapter: InventryListAdapter

    private var genreRef: DatabaseReference? = null

    private val eventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>
            val commodity = map["commodity"] as? String ?: ""
            val price = map["price"] as? String ?: ""
            val count = map["count"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""
            val genre = map["genre"] as? String ?: ""
            val place = map["place"] as? String ?: ""
            val date = map["date"] as? String ?: ""
            val imageString = map["image"] as? String ?: ""
            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }

            val inventry = Inventry(
                commodity, price, count, uid, dataSnapshot.key ?: "",
                genre, place, date, bytes
            )
            inventryArrayList.add(inventry)
            adapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onChildRemoved(p0: DataSnapshot) {}
        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
        override fun onCancelled(p0: DatabaseError) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.content.toolbar)

        binding.content.fab.setOnClickListener {
            // ジャンルを選択していない場合はメッセージを表示するだけ
            if (genre == 0) {
                Snackbar.make(it, getString(R.string.inventry_no_select_screen), Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            // ログインしていなければログイン画面に遷移させる
            if (user == null) {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            }else{
                val intent = Intent(applicationContext, InventryAdd::class.java)
                startActivity(intent)
            }
        }

        // ----- 追加:ここから
        // ナビゲーションドロワーの設定
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.content.toolbar,
            R.string.app_name,
            R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)
        // ----- 追加:ここまで

        // 買い物リスト以外はお金を消す
        binding.content.inner.priceTextView.visibility = View.GONE

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        adapter = InventryListAdapter(this)
        inventryArrayList = ArrayList()
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        // 1:趣味を既定の選択とする
        if(genre == 0) {
            onNavigationItemSelected(navigationView.menu.getItem(0))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, UserSettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    // ----- 追加:ここから
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_inventry -> {
                binding.content.toolbar.title = getString(R.string.menu_inventry_label)
                genre = 1
                binding.content.inner.priceTextView.visibility = View.GONE
            }
            R.id.nav_shopping -> {
                binding.content.toolbar.title = getString(R.string.menu_shopping_label)
                genre = 2
                binding.content.inner.priceTextView.visibility = View.VISIBLE
            }
            R.id.nav_foodstuff -> {
                binding.content.toolbar.title = getString(R.string.menu_foodstuff_label)
                genre = 3
                binding.content.inner.priceTextView.visibility = View.GONE
            }
            R.id.nav_commodity -> {
                binding.content.toolbar.title = getString(R.string.menu_commodity_label)
                genre = 4
                binding.content.inner.priceTextView.visibility = View.GONE
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)

        // 在庫のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        inventryArrayList.clear()
        adapter.setInventryArrayList(inventryArrayList)
        binding.content.inner.listView.adapter = adapter

        // 選択したジャンルにリスナーを登録する
        if (genreRef != null) {
            genreRef!!.removeEventListener(eventListener)
        }
        genreRef = databaseReference.child(InventriesPATH)
        genreRef!!.addChildEventListener(eventListener)
        return true
    }
    // ----- 追加:ここまで
}