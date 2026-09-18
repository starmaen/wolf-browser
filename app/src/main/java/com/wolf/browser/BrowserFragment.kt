package com.wolf.browser

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.wolf.browser.databinding.FragmentBrowserBinding
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

class BrowserFragment : Fragment() {
    private var _b: FragmentBrowserBinding? = null
    private val b get() = _b!!
    private lateinit var runtime: GeckoRuntime
    private lateinit var tabs: TabsManager
    private var canBack = false
    private var canFwd = false
    private var curUrl = ""
    private var curTitle = ""
    private val HOME = "resource://android/assets/home.html"

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentBrowserBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        runtime = GeckoRuntime.create(requireContext())
        tabs = TabsManager(runtime)
        createTab(HOME)

        b.homeBtn.setOnClickListener { loadUrl(HOME) }
        b.bookmarkBtn.setOnClickListener { toggleBookmark() }
        b.menuBtn.setOnClickListener { showMenu(it) }
        b.backBtn.setOnClickListener { if (canBack) tabs.activeTab()?.session?.goBack() }
        b.fwdBtn.setOnClickListener { if (canFwd) tabs.activeTab()?.session?.goForward() }
        b.tabsBtn.setOnClickListener { showTabs() }
        b.newTabBtn.setOnClickListener { createTab(HOME) }
        b.syncBtn.setOnClickListener { showSync() }

        b.urlBar.setOnEditorActionListener { _, a, e ->
            val enter = e?.keyCode == KeyEvent.KEYCODE_ENTER && e.action == KeyEvent.ACTION_DOWN
            if (a == EditorInfo.IME_ACTION_GO || enter) { doSearch(b.urlBar.text.toString()); true } else false
        }
    }

    private fun createTab(url: String) {
        val t = tabs.createTab(url)
        attachSession(t.session)
        b.geckoView.setSession(t.session)
        updateTabCount(); loadUrl(url)
    }

    private fun attachSession(s: GeckoSession) {
        s.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(sess: GeckoSession, u: String) {
                if (sess === tabs.activeTab()?.session) {
                    activity?.runOnUiThread { b.progressBar.visibility = View.VISIBLE; b.progressBar.progress = 0 }
                    curUrl = u; updateUrl(u)
                }
            }
            override fun onPageStop(sess: GeckoSession, ok: Boolean) {
                if (sess === tabs.activeTab()?.session) {
                    activity?.runOnUiThread { b.progressBar.visibility = View.GONE }
                    if (ok && curUrl.isNotBlank()) Storage.addHistory(requireContext(), curUrl, curTitle)
                }
            }
            override fun onProgressChange(sess: GeckoSession, p: Int) {
                if (sess === tabs.activeTab()?.session) activity?.runOnUiThread { b.progressBar.progress = p }
            }
        }
        s.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(sess: GeckoSession, ok: Boolean) { if (sess === tabs.activeTab()?.session) canBack = ok }
            override fun onCanGoForward(sess: GeckoSession, ok: Boolean) { if (sess === tabs.activeTab()?.session) canFwd = ok }
            override fun onLoadRequest(sess: GeckoSession, req: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                val u = req.uri
                // wolf:// — داخلي
                if (u.startsWith("wolf://")) {
                    activity?.runOnUiThread { handleWolf(u) }
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                // intent:// — تطبيقات خارجية
                if (u.startsWith("intent://")) {
                    try {
                        val intent = Intent.parseUri(u, Intent.URI_INTENT_SCHEME)
                        startActivity(intent)
                    } catch(e: Exception) {
                        // جرب الرابط الاحتياطي
                        try {
                            val fallback = Intent.parseUri(u, Intent.URI_INTENT_SCHEME).getStringExtra("browser_fallback_url")
                            if(fallback != null) loadUrl(fallback)
                        } catch(e2: Exception) {}
                    }
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                // تنزيل
                if (DownloadsHelper.isDownloadUrl(u)) {
                    activity?.runOnUiThread { startDl(u, null) }
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                // بروتوكولات أخرى (tel:, mailto:, market:, whatsapp://... إلخ)
                val scheme = try { Uri.parse(u).scheme?.lowercase() } catch(e: Exception) { null }
                if (scheme != null && scheme !in listOf("http","https","resource","about","file","wolf","jar","data","blob","javascript")) {
                    try {
                        val i = Intent(Intent.ACTION_VIEW, Uri.parse(u))
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(i)
                    } catch(e: Exception) {
                        activity?.runOnUiThread { toast("لا يوجد تطبيق يفتح هذا الرابط") }
                    }
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }
        }
        s.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(sess: GeckoSession, title: String?) {
                curTitle = title ?: ""
                if (sess === tabs.activeTab()?.session) tabs.updateTab(tabs.activeTab()!!, title = curTitle)
            }
        }
    }

    private fun loadUrl(u: String) { tabs.activeTab()?.session?.loadUri(u) }

    private fun handleWolf(u: String) {
        try {
            val uri = Uri.parse(u)
            when (uri.host) {
                "bookmarks" -> loadBookmarks()
                "history" -> loadHistory()
                "downloads" -> loadDownloads()
                "add-bookmark" -> {
                    val url = uri.getQueryParameter("url") ?: return
                    val title = uri.getQueryParameter("title") ?: ""
                    Storage.addBookmark(requireContext(), url, title); toast("تم الحفظ")
                    loadBookmarks()
                }
                "remove-bookmark" -> {
                    Storage.removeBookmark(requireContext(), uri.getQueryParameter("url") ?: return)
                    loadBookmarks()
                }
                "remove-history" -> {
                    Storage.removeHistoryItem(requireContext(), uri.getQueryParameter("url") ?: return)
                    loadHistory()
                }
                "clear-history" -> { Storage.clearHistory(requireContext()); toast("تم مسح السجل"); loadHistory() }
                "clear-bookmarks" -> { Storage.clearBookmarks(requireContext()); toast("تم مسح المفضلة"); loadBookmarks() }
                "clear-downloads" -> { Storage.clearDownloads(requireContext()); toast("تم المسح"); loadDownloads() }
                "clear-all" -> { Storage.clearAll(requireContext()); toast("تم مسح كل شيء") }
                "export" -> exportSync()
                "import" -> showImport()
            }
        } catch (e: Exception) { toast("خطأ: ${e.message}") }
    }

    private fun startDl(u: String, f: String?) {
        val id = DownloadsHelper.start(requireContext(), u, f)
        toast(if (id > 0) "بدأ التنزيل في الخلفية" else "فشل بدء التنزيل")
    }

    private fun loadBookmarks() { loadUrl("resource://android/assets/bookmarks.html?d=${enc(Storage.getBookmarks(requireContext()).toString())}") }
    private fun loadHistory() { loadUrl("resource://android/assets/history.html?d=${enc(Storage.getHistory(requireContext()).toString())}") }
    private fun loadDownloads() {
        val realData = DownloadsHelper.queryAll(requireContext()).toString()
        loadUrl("resource://android/assets/downloads.html?d=${enc(realData)}")
    }
    private fun enc(s: String) = Base64.encodeToString(s.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)

    private fun toggleBookmark() {
        if (curUrl.isBlank() || curUrl.startsWith("resource://") || curUrl.startsWith("about:") || curUrl.startsWith("jar:")) { toast("لا يمكن حفظ هذه الصفحة"); return }
        if (Storage.isBookmarked(requireContext(), curUrl)) { Storage.removeBookmark(requireContext(), curUrl); toast("تم الحذف") }
        else { Storage.addBookmark(requireContext(), curUrl, curTitle); toast("تم الحفظ") }
    }

    private fun showTabs() {
        val list = tabs.allTabs()
        val items = list.mapIndexed { i, t -> (if (i == tabs.activeIndex()) "▶ " else "   ") + t.title.take(40) }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("التبويبات (${list.size})")
            .setItems(items) { _, w -> switchTab(w) }
            .setPositiveButton("تبويب جديد") { _, _ -> createTab(HOME) }
            .setNeutralButton("حذف الحالي") { _, _ ->
                tabs.closeTab(tabs.activeIndex())
                tabs.activeTab()?.let { attachSession(it.session); b.geckoView.setSession(it.session); loadUrl(it.url); updateTabCount() }
            }
            .setNegativeButton("إغلاق", null).show()
    }

    private fun switchTab(i: Int) {
        val t = tabs.setActive(i) ?: return
        b.geckoView.setSession(t.session); loadUrl(t.url); updateTabCount(); updateUrl(t.url)
    }

    private fun updateTabCount() { b.tabsBtn.text = "📑 ${tabs.count()}" }

    private fun showSync() {
        val opts = arrayOf("☁ تصدير البيانات", "📥 استيراد البيانات", "🔗 نسخ كود المزامنة")
        AlertDialog.Builder(requireContext()).setTitle("المزامنة").setItems(opts) { _, w ->
            when (w) { 0 -> exportSync(); 1 -> showImport(); 2 -> copyCode() }
        }.show()
    }

    private fun exportSync() {
        val json = SyncManager.exportJson(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("تصدير البيانات")
            .setMessage("احفظ هذا النص:")
            .setNeutralButton("نسخ") { _, _ -> copyToClip(json); toast("تم النسخ") }
            .setPositiveButton("مشاركة") { _, _ ->
                val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, json) }
                startActivity(Intent.createChooser(i, "مشاركة"))
            }
            .setNegativeButton("إغلاق", null).show()
    }

    private fun showImport() {
        val inp = EditText(requireContext()).apply { hint = "الصق كود المزامنة"; setPadding(30,30,30,30) }
        AlertDialog.Builder(requireContext()).setTitle("استيراد البيانات").setView(inp)
            .setPositiveButton("استيراد") { _, _ ->
                val t = inp.text.toString().trim()
                if (t.isBlank()) { toast("لا يوجد نص"); return@setPositiveButton }
                val ok = if (t.startsWith("{")) SyncManager.importJson(requireContext(), t) else SyncManager.importBase64(requireContext(), t)
                toast(if (ok) "تم الاستيراد" else "فشل")
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun copyCode() { copyToClip(SyncManager.exportBase64(requireContext())); toast("تم النسخ") }
    private fun copyToClip(s: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("wolf", s))
    }

    private fun doSearch(q: String) {
        val x = q.trim(); if (x.isEmpty()) return
        val u = when {
            x.startsWith("http://") || x.startsWith("https://") -> x
            x.matches(Regex("^[a-z0-9.-]+\\.[a-z]{2,}(/.*)?$", RegexOption.IGNORE_CASE)) -> "https://$x"
            else -> "https://www.google.com/search?q=" + Uri.encode(x)
        }
        loadUrl(u)
    }

    private fun updateUrl(u: String) = activity?.runOnUiThread {
        if (u.startsWith("resource://") || u.startsWith("file://") || u.startsWith("jar:")) { b.urlBar.setText(""); b.urlBar.hint = "ابحث..." }
        else b.urlBar.setText(u)
    }

    private fun showMenu(anchor: View) {
        val p = PopupMenu(requireContext(), anchor)
        p.menu.add(0,1,0,"🏠 الرئيسية")
        p.menu.add(0,2,1,"⭐ المفضلة")
        p.menu.add(0,3,2,"🕘 السجل")
        p.menu.add(0,4,3,"⬇ التنزيلات")
        p.menu.add(0,5,4,"🌐 الترجمة")
        p.menu.add(0,6,5,"☁ المزامنة")
        p.menu.add(0,7,6,"⚙ الإعدادات")
        p.menu.add(0,8,7,"ℹ حول")
        p.setOnMenuItemClickListener { it ->
            when (it.itemId) {
                1 -> loadUrl(HOME); 2 -> loadBookmarks(); 3 -> loadHistory(); 4 -> loadDownloads()
                5 -> loadUrl("resource://android/assets/translate.html")
                6 -> showSync()
                7 -> loadUrl("resource://android/assets/settings.html")
                8 -> loadUrl("resource://android/assets/about.html")
            }; true
        }; p.show()
    }

    private fun toast(m: String) = activity?.runOnUiThread { Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show() }

    fun canGoBack() = canBack
    fun canGoForward() = canFwd
    fun goBack() { if (canBack) tabs.activeTab()?.session?.goBack() }
    fun goForward() { if (canFwd) tabs.activeTab()?.session?.goForward() }

    override fun onDestroyView() {
        super.onDestroyView()
        try { tabs.allTabs().forEach { it.session.close() } } catch (e: Exception) {}
        _b = null
    }
}
