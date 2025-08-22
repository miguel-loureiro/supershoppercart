package com.migge.supershoppercart.storage

import android.util.Base64
import org.json.JSONObject

object JwtUtils {
    fun getExpiry(jwt: String): Long? {
        val parts = jwt.split(".")
        if (parts.size != 3) return null
        val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        val payload = JSONObject(payloadJson)
        return if (payload.has("exp")) payload.getLong("exp") * 1000 else null // Convert to ms
    }

    fun isExpired(jwt: String): Boolean {
        val expiry = getExpiry(jwt) ?: return true
        return System.currentTimeMillis() > expiry
    }
}