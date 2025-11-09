package com.csi.edile
data class Entry(val id:String="", val date:String="", val worker:String="", val hours:Int=0, val expense:Double=0.0, val notes:String="")
data class UserProfile(val uid:String="", val email:String="", val displayName:String="", val role:String="admin")