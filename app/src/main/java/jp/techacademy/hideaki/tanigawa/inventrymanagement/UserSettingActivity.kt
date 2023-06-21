package jp.techacademy.hideaki.tanigawa.inventrymanagement

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import jp.techacademy.hideaki.tanigawa.inventrymanagement.databinding.ActivityUserSettingBinding

class UserSettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserSettingBinding

    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Preferenceから表示名を取得してEditTextに反映させる
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(NameKEY, "")
        binding.userNameText.setText(name)

        databaseReference = FirebaseDatabase.getInstance().reference

        // UIの初期設定
        title = getString(R.string.settings_title)

        binding.userNameChangeButton.setOnClickListener { v ->
            // キーボードが出ていたら閉じる
            val im = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていない場合は何もしない
                Snackbar.make(v, getString(R.string.no_login_user), Snackbar.LENGTH_LONG).show()
            } else {
                // 変更した表示名をFirebaseに保存する
                val name2 = binding.userNameText.text.toString()
                val userRef = databaseReference.child(UsersPATH).child(user.uid)
                val data = HashMap<String, Any>()
                data["name"] = name2

                // 名前の値だけをDBに変更をかける
                userRef.updateChildren(data).addOnSuccessListener {
                    // 変更した表示名をPreferenceに保存する
                    val sp2 = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    val editor = sp2.edit()
                    editor.putString(NameKEY, name2)
                    editor.apply()

                    Snackbar.make(v, getString(R.string.change_display_name), Snackbar.LENGTH_LONG)
                        .show()
                }
            }
        }

        /**
         * ログアウトボタンが押下した際、ログアウトする処理
         */
        binding.logoutButton.setOnClickListener { v ->
            FirebaseAuth.getInstance().signOut()
            binding.userNameText.setText("")
            Snackbar.make(v, getString(R.string.logout_complete_message), Snackbar.LENGTH_LONG)
                .show()
        }
    }
}