package com.wolf.browser

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.wolf.browser.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var browserFragment: BrowserFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            browserFragment = BrowserFragment()
            supportFragmentManager.beginTransaction()
                .replace(binding.container.id, browserFragment!!)
                .commit()
        } else {
            browserFragment = supportFragmentManager
                .findFragmentById(binding.container.id) as? BrowserFragment
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val frag = browserFragment ?: return
                if (frag.canGoBack()) {
                    frag.goBack()
                } else {
                    // في نهاية السجل — الخروج
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}
