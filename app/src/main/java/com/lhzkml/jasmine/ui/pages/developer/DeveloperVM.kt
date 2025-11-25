package com.lhzkml.jasmine.ui.pages.developer

import androidx.lifecycle.ViewModel
import com.lhzkml.jasmine.data.ai.AILoggingManager

class DeveloperVM(
    private val aiLoggingManager: AILoggingManager
) : ViewModel() {
    val logs = aiLoggingManager.getLogs()
}
