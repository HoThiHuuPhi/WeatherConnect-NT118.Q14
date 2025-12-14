package com.example.doanck.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doanck.R

enum class MainTab {
    WEATHER,
    COMMUNITY,
    SEARCH,
    SETTINGS
}

private val GlassDark = Color(0xFF020617).copy(alpha = 0.55f)   // navy rất đậm + alpha
private val GlassBorder = Color.White.copy(alpha = 0.45f)

@Composable
fun MainTopNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onOpenWeatherMap: () -> Unit,
    onOpenRescueMap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        var mapMenuExpanded by remember { mutableStateOf(false) }

        Box {
            Surface(
                onClick = { mapMenuExpanded = true },
                shape = CircleShape,
                color = GlassDark,                               // 🔵 cùng tông
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.map),
                        contentDescription = "Bản đồ",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = mapMenuExpanded,
                onDismissRequest = { mapMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Bản đồ thời tiết") },
                    onClick = {
                        mapMenuExpanded = false
                        onOpenWeatherMap()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Bản đồ cứu trợ") },
                    onClick = {
                        mapMenuExpanded = false
                        onOpenRescueMap()
                    }
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(60.dp),
            shape = RoundedCornerShape(999.dp),
            color = GlassDark,                               // 🔵 glass tối
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavPillItem(
                    iconRes = R.drawable.ic_tab_weather,
                    label = "Thời tiết",
                    modifier = Modifier.weight(1f)
                ) { onTabSelected(MainTab.WEATHER) }

                NavPillItem(
                    iconRes = R.drawable.ic_tab_chat,
                    label = "Chat",
                    modifier = Modifier.weight(1f)
                ) { onTabSelected(MainTab.COMMUNITY) }

                NavPillItem(
                    iconRes = R.drawable.ic_tab_search,
                    label = "Tìm kiếm",
                    modifier = Modifier.weight(1f)
                ) { onTabSelected(MainTab.SEARCH) }

                NavPillItem(
                    iconRes = R.drawable.ic_tab_settings,
                    label = "Cài đặt",
                    modifier = Modifier.weight(1f)
                ) { onTabSelected(MainTab.SETTINGS) }
            }
        }
    }
}



@Composable
private fun NavPillItem(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(999.dp)),
        color = Color.Transparent,
        onClick = onClick,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.96f),   // 🔥 gần trắng
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


@Preview(
    showBackground = true,
    backgroundColor = 0xFF37474F,
    widthDp = 390,
    heightDp = 200
)
@Composable
fun MainBottomBarPreview() {
    var selectedTab by remember { mutableStateOf(MainTab.WEATHER) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF455A64))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Nội dung màn chính (Weather / Search / Settings...)", color = Color.White)
            }

            MainTopNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onOpenWeatherMap = {},
                onOpenRescueMap = {}
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}