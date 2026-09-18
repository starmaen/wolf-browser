package com.wolf.browser

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.wolf.browser.databinding.FragmentBrowserBinding
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class BrowserFragment : Fragment() {

    private var _binding: FragmentBrowserBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: GeckoSession
    private lateinit var runtime: GeckoRuntime

    private val HOME_URL = "resource://android/assets/home.html"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?
    ): View {
        _binding = FragmentBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)

        runtime = GeckoRuntime.create(requireContext())
        session = GeckoSession()
        session.open(runtime)

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                showProgress()
                updateUrlBar(url)
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                hideProgress()
            }
            override fun onProgressChange(session: GeckoSession, progress: Int) {
                setProgress(progress)
            }
        }

        binding.geckoView.setSession(session)
        session.loadUri(HOME_URL)

        binding.homeBtn.setOnClickListener {
            session.loadUri(HOME_URL)
        }

        binding.transBtn.setOnClickListener {
            session.loadUri("resource://android/assets/translate.html")
        }

        binding.menuBtn.setOnClickListener { v ->
            showMainMenu(v)
        }

        binding.urlBar.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_GO || isEnter) {
                doSearch(binding.urlBar.text.toString())
                true
            } else false
        }
    }

    fun canGoBack(): Boolean = try { session.canGoBack() } catch (e: Exception) { false }
    fun canGoForward(): Boolean = try { session.canGoForward() } catch (e: Exception) { false }
    fun goBack() { try { session.goBack() } catch (e: Exception) {} }
    fun goForward() { try { session.goForward() } catch (e: Exception) {} }
    fun reload() { try { session.reload() } catch (e: Exception) {} }
    fun stopLoad() { try { session.stop() } catch (e: Exception) {} }
    fun goHome() { try { session.loadUri(HOME_URL) } catch (e: Exception) {} }

    fun currentUrl(): String = try {
        session.progressDelegate?.let { "" } ?: ""
        binding.urlBar.text.toString()
    } catch (e: Exception) { "" }

    private fun doSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return

        val url = when {
            q.startsWith("http://") || q.startsWith("https://") -> q
            q.matches(Regex("^[a-z0-9.-]+\\.[a-z]{2,}(/.*)?$", RegexOption.IGNORE_CASE)) -> "https://$q"
            else -> "https://www.google.com/search?q=" + Uri.encode(q)
        }
        session.loadUri(url)
    }

    private fun updateUrlBar(url: String) {
        activity?.runOnUiThread {
            if (url.startsWith("resource://") || url.startsWith("file:///android_asset")) {
                binding.urlBar.setText("")
                binding.urlBar.hint = "ابحث..."
            } else {
                binding.urlBar.setText(url)
            }
        }
    }

    private fun showProgress() {
        activity?.runOnUiThread {
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.progress = 0
        }
    }

    private fun hideProgress() {
        activity?.runOnUiThread {
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun setProgress(p: Int) {
        activity?.runOnUiThread {
            binding.progressBar.progress = p
        }
    }

    private fun showMainMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "→  رجوع")
        popup.menu.add(0, 2, 1, "←  تقدّم")
        popup.menu.add(0, 3, 2, "⟳  تحديث")
        popup.menu.add(0, 4, 3, "🏠  الرئيسية")
        popup.menu.add(0, 5, 4, "📑  التبويبات")
        popup.menu.add(0, 6, 5, "⬇  التنزيلات")
        popup.menu.add(0, 7, 6, "⭐  المفضلة")
        popup.menu.add(0, 8, 7, "🕘  السجل")
        popup.menu.add(0, 9, 8, "🌐  الترجمة")
        popup.menu.add(0, 10, 9, "⚙  الإعدادات")
        popup.menu.add(0, 11, 10, "ℹ  حول")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> if (canGoBack()) goBack()
                2 -> if (canGoForward()) goForward()
                3 -> reload()
                4 -> goHome()
                5 -> showTabsDialog()
                6 -> session.loadUri("resource://android/assets/downloads.html")
                7 -> session.loadUri("resource://android/assets/bookmarks.html")
                8 -> session.loadUri("resource://android/assets/history.html")
                9 -> session.loadUri("resource://android/assets/translate.html")
                10 -> session.loadUri("resource://android/assets/settings.html")
                11 -> session.loadUri("resource://android/assets/about.html")
            }
            true
        }
        popup.show()
    }

    private fun showTabsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("التبويبات المفتوحة")
            .setMessage("• التبويب الحالي\n\n(دعم تعدد التبويبات سيُضاف في التحديث القادم)")
            .setPositiveButton("تبويب جديد") { _, _ ->
                session.loadUri(HOME_URL)
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { session.close() } catch (e: Exception) {}
        _binding = null
    }
}
