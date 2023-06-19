package jp.techacademy.hideaki.tanigawa.inventrymanagement

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// 通知チャネルのID
private val CHANNEL_ID = "my_channel_id"

// 通知の重要度
private val NOTIFICATION_PRIORITY = NotificationCompat.PRIORITY_DEFAULT
private val GROUP_KEY_WORK_EMAIL = "com.android.example.WORK_EMAIL"

private var inventoryCountArrayList = ArrayList<ArrayList<String>>()
private var inventoryInfoArrayList = ArrayList<String>()
private var inventoryIdArrayList = ArrayList<String>()

/**
 * 在庫数が減る毎に通知する処理
 * @param context コンテキスト
 */
fun countEachNotificationChannel(context: Context, commodity: String){
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
        .setContentText(commodity + "が一つ減りました。")
        .setPriority(NOTIFICATION_PRIORITY)

    // 通知を表示
    notificationManager.notify(/* 通知ID */ 1, builder.build())
}

/**
 * 在庫数が残り1つになると通知する処理
 * @param context コンテキスト
 */
fun countOneNotificationChannel(context: Context, commodity: String){
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
        .setContentText(commodity + "が残り１つです。")
        .setPriority(NOTIFICATION_PRIORITY)

    // 通知を表示
    notificationManager.notify(/* 通知ID */ 2, builder.build())
}

/**
 * 在庫数が残り2つになると通知する処理
 * @param context コンテキスト
 */
fun countTwoNotificationChannel(context: Context, commodity: String){
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
        .setContentText(commodity + "が残り２つです。")
        .setPriority(NOTIFICATION_PRIORITY)

    // 通知を表示
    notificationManager.notify(/* 通知ID */ 3, builder.build())
}

/**
 * 在庫数が残り3つになると通知する処理
 * @param context コンテキスト
 */
fun countThreeNotificationChannel(context: Context, commodity: String){
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
        .setContentText(commodity + "が残り３つです。")
        .setPriority(NOTIFICATION_PRIORITY)

    // 通知を表示
    notificationManager.notify(/* 通知ID */ 4, builder.build())
}

/**
 * userが参加しているGroupのIDを取得する関数
 */
fun userHaveGroupIdStorage(context: Context){
    // userが参加しているGroupIDの一覧を格納する配列
    val userHaveGroupIdArrayList = ArrayList<String>()

    val databaseReference = FirebaseDatabase.getInstance().reference
    val userID = FirebaseAuth.getInstance().currentUser!!.uid
    val userGroupRef = databaseReference.child(UsersPATH).child(userID)
    userGroupRef.addListenerForSingleValueEvent(object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            val userMap = snapshot.value as Map<*,*>
            val groupIdMap = userMap["groupID"] as Map<*,*>
            for(key in groupIdMap.keys){
                userHaveGroupIdArrayList.add(key.toString())
            }
            groupIdInventoryCountStorage(userHaveGroupIdArrayList, context)
        }
        override fun onCancelled(error: DatabaseError) {}

    })
}

/**
 * グループIDに紐付くグループ名を取得
 * グループIDに紐付く在庫IDの取得
 * @param groupIdArrayList userが参加しているGroupIDの一覧を格納する配列
 */
private fun groupIdInventoryCountStorage(groupIdArrayList: ArrayList<String>, context: Context){

    inventoryCountArrayList.clear()
    inventoryInfoArrayList.clear()
    inventoryIdArrayList.clear()
    Log.d("保有Group数",groupIdArrayList.size.toString())

    val databaseReference = FirebaseDatabase.getInstance().reference
    val invRef = databaseReference
    invRef.addListenerForSingleValueEvent(object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            val groupIdMap = snapshot.value as Map<*,*>
            val groupMap = groupIdMap[InventriesPATH] as Map<*,*>
            for(groupKey in groupMap.keys){
                Log.d("CCCCCCC",groupKey.toString())
                for(index in groupIdArrayList.indices){
                    if(groupKey.toString().equals(groupIdArrayList[index])) {
                        Log.d("CCCCCCC", groupMap.keys.toString())
                        val groupNameMap = groupMap[groupKey] as Map<*,*>
                        Log.d("DDDDDDD", groupNameMap.keys.toString())
                        val groupName = groupNameMap["groupName"] as? String ?: ""
                        for(groupNameKey in groupNameMap.keys){
                            if(!groupName.equals("")){
                                inventoryInfoArrayList.add(groupName)
                            }else if(!groupNameKey.toString().equals("member") && groupName.equals("")){
                                inventoryInfoArrayList.add("個人")
                            }

                            if(!groupNameKey.toString().equals("member") && !groupNameKey.toString().equals("groupName")){
                                inventoryIdArrayList.add(groupNameKey.toString())
                                val invMap = groupNameMap[groupNameKey] as Map<*,*>
                                inventoryInfoArrayList.add(invMap["commodity"].toString())
                                inventoryInfoArrayList.add(invMap["count"].toString())
                                inventoryInfoArrayList.add(invMap["notice"].toString())
                                notificationChoice(inventoryInfoArrayList, context)
                            }
                            Log.d("TTTTTTTT", groupNameKey.toString())
                        }
                    }
                }
            }
        }
        override fun onCancelled(error: DatabaseError) {}
    })
}


//if(!groupNameKey.toString().equals("member")){
//    val invMap = groupNameMap[groupNameKey] as Map<*,*>
//    Log.d("SSSSSS",invMap.keys.toString())
//    for(inventoryKey in invMap.keys){
//        if(inventoryKey.toString().equals("commodity")){
//            inventoryInfoArrayList.add(invMap[inventoryKey].toString())
//        }else if(inventoryKey.toString().equals("count")){
//            Log.d("在庫確認",invMap[inventoryKey].toString())
//            inventoryInfoArrayList.add(invMap[inventoryKey].toString())
//        }
//    }
//    inventoryCountArrayList.add(inventoryInfoArrayList)
//}

/**
 * @param invInfoArrayList
 */
private fun notificationChoice(invInfoArrayList: ArrayList<String>, context: Context) {
    for(count in 0..invInfoArrayList.size - 1){
        Log.d("これっていける",invInfoArrayList[count])
    }
    val count = invInfoArrayList[2].toInt()

    if(invInfoArrayList[3].equals("2") && count == 2){
        countTwoNotificationChannel(context, invInfoArrayList[1])
    }else if (invInfoArrayList[3].equals("1") && count == 1){
        countOneNotificationChannel(context, invInfoArrayList[1])
    }else if (invInfoArrayList[3].equals("3") && count == 3){
        countThreeNotificationChannel(context, invInfoArrayList[1])
    }
    inventoryInfoArrayList.clear()
}

private fun groupNameInventoryIdStorage(inventoryIdList: ArrayList<String>){
    for(index in inventoryIdList.indices){
        Log.d("在庫管理",inventoryIdList[index])
    }
}


