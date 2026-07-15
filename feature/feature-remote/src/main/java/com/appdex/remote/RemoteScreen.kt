package com.appdex.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdex.remote.ftp.FtpClientScreen
import com.appdex.remote.server.WebServerScreen
import com.appdex.ui.components.AppDexBar
import com.appdex.ui.components.AppDexTabRow
import com.appdex.ui.theme.*

@Composable
fun RemoteScreen(
    onBack: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Web 服务" to Icons.Default.Cloud, "FTP 客户端" to Icons.Default.FolderShared)

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppDexBar(title = "远程管理", back = true, onBack = onBack)

            AppDexTabRow(
                selectedTabIndex = selectedTab,
                tabs = tabs,
                onTabSelected = { selectedTab = it }
            )

            when (selectedTab) {
                0 -> WebServerScreen()
                1 -> FtpClientScreen()
            }
        }
    }
}
