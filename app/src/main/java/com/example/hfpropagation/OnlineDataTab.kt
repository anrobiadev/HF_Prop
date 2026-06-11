package com.example.hfpropagation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OnlineDataTab(s: AppStrings) {
    val context = LocalContext.current
    val isConnected = remember { isOnline(context) }

    // Salvează starea internă a WebView-ului (istoric, scroll, cache)
    val webViewStateBundle = rememberSaveable { Bundle() }

    // Păstrăm informația despre ultima actualizare
    var lastUpdateText by rememberSaveable { mutableStateOf("") }
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

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
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isConnected || !webViewStateBundle.isEmpty) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        setBackgroundColor(0)
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // Salvăm starea în bundle imediat ce s-a încărcat
                                lastUpdateText = dateFormatter.format(Date())
                                saveState(webViewStateBundle)
                            }
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true

                            // Optimizare Cache pentru persistență
                            domStorageEnabled = true
                            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                        }

                        // Dacă avem stare salvată, o restaurăm, altfel încărcăm HTML-ul
                        if (!webViewStateBundle.isEmpty) {
                            restoreState(webViewStateBundle)
                        } else {
                            val htmlData = """
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
                                    <style>
                                        body { margin:0; padding:0; display:flex; flex-direction: column; justify-content: flex-start; align-items: center; background-color:transparent; }
                                        img { max-width: 100%; height: auto; margin-bottom: 15px; }
                                    </style>
                                </head>
                                <body>
                                    <img src="https://www.hamqsl.com/solar101vhfper.php">
                                    <img src="https://www.hamqsl.com/solar101pic.php">
                                    <img src="https://www.hamqsl.com/solarmuf.php">
                                </body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
                        }
                    }
                },
                update = { webView ->

                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = s.noInternetTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
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

        Spacer(modifier = Modifier.height(16.dp))

        if (lastUpdateText.isNotEmpty()) {
            Text(
                text = s.lastUpdate + " $lastUpdateText",
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
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}