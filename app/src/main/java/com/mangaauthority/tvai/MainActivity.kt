package com.mangaauthority.tvai

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mangaauthority.tvai.ui.ChatViewModel
import com.mangaauthority.tvai.ui.SettingsViewModel
import com.mangaauthority.tvai.ui.TvAiApp
import com.mangaauthority.tvai.ui.theme.TvAiTheme

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uiMode = getSystemService(UI_MODE_SERVICE) as UiModeManager
        val isTv = uiMode.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        setContent {
            TvAiTheme(isTv = isTv) {
                TvAiApp(chatVm = chatViewModel, settingsVm = settingsViewModel, isTv = isTv)
            }
        }
    }
}
