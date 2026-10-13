package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.AmapClient
import com.example.location.LocationHelper
import kotlinx.coroutines.launch

data class PoiItem(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

private val PRESET_POIS = listOf(
    PoiItem("市中心国际会议中心 A座", "人民大道200号", 31.2304, 121.4737),
    PoiItem("虹桥国际机场 T2航站楼", "申达一路1号", 31.1979, 121.3363),
    PoiItem("陆家嘴上海中心大厦", "银城中路501号", 31.2355, 121.5015),
    PoiItem("浦东国际机场 T1航站楼", "迎宾大道6000号", 31.1443, 121.8083),
    PoiItem("张江高科技园区 展想广场", "祖冲之路2290号", 31.2065, 121.6033),
    PoiItem("北京大兴国际机场", "大兴区礼贤镇", 39.5098, 116.4105),
    PoiItem("首都国际机场 T3", "顺义区机场南路", 40.0799, 116.6031),
    PoiItem("深圳南山科技园 腾讯大厦", "深南大道10000号", 22.5407, 113.9344),
    PoiItem("杭州西溪湿地 阿里巴巴园区", "文一西路969号", 30.2797, 120.0253),
    PoiItem("广州珠江新城 IFC国金中心", "珠江西路5号", 23.1192, 113.3213)
)

@Composable
fun LocationPickerContent(
    initialQuery: String = "",
    onSelectLocation: (name: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var query by remember { mutableStateOf(initialQuery) }
    var isLoading by remember { mutableStateOf(false) }
    var poiList by remember { mutableStateOf<List<PoiItem>>(PRESET_POIS) }

    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    val amapKey = prefs.getString("amap_api_key", null)

    fun performSearch(text: String) {
        if (text.isBlank()) {
            poiList = PRESET_POIS
            return
        }

        coroutineScope.launch {
            isLoading = true
            val filteredPreset = PRESET_POIS.filter {
                it.name.contains(text, ignoreCase = true) || it.address.contains(text, ignoreCase = true)
            }.toMutableList()

            // Try Amap Web API if key is available
            if (!amapKey.isNullOrBlank()) {
                try {
                    val res = AmapClient.api.searchPoi(keywords = text, key = amapKey)
                    val apiPois = res.pois?.mapNotNull { p ->
                        val parts = p.location?.split(",")
                        if (parts != null && parts.size == 2) {
                            val lng = parts[0].toDoubleOrNull()
                            val lat = parts[1].toDoubleOrNull()
                            if (lat != null && lng != null) {
                                PoiItem(
                                    name = p.name,
                                    address = p.address ?: p.cityname ?: "",
                                    latitude = lat,
                                    longitude = lng
                                )
                            } else null
                        } else null
                    }
                    if (!apiPois.isNullOrEmpty()) {
                        filteredPreset.addAll(0, apiPois)
                    }
                } catch (_: Exception) {
                    // Fall back to filtered preset
                }
            }

            // Always offer custom entry with user current GPS if query is non-empty
            val currentLoc = LocationHelper.getLastKnownLocation(context)
            filteredPreset.add(
                0,
                PoiItem(
                    name = text,
                    address = "自定义地址（匹配坐标）",
                    latitude = currentLoc?.latitude ?: 31.2304,
                    longitude = currentLoc?.longitude ?: 121.4737
                )
            )

            poiList = filteredPreset.distinctBy { it.name }
            isLoading = false
        }
    }

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            performSearch(query)
        } else {
            poiList = PRESET_POIS
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择待办地点",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Field
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("poi_search_input"),
            placeholder = { Text("搜索高德 POI 地点、车站、商圈或直接输入...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "清除")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick button to use current GPS location
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val currentLoc = LocationHelper.getLastKnownLocation(context)
                    if (currentLoc != null) {
                        onSelectLocation("当前位置 (GPS)", currentLoc.latitude, currentLoc.longitude)
                    } else {
                        onSelectLocation("当前位置 (默认)", 31.2304, 121.4737)
                    }
                },
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "使用当前位置作为地点",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "自动读取系统 GPS 经纬度",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (query.isBlank()) "常见热门推荐地点" else "搜索结果",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(poiList) { poi ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectLocation(poi.name, poi.latitude, poi.longitude)
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = poi.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (poi.address.isNotBlank()) {
                                    Text(
                                        text = poi.address,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "坐标: ${"%.4f".format(poi.latitude)}, ${"%.4f".format(poi.longitude)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
