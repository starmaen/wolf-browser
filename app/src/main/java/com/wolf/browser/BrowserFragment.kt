package com.wolf.browser

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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

    private val HOME_URL = "file:///android_asset/home.html"

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
                updateUrlBar(url)
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {}
            override fun onProgressChange(session: GeckoSession, progress: Int) {}
        }

        binding.geckoView.setSession(session)
        session.loadUri(HOME_URL)

        binding.homeBtn.setOnClickListener {
            session.loadUri(HOME_URL)
        }

        binding.menuBtn.setOnClickListener {
            showMenu()
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
            if (url.startsWith("file:///android_asset/")) {
                binding.urlBar.setText("")
                binding.urlBar.hint = "ابحث أو اكتب عنواناً..."
            } else {
                binding.urlBar.setText(url)
            }
        }
    }

    private fun showMenu() {
        val options = arrayOf(
            "الرئيسية",
            "الإعدادات",
            "المفضلة",
            "السجل",
            "حول التطبيق"
        )
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("القائمة")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> session.loadUri(HOME_URL)
                    1 -> session.loadUri("file:///android_asset/settings.html")
                    2 -> session.loadUri("file:///android_asset/bookmarks.html")
                    3 -> session.loadUri("file:///android_asset/history.html")
                    4 -> session.loadUri("file:///android_asset/about.html")
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        session.close()
        _binding = null
    }
}
