package com.wolf.browser

import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

class TabsManager(private val runtime: GeckoRuntime) {
    data class Tab(val id: Long, val session: GeckoSession, var title: String = "تبويب", var url: String = "")
    private val tabs = mutableListOf<Tab>()
    private var activeIndex = 0
    private var nextId = 1L

    fun createTab(url: String = "resource://android/assets/home.html"): Tab {
        val s = GeckoSession()
        s.open(runtime)
        val t = Tab(nextId++, s, "تبويب", url)
        tabs.add(t)
        activeIndex = tabs.size - 1
        s.loadUri(url)
        return t
    }
    fun closeTab(i: Int) {
        if(i < 0 || i >= tabs.size || tabs.size <= 1) return
        try { tabs.removeAt(i).session.close() } catch(e: Exception) {}
        if(activeIndex >= tabs.size) activeIndex = tabs.size - 1
    }
    fun activeTab(): Tab? = tabs.getOrNull(activeIndex)
    fun allTabs(): List<Tab> = tabs.toList()
    fun count(): Int = tabs.size
    fun activeIndex(): Int = activeIndex
    fun setActive(i: Int): Tab? { if(i in 0 until tabs.size){ activeIndex = i; return tabs[i] }; return null }
    fun updateTab(t: Tab, title: String? = null, url: String? = null) {
        title?.let { t.title = if(it.isBlank()) "تبويب" else it }
        url?.let { t.url = it }
    }
}
