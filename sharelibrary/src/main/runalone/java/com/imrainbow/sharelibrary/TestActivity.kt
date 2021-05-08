package com.imrainbow.sharelibrary

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "test activity"
            setOnClickListener {
                startActivity(
                    Intent(
                        this@TestActivity,
                        LibViewActivity::class.java
                    )
                )
            }
        })
    }

}