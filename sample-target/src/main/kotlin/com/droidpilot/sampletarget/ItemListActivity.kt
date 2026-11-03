package com.droidpilot.sampletarget

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ItemListActivity : AppCompatActivity() {

    private lateinit var adapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_list)

        val items = mutableListOf("Item 1", "Item 2", "Item 3")
        adapter = ItemAdapter(items)

        val recyclerView = findViewById<RecyclerView>(R.id.rv_items)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener { showAddDialog() }
    }

    private fun showAddDialog() {
        val input = EditText(this)
        input.contentDescription = "item name input"
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_title)
            .setView(input)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                val name = input.text.toString()
                if (name.isNotBlank()) {
                    adapter.addItem(name)
                    startActivity(Intent(this, WebViewActivity::class.java))
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
