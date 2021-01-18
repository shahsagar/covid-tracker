package com.sagar.assignment1

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface APIInterface {
    @Multipart
    @POST("/upload_db.php")
    fun uploadDB(
        @Part("description") description: RequestBody,
        @Part file: MutableList<MultipartBody.Part>
    ): Call<String>
}