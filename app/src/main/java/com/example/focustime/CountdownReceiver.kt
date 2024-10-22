package com.example.focustime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class CountdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            Toast.makeText(it, "Task deadline has passed!", Toast.LENGTH_LONG).show()
        }
    }
}
