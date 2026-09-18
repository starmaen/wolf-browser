package com.wolf.browser

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object Storage {
    private const val PREFS = "wolf_prefs"
    private fun prefs(c: Context): SharedPreferences = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ===== BOOKMARKS =====
    fun getBookmarks(c: Context): JSONArray = try { JSONArray(prefs(c).getString("bookmarks","[]")) } catch(e:Exception){ JSONArray() }
    fun addBookmark(c: Context, url: String, title: String) {
        val arr = getBookmarks(c); val n = JSONArray()
        val o = JSONObject(); o.put("url",url); o.put("title", if(title.isBlank()) url else title); o.put("time",System.currentTimeMillis())
        n.put(o)
        for(i in 0 until arr.length()){ val x=arr.getJSONObject(i); if(x.optString("url")!=url) n.put(x) }
        prefs(c).edit().putString("bookmarks",n.toString()).apply()
    }
    fun removeBookmark(c: Context, url: String) {
        val arr = getBookmarks(c); val n = JSONArray()
        for(i in 0 until arr.length()){ val x=arr.getJSONObject(i); if(x.optString("url")!=url) n.put(x) }
        prefs(c).edit().putString("bookmarks",n.toString()).apply()
    }
    fun isBookmarked(c: Context, url: String): Boolean {
        val arr = getBookmarks(c)
        for(i in 0 until arr.length()) if(arr.getJSONObject(i).optString("url")==url) return true
        return false
    }
    fun clearBookmarks(c: Context) { prefs(c).edit().putString("bookmarks","[]").apply() }

    // ===== HISTORY =====
    fun getHistory(c: Context): JSONArray = try { JSONArray(prefs(c).getString("history","[]")) } catch(e:Exception){ JSONArray() }
    fun addHistory(c: Context, url: String, title: String) {
        if(url.startsWith("resource://")||url.startsWith("file://")||url.startsWith("about:")||url.startsWith("jar:")||url.startsWith("wolf://")) return
        val arr = getHistory(c); val n = JSONArray()
        val o = JSONObject(); o.put("url",url); o.put("title", if(title.isBlank()) url else title); o.put("time",System.currentTimeMillis())
        n.put(o)
        for(i in 0 until minOf(arr.length(),200)){ val x=arr.getJSONObject(i); if(x.optString("url")!=url) n.put(x) }
        prefs(c).edit().putString("history",n.toString()).apply()
    }
    fun removeHistoryItem(c: Context, url: String) {
        val arr = getHistory(c); val n = JSONArray()
        for(i in 0 until arr.length()){ val x=arr.getJSONObject(i); if(x.optString("url")!=url) n.put(x) }
        prefs(c).edit().putString("history",n.toString()).apply()
    }
    fun clearHistory(c: Context) { prefs(c).edit().putString("history","[]").apply() }

    // ===== DOWNLOADS =====
    fun getDownloads(c: Context): JSONArray = try { JSONArray(prefs(c).getString("downloads","[]")) } catch(e:Exception){ JSONArray() }
    fun addDownload(c: Context, url: String, filename: String, id: Long) {
        val arr = getDownloads(c); val n = JSONArray()
        val o = JSONObject(); o.put("url",url); o.put("filename",filename); o.put("time",System.currentTimeMillis()); o.put("id",id)
        n.put(o)
        for(i in 0 until minOf(arr.length(),100)) n.put(arr.getJSONObject(i))
        prefs(c).edit().putString("downloads",n.toString()).apply()
    }
    fun clearDownloads(c: Context) { prefs(c).edit().putString("downloads","[]").apply() }

    // ===== CLEAR ALL =====
    fun clearAll(c: Context) { prefs(c).edit().clear().apply() }

    // ===== GENERIC =====
    fun getString(c: Context, k: String, def: String): String = prefs(c).getString(k, def) ?: def
    fun setString(c: Context, k: String, v: String) { prefs(c).edit().putString(k,v).apply() }
    fun getBool(c: Context, k: String, def: Boolean): Boolean = prefs(c).getBoolean(k, def)
    fun setBool(c: Context, k: String, v: Boolean) { prefs(c).edit().putBoolean(k,v).apply() }
    fun getInt(c: Context, k: String, def: Int): Int = prefs(c).getInt(k, def)
    fun setInt(c: Context, k: String, v: Int) { prefs(c).edit().putInt(k,v).apply() }

    // ===== ★ الدوال المفقودة التي سببّت الخطأ ★ =====
    fun jsEnabled(c: Context): Boolean = getBool(c, "js_enabled", true)
    fun imagesEnabled(c: Context): Boolean = getBool(c, "images_enabled", true)
    fun adBlockEnabled(c: Context): Boolean = getBool(c, "adblock", true)
    fun cookiesEnabled(c: Context): Boolean = getBool(c, "cookies_enabled", true)
    fun trackingProtection(c: Context): Boolean = getBool(c, "tracking_prot", true)
    fun desktopMode(c: Context): Boolean = getBool(c, "desktop_mode", false)
    fun saveHistory(c: Context): Boolean = getBool(c, "save_history", true)
    fun maxTabs(c: Context): Int = getInt(c, "max_tabs", 5)
    fun searchEngine(c: Context): String = getString(c, "engine", "google")
    fun homeUrl(c: Context): String = getString(c, "home_url", "resource://android/assets/home.html")
    fun adsBlocked(c: Context): Int = getInt(c, "ads_blocked", 0)
    fun addAdsBlocked(c: Context, n: Int) { setInt(c, "ads_blocked", adsBlocked(c) + n) }
    fun resetAdsBlocked(c: Context) { setInt(c, "ads_blocked", 0) }
}
