package com.wolf.browser

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.wolf.browser.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var frag: BrowserFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.container) { v, insets ->
            val sys = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(left = sys.left, top = sys.top, right = sys.right, bottom = sys.bottom)
            WindowInsetsCompat.CONSUMED
        }

        if (savedInstanceState == null) {
            frag = BrowserFragment()
            supportFragmentManager.beginTransaction()
                .replace(binding.container.id, frag!!)
                .commit()
        } else {
            frag = supportFragmentManager.findFragmentById(binding.container.id) as? BrowserFragment
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val f = frag
                if (f != null && f.canGoBack()) f.goBack()
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })
    }
}
