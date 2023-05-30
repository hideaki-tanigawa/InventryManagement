package jp.techacademy.hideaki.tanigawa.inventrymanagement

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

        databaseReference = FirebaseDatabase.getInstance().reference

        // UIの初期設定
        title = getString(R.string.settings_title)

        binding.logoutButton.setOnClickListener { v ->
            FirebaseAuth.getInstance().signOut()
            Snackbar.make(v, getString(R.string.logout_complete_message), Snackbar.LENGTH_LONG)
                .show()
        }
    }
}