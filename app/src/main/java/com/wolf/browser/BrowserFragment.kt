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

    private lateinit var geckoView: GeckoView
    private lateinit var session: GeckoSession
    private lateinit var runtime: GeckoRuntime

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?
    ): View {
        _binding = FragmentBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)

        geckoView = binding.geckoView

        runtime = GeckoRuntime.create(requireContext())

        session = GeckoSession().apply {
            open(runtime)
            loadUri("https://www.google.com")
        }

        geckoView.setSession(session)

        binding.searchInput.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_GO || isEnter) {
                val q = binding.searchInput.text.toString().trim()
                if (q.isNotEmpty()) {
                    val url = if (isUrl(q)) {
                        if (q.startsWith("http")) q else "https://$q"
                    } else {
                        "https://www.google.com/search?q=" + Uri.encode(q)
                    }
                    session.loadUri(url)
                }
                true
            } else false
        }
    }

    private fun isUrl(s: String): Boolean {
        if (s.startsWith("http://") || s.startsWith("https://")) return true
        if (s.contains(" ")) return false
        val first = s.split("/")[0]
        return first.matches(
            Regex("^[a-z0-9.-]+\\.[a-z]{2,}(:[0-9]+)?$", RegexOption.IGNORE_CASE)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        session.close()
        _binding = null
    }
}
