package com.wolf.browser

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.wolf.browser.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var frag: BrowserFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            frag = BrowserFragment()
            supportFragmentManager.beginTransaction().replace(binding.container.id, frag!!).commit()
        } else {
            frag = supportFragmentManager.findFragmentById(binding.container.id) as? BrowserFragment
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val f = frag ?: run { isEnabled = false; onBackPressedDispatcher.onBackPressed(); return }
                if (f.canGoBack()) f.goBack()
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })
    }
}
