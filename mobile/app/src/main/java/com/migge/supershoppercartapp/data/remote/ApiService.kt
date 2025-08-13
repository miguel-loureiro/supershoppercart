package com.migge.supershoppercartapp.data.remote

import com.migge.supershoppercartapp.data.models.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // Login with Google
    @POST("google")
    suspend fun loginWithGoogle(
        @Header("Authorization") authorization: String,
        @Header("X-Device-Id") deviceId: String
    ): AuthModels

    // Example other endpoints (based on your models)
    @GET("shopcarts")
    suspend fun getShopCarts(): List<ShopCart>

    @GET("shopcarts/{id}")
    suspend fun getShopCartDetail(@Path("id") id: String): ShopCartDetailDTO

    @POST("shopcarts/{id}/share")
    suspend fun shareCart(
        @Path("id") id: String,
        @Body request: ShareCartRequest
    )
}