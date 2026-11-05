package com.droidpilot.sampletarget

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ItemListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_list)

        val items = mutableListOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
        val adapter = ItemAdapter(items) { item ->
            AlertDialog.Builder(this)
                .setMessage("Detail: $item")
                .setPositiveButton("OK", null)
                .show()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rvItems)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val btnOpenWebView = findViewById<Button>(R.id.btnOpenWebView)
        btnOpenWebView.setOnClickListener {
            startActivity(Intent(this, WebViewActivity::class.java))
        }
    }
}
