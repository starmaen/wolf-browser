package com.wolf.browser

import android.content.Context
import android.util.Base64
import org.json.JSONObject

object SyncManager {
    fun exportJson(c: Context): String {
        val o = JSONObject()
        o.put("version", 1)
        o.put("exported", System.currentTimeMillis())
        o.put("bookmarks", Storage.getBookmarks(c))
        o.put("history", Storage.getHistory(c))
        o.put("downloads", Storage.getDownloads(c))
        o.put("engine", Storage.getString(c, "engine", "google"))
        o.put("theme", Storage.getString(c, "theme", "dark"))
        return o.toString(2)
    }
    fun exportBase64(c: Context): String =
        Base64.encodeToString(exportJson(c).toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
    fun importJson(c: Context, json: String): Boolean = try {
        val o = JSONObject(json)
        o.optJSONArray("bookmarks")?.let { Storage.setString(c, "bookmarks", it.toString()) }
        o.optJSONArray("history")?.let { Storage.setString(c, "history", it.toString()) }
        o.optJSONArray("downloads")?.let { Storage.setString(c, "downloads", it.toString()) }
        o.optString("engine").takeIf { it.isNotBlank() }?.let { Storage.setString(c, "engine", it) }
        o.optString("theme").takeIf { it.isNotBlank() }?.let { Storage.setString(c, "theme", it) }
        true
    } catch(e: Exception) { false }
    fun importBase64(c: Context, b: String): Boolean = try {
        importJson(c, String(Base64.decode(b, Base64.NO_WRAP or Base64.URL_SAFE)))
    } catch(e: Exception) { false }
}
