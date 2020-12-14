package com.apolis.wenzhao.reminderapplication.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.apolis.wenzhao.reminderapplication.R

class ConfirmationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)

        val buttonClose: Button = findViewById(R.id.button_close)
        buttonClose.setOnClickListener {
            finish()
        }
    }
}