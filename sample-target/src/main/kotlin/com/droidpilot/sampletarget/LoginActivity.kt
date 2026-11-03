package com.droidpilot.sampletarget

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.username)
        val etPassword = findViewById<EditText>(R.id.password)
        val tvError = findViewById<TextView>(R.id.tvLoginError)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()
            if (isValidCredentials(username, password)) {
                tvError.visibility = View.GONE
                startActivity(Intent(this, ItemListActivity::class.java))
            } else {
                tvError.text = getString(R.string.error_invalid_credentials)
                tvError.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        fun isValidCredentials(username: String, password: String): Boolean =
            username.isNotEmpty() && password.isNotEmpty() &&
                username == "qa_user" && password == "secret"
    }
}
