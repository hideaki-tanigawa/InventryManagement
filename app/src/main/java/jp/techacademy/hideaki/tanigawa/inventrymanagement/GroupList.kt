package jp.techacademy.hideaki.tanigawa.inventrymanagement

import jp.techacademy.hideaki.tanigawa.inventrymanagement.Member
import java.io.Serializable
import java.util.ArrayList

class GroupList(
    val groupId: String,
    val groupName: String,
    val groupKindName: String,
    val groups: Int
):Serializable