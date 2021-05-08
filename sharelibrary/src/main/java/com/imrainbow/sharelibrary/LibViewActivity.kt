package com.imrainbow.sharelibrary

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LibViewActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "LibView"
            setOnClickListener {
//                startActivity(
//                    Intent(
//                        this@LibViewActivity,
//                        TestActivity::class.java
//                    )
//                )
            }
        })
    }
}