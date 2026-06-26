package com.softartdev.kvace.app.share

import com.softartdev.kvace.core.presentation.TextShareInteractor
import kotlin.JsFun
import kotlin.js.ExperimentalWasmJsInterop

class WasmTextShareInteractor : TextShareInteractor {

    override fun shareText(text: String) {
        shareOrCopyText(text)
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(text) => {
        if (globalThis.navigator && globalThis.navigator.share) {
            globalThis.navigator.share({ text });
            return;
        }
        if (globalThis.navigator && globalThis.navigator.clipboard) {
            globalThis.navigator.clipboard.writeText(text);
            return;
        }
    }"""
)
private external fun shareOrCopyText(text: String)
