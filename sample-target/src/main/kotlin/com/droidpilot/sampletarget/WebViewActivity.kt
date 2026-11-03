package com.droidpilot.sampletarget

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        val webView = findViewById<WebView>(R.id.wv_main)
        webView.loadData(
            "<html><body><h1 id=\"title\">DroidPilot Web</h1>" +
                "<p id=\"content\">Sample web content for testing.</p></body></html>",
            "text/html",
            "utf-8"
        )
    }
}
