package com.wolf.browser

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object Storage {
    private const val PREFS = "wolf_prefs"
    private fun prefs(c: Context): SharedPreferences = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

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
    fun getHistory(c: Context): JSONArray = try { JSONArray(prefs(c).getString("history","[]")) } catch(e:Exception){ JSONArray() }
    fun addHistory(c: Context, url: String, title: String) {
        if(url.startsWith("resource://")||url.startsWith("file://")||url.startsWith("about:")) return
        val arr = getHistory(c); val n = JSONArray()
        val o = JSONObject(); o.put("url",url); o.put("title", if(title.isBlank()) url else title); o.put("time",System.currentTimeMillis())
        n.put(o)
        for(i in 0 until minOf(arr.length(),200)){ val x=arr.getJSONObject(i); if(x.optString("url")!=url) n.put(x) }
        prefs(c).edit().putString("history",n.toString()).apply()
    }
    fun clearHistory(c: Context) { prefs(c).edit().putString("history","[]").apply() }
    fun clearBookmarks(c: Context) { prefs(c).edit().putString("bookmarks","[]").apply() }
    fun getDownloads(c: Context): JSONArray = try { JSONArray(prefs(c).getString("downloads","[]")) } catch(e:Exception){ JSONArray() }
    fun addDownload(c: Context, url: String, filename: String) {
        val arr = getDownloads(c); val n = JSONArray()
        val o = JSONObject(); o.put("url",url); o.put("filename",filename); o.put("time",System.currentTimeMillis())
        n.put(o)
        for(i in 0 until minOf(arr.length(),100)) n.put(arr.getJSONObject(i))
        prefs(c).edit().putString("downloads",n.toString()).apply()
    }
    fun clearAll(c: Context) { prefs(c).edit().clear().apply() }
    fun getString(c: Context, k: String, def: String): String = prefs(c).getString(k, def) ?: def
    fun setString(c: Context, k: String, v: String) { prefs(c).edit().putString(k,v).apply() }
}
