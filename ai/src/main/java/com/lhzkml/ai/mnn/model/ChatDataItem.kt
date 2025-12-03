package com.lhzkmlai.mnn.model

import android.net.Uri

class ChatDataItem {
    var loading: Boolean = false

    @JvmField
    var time: String? = null

    // @JvmField
    // var audioPlayComponent: AudioPlayerComponent? = null

    @JvmField
    var text: String? = null
    var type: Int
        private set

    @JvmField
    var imageUri: Uri? = null

    @JvmField
    var benchmarkInfo: String? = null

    var displayText: String? = null
        get() = field ?: ""

    var thinkingText: String? = null

    constructor(time: String?, type: Int, text: String?) {
        this.time = time
        this.type = type
        this.text = text
        this.displayText = text
    }

    constructor(type: Int) {
        this.type = type
    }

    var showThinking: Boolean = true

    var thinkingFinishedTime = -1L

    fun toggleThinking() {
        showThinking = !showThinking
    }

    companion object {
        fun createImageInputData(timeString: String?, text: String?, imageUri: Uri?): ChatDataItem {
            val result = ChatDataItem(timeString, ChatViewHolders.USER, text)
            result.imageUri = imageUri
            return result
        }
    }
}

object ChatViewHolders {
    const val HEADER: Int = 0
    const val ASSISTANT: Int = 1
    const val USER: Int = 2
}
