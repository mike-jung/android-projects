package org.techtown.photo

data class UploadResponse(
    val code:Int?,
    val message:String?,
    val output:UploadOutput?
)