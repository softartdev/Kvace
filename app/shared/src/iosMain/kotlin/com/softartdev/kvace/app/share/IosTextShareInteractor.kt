package com.softartdev.kvace.app.share

import com.softartdev.kvace.core.presentation.TextShareInteractor
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

class IosTextShareInteractor : TextShareInteractor {

    override fun shareText(text: String) {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootViewController != null) {
            val activityController = UIActivityViewController(
                activityItems = listOf(text),
                applicationActivities = null,
            )
            rootViewController.presentViewController(
                viewControllerToPresent = activityController,
                animated = true,
                completion = null,
            )
            return
        }
        UIPasteboard.generalPasteboard.string = text
    }
}
