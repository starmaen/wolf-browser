package com.wolf.browser

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import org.json.JSONArray
import org.json.JSONObject

object DownloadsHelper {

    private val EXTS = listOf(".zip",".rar",".7z",".tar",".gz",".mp3",".mp4",".avi",".mkv",".mov",
        ".webm",".wav",".flac",".pdf",".doc",".docx",".xls",".xlsx",".ppt",".pptx",".apk",".exe",
        ".iso",".bin",".img",".jpg",".jpeg",".png",".gif",".webp",".svg",".bmp",".txt",".csv",".json")

    fun isDownloadUrl(url: String, contentType: String? = null): Boolean {
        contentType?.let { c ->
            val l = c.lowercase()
            if(l.startsWith("application/") || l.startsWith("image/") || l.startsWith("video/") ||
                l.startsWith("audio/") || l.contains("octet-stream")) return true
        }
        val lower = url.lowercase().substringBefore('?').substringBefore('#')
        for(e in EXTS) if(lower.endsWith(e)) return true
        return false
    }

    fun guessFilename(url: String, cd: String? = null): String {
        cd?.let {
            Regex("filename\\*?=(?:UTF-8'')?[\"']?([^\"';]+)", RegexOption.IGNORE_CASE)
                .find(it)?.groupValues?.getOrNull(1)?.let { f -> return Uri.decode(f) }
        }
        val p = Uri.parse(url).path ?: return "download_${System.currentTimeMillis()}"
        val n = p.substringAfterLast('/')
        return if(n.isBlank() || !n.contains('.')) "download_${System.currentTimeMillis()}" else n
    }

    fun start(c: Context, url: String, filename: String?): Long {
        return try {
            val name = filename ?: guessFilename(url)
            val req = DownloadManager.Request(Uri.parse(url))
            req.setTitle(name)
            req.setDescription(url)
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.',"").lowercase())
            if(mime != null) req.setMimeType(mime)
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            req.allowScanningByMediaScanner()
            val dm = c.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = dm.enqueue(req)
            Storage.addDownload(c, url, name, id)
            id
        } catch(e: Exception) { -1L }
    }

    // استعلام حالة التنزيلات مع الحجم والتقدم
    fun queryAll(c: Context): JSONArray {
        val arr = Storage.getDownloads(c)
        val result = JSONArray()
        try {
            val dm = c.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            for(i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val id = item.optLong("id", -1)
                val o = JSONObject()
                o.put("url", item.optString("url"))
                o.put("filename", item.optString("filename"))
                o.put("time", item.optLong("time"))
                if(id > 0) {
                    val q = DownloadManager.Query().setFilterById(id)
                    val cur: Cursor? = dm.query(q)
                    if(cur != null && cur.moveToFirst()) {
                        val status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val total = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val soFar = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        o.put("total", total)
                        o.put("downloaded", soFar)
                        o.put("progress", if(total > 0) ((soFar * 100) / total) else 0)
                        o.put("status", when(status) {
                            DownloadManager.STATUS_SUCCESSFUL -> "complete"
                            DownloadManager.STATUS_FAILED -> "failed"
                            DownloadManager.STATUS_RUNNING -> "running"
                            DownloadManager.STATUS_PAUSED -> "paused"
                            DownloadManager.STATUS_PENDING -> "pending"
                            else -> "unknown"
                        })
                    } else {
                        o.put("status", "unknown")
                        o.put("total", 0)
                        o.put("downloaded", 0)
                        o.put("progress", 0)
                    }
                    cur?.close()
                } else {
                    o.put("status", "unknown")
                    o.put("total", 0)
                    o.put("downloaded", 0)
                    o.put("progress", 0)
                }
                result.put(o)
            }
        } catch(e: Exception) {}
        return result
    }
}
