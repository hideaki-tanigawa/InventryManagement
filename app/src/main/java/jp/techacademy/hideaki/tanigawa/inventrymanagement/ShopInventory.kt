package jp.techacademy.hideaki.tanigawa.inventrymanagement

import java.io.Serializable

class ShopInventory(
    val commodity: String,
    val price: String,
    val count: String,
    val uid: String,
    val inventryUid: String,
    val genre: String,
    val place: String,
    val date: String,
    val notice: String,
    val groupId: String,
    val addCount: String,
    bytes: ByteArray,
) : Serializable {
    val imageBytes: ByteArray

    init {
        imageBytes = bytes.clone()
    }
}