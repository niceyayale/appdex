package com.appdex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdex.ui.theme.AppDexTheme
import com.appdex.ui.theme.*

data class AppDexNavItem(
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppDexBottomNav(
    items: List<AppDexNavItem>,
    currentRoute: String,
    onNavigate: (Int) -> Unit
) {
    val c = AppDexTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(76.dp)
            .background(c.surfaceDeep)
            .padding(horizontal = 8.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == items.indexOfFirst { it.label == currentRoute }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate(index) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(56.dp)
                        .background(if (selected) c.amberGoldSelectedBg else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(21.dp),
                        tint = if (selected) c.amberGoldHighlight else c.textTertiary
                    )
                }
                Text(
                    text = item.label,
                    fontSize = 10.sp,
                    color = if (selected) c.amberGoldHighlight else c.textTertiary
                )
            }
        }
    }
}
