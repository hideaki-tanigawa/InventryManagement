package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

// 通知チャネルのID
private val CHANNEL_ID = "my_channel_id"

// 通知の重要度
private val NOTIFICATION_PRIORITY = NotificationCompat.PRIORITY_DEFAULT

// 通知を作成する関数
fun createNotificationChannel(context: Context){
    // チャンネルをシステムに登録する
    val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // NotificationChannelを作成する、ただしAPI26+の場合のみなので
    // NotificationChannelクラスは新しいもので、サポートライブラリにはありません。
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "在庫管理アプリ"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        notificationManager.createNotificationChannel(channel)
    }

    // NotificationCompat.Builderを使用して通知を作成
    var builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_icon)
        .setContentTitle("在庫管理アプリ")
        .setContentText("在庫が一つ減りました。")
        .setPriority(NOTIFICATION_PRIORITY)

    // 通知を表示
    notificationManager.notify(/* 通知ID */ 1, builder.build())
}