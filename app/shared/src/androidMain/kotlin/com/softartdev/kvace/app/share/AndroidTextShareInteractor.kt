package com.softartdev.kvace.app.share

import android.content.Context
import android.content.Intent
import com.softartdev.kvace.core.presentation.TextShareInteractor

class AndroidTextShareInteractor(private val context: Context) : TextShareInteractor {

    override fun shareText(text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        val chooser = Intent.createChooser(sendIntent, null)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
