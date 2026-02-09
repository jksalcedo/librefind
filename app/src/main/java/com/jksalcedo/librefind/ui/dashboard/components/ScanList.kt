package com.jksalcedo.librefind.ui.dashboard.components

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.jksalcedo.librefind.domain.model.AppItem
import com.jksalcedo.librefind.domain.model.AppStatus
import com.jksalcedo.librefind.ui.common.StatusBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppIconCache {
    private const val MAX_CACHE_SIZE = 50 * 1024 * 1024

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_SIZE) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    fun get(packageName: String): Bitmap? = cache.get(packageName)

    fun put(packageName: String, bitmap: Bitmap) {
        cache.put(packageName, bitmap)
    }

    fun getCacheSize(): Long {
        return cache.size().toLong()
    }

    fun clearCache() {
        cache.evictAll()
    }
}

@Composable
fun ScanList(
    apps: List<AppItem>,
    onAppClick: (String, String) -> Unit,
    onIgnoreClick: (String) -> Unit,
    onRestoreClick: (String) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null
) {
    val sortedApps = remember(apps) { apps.sortedBy { it.label } }

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (headerContent != null) {
                item(key = "header") {
                    headerContent()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            items(
                items = sortedApps,
                key = { it.packageName }
            ) { app ->
                AppRow(
                    app = app,
                    onClick = { onAppClick(app.label, app.packageName) },
                    onIgnoreClick = { onIgnoreClick(app.packageName) },
                    onRestoreClick = { onRestoreClick(app.packageName)},
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: AppItem,
    onClick: () -> Unit,
    onIgnoreClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AppIconAsync(
                packageName = app.packageName,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (app.knownAlternatives > 0) {
                    Text(
                        text = "${app.knownAlternatives} alternative${if (app.knownAlternatives > 1) "s" else ""} available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(status = app.status)

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (app.status != AppStatus.IGNORED) {
                    DropdownMenuItem(
                        text = { Text("Ignore") },
                        onClick = {
                            showMenu = false
                            onIgnoreClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null)
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Restore") },
                        onClick = {
                            showMenu = false
                            onRestoreClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppIconAsync(
    packageName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var iconBitmap by remember(packageName) { 
        mutableStateOf(AppIconCache.get(packageName))
    }
    var isLoading by remember(packageName) { 
        mutableStateOf(AppIconCache.get(packageName) == null)
    }

    LaunchedEffect(packageName) {
        if (iconBitmap == null) {
            iconBitmap = withContext(Dispatchers.IO) {
                try {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    val bitmap = drawable.toBitmap(
                        width = 120,
                        height = 120
                    )
                    AppIconCache.put(packageName, bitmap)
                    bitmap
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = "Loading",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            iconBitmap != null -> {
                Image(
                    bitmap = iconBitmap!!.asImageBitmap(),
                    contentDescription = "App Icon",
                    modifier = Modifier.size(40.dp)
                )
            }

            else -> {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = "Default Icon",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
