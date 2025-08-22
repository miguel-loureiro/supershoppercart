package com.migge.supershoppercart.storage

import android.content.Context
import com.migge.supershoppercart.R

object Config {
    fun apiBaseUrl(context: Context): String = context.getString(R.string.api_base_url)
    fun googleClientId(context: Context): String = context.getString(R.string.google_client_id)
}