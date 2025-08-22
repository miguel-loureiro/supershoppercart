package com.migge.supershoppercart.remote

import android.content.Context
import com.migge.supershoppercart.R
import com.migge.supershoppercart.storage.Config
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(context: Context): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .authenticator(TokenAuthenticator(context))
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(context: Context, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(Config.apiBaseUrl(context))
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}