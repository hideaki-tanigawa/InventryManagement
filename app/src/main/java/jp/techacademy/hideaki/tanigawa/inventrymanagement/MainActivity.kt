package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = getString(R.string.menu_inventry_list_label)
        replaceFragment(Person())

        setSupportActionBar(binding.content.toolbar)

        val userLoginBoolean = getLoginBoolean()
        if(userLoginBoolean){
            val intent = Intent(applicationContext, LoginActivity::class.java)
            startActivity(intent)
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

        binding.content.bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.navi_person -> {
                    title = getString(R.string.menu_inventry_list_label)
                    replaceFragment(Person())
                }
                R.id.navi_group -> {
                    title = getString(R.string.menu_group_list_label)
                    replaceFragment(Group())
                }
                R.id.navi_shop -> {
                    title = getString(R.string.menu_shopping_label)
                    replaceFragment(Shop())
                }
                else -> {

                }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
    }

    // ----- 追加:ここから
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_user_management -> {
//                binding.content.toolbar.title = getString(R.string.menu_user_management_label)
                val intent = Intent(applicationContext, UserSettingActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_group_notice -> {
                binding.content.toolbar.title = getString(R.string.menu_group_notice_label)
//                binding.content.inner.priceTextView.visibility = View.VISIBLE
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun replaceFragment(fragment: Fragment){
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout,fragment)
        fragmentTransaction.commit()
    }

    /**
     * ユーザーがログインしているかどうかを判定する関数
     */
    private fun getLoginBoolean():Boolean{
        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser
        // ログイン済み：False、ログインしていない：True
        if(user == null){
            return true
        }else{
            return false
        }
    }
}