package com.softartdev.kvace.app.share

import com.softartdev.kvace.core.presentation.TextShareInteractor
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class JvmTextShareInteractor : TextShareInteractor {

    override fun shareText(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}
