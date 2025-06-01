package com.example.canstone2

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object UtilNotifier {
    fun showMessageShort(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    fun showMessageLong(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}