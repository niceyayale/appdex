package com.appdex.remote

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.appdex.remote.ftp.FtpClientScreen
import com.appdex.remote.server.WebServerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Web Server" to Icons.Default.Cloud, "FTP Client" to Icons.Default.FolderShared)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remote Management", style = MaterialTheme.typography.titleLarge) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, (title, icon) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        icon = { Icon(icon, contentDescription = null) }
                    )
                }
            }

            when (selectedTab) {
                0 -> WebServerScreen()
                1 -> FtpClientScreen()
            }
        }
    }
}
