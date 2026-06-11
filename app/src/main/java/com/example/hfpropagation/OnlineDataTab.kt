package com.example.hfpropagation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OnlineDataTab(s: AppStrings) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Connection and loading state
    var isConnected    by remember { mutableStateOf(isOnline(context)) }
    var isLoading      by remember { mutableStateOf(false) }
    var showCachedNote by remember { mutableStateOf(false) }
    var htmlContent    by remember { mutableStateOf("") }
    var lastUpdateText by remember {
        mutableStateOf(StorageUtils.formatOnlineUpdateTime(
            StorageUtils.loadOnlineImagesTimestamp(context)))
    }

    // Decide what to show on first composition
    LaunchedEffect(Unit) {
        isConnected = isOnline(context)
        when {
            isConnected -> {
                // Download fresh images in background, then cache them
                isLoading = true
                withContext(Dispatchers.IO) {
                    val ok = StorageUtils.downloadAndCacheImages(context)
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        htmlContent = if (ok) {
                            lastUpdateText = StorageUtils.formatOnlineUpdateTime(
                                StorageUtils.loadOnlineImagesTimestamp(context))
                            showCachedNote = false
                            StorageUtils.buildCachedHtml(context)
                        } else if (StorageUtils.hasCachedImages(context)) {
                            // Download failed but we have a cache
                            showCachedNote = true
                            StorageUtils.buildCachedHtml(context)
                        } else {
                            // No cache, no fresh data
                            ""
                        }
                    }
                }
            }
            StorageUtils.hasCachedImages(context) -> {
                // Offline but cached images available
                showCachedNote = true
                htmlContent = StorageUtils.buildCachedHtml(context)
            }
            else -> {
                // Offline and no cache
                htmlContent = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = s.titlu,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Cached images note banner
        if (showCachedNote) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = s.cachedImagesNote,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // WebView with cached or live content
        if (htmlContent.isNotEmpty() && !isLoading) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(0)
                        webViewClient = object : WebViewClient() {}
                        settings.apply {
                            javaScriptEnabled   = false  // not needed for local images
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            useWideViewPort     = true
                            loadWithOverviewMode = true
                            allowFileAccess     = true   // needed for file:// URLs
                            cacheMode           = WebSettings.LOAD_NO_CACHE
                        }
                        loadDataWithBaseURL(
                            "file://", htmlContent, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL(
                        "file://", htmlContent, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(620.dp)
            )
        }

        // No content at all
        if (htmlContent.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = s.noInternetTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = s.noInternetMessage,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Last update timestamp
        if (lastUpdateText.isNotEmpty() && lastUpdateText != "—") {
            Text(
                text = "${s.lastUpdate} $lastUpdateText",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Text(
            text = s.copyRight,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}