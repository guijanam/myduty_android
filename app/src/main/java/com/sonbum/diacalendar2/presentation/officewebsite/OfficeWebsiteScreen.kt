package com.sonbum.diacalendar2.presentation.officewebsite

import android.annotation.SuppressLint
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.sonbum.diacalendar2.LocalScaffoldPaddingValues
import com.sonbum.diacalendar2.data.local.OfficeWebsiteRegistry
import org.koin.compose.koinInject

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeWebsiteScreen(
	url: String,
	officeName: String,
	onBack: () -> Unit,
	isTab: Boolean = false,
) {
	val registry: OfficeWebsiteRegistry = koinInject()
	val variantUrls = remember(officeName) { registry.getVariantUrls(officeName) }
	val dayUrl = variantUrls["day"]
	val monthUrl = variantUrls["month"]

	var webView by remember { mutableStateOf<WebView?>(null) }
	var isLoading by remember { mutableStateOf(true) }
	var currentLabel by remember {
		mutableStateOf<String?>(
			when (url) {
				dayUrl -> "day"
				monthUrl -> "month"
				else -> null
			}
		)
	}

	BackHandler(enabled = webView?.canGoBack() == true) {
		webView?.goBack()
	}
	// 탭 모드가 아닐 때만 WebView 히스토리 없으면 onBack 호출
	BackHandler(enabled = !isTab && webView?.canGoBack() != true) {
		onBack()
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(officeName.ifBlank { "승무소 사이트" }) },
				navigationIcon = {
					if (!isTab) {
						IconButton(onClick = onBack) {
							Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
						}
					}
				},
				actions = {
					IconButton(onClick = { webView?.reload() }) {
						Icon(Icons.Default.Refresh, contentDescription = "새로고침")
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = MaterialTheme.colorScheme.surface
				)
			)
		}
	) { padding ->
		val bottomNavPadding = if (isTab) LocalScaffoldPaddingValues.current.calculateBottomPadding() else 0.dp
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.padding(bottom = bottomNavPadding)
		) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
			) {
				AndroidView(
					modifier = Modifier.fillMaxSize(),
					factory = { context ->
						WebView(context).apply {
							layoutParams = ViewGroup.LayoutParams(
								ViewGroup.LayoutParams.MATCH_PARENT,
								ViewGroup.LayoutParams.MATCH_PARENT
							)
							webViewClient = object : WebViewClient() {
								override fun onPageStarted(
									view: WebView?,
									url: String?,
									favicon: android.graphics.Bitmap?
								) {
									isLoading = true
								}

								override fun onPageFinished(view: WebView?, url: String?) {
									isLoading = false
								}

								override fun shouldOverrideUrlLoading(
									view: WebView?,
									request: WebResourceRequest?
								): Boolean {
									val uri = request?.url ?: return false
									return when (uri.scheme?.lowercase()) {
										"tel", "sms", "smsto", "mailto" -> {
											val intent = Intent(Intent.ACTION_VIEW, uri).apply {
												addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
											}
											runCatching { context.startActivity(intent) }
											true
										}
										else -> false
									}
								}
							}
							webChromeClient = WebChromeClient()
							settings.apply {
								javaScriptEnabled = true
								domStorageEnabled = true
								loadWithOverviewMode = true
								useWideViewPort = true
								mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
								cacheMode = WebSettings.LOAD_DEFAULT
								javaScriptCanOpenWindowsAutomatically = true
								setSupportMultipleWindows(false)
								setSupportZoom(true)
								builtInZoomControls = true
								displayZoomControls = false
								layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
								userAgentString =
									"Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
									"AppleWebKit/537.36 (KHTML, like Gecko) " +
									"Chrome/120.0.0.0 Mobile Safari/537.36"
							}
							loadUrl(url)
							webView = this
						}
					},
				)

				if (isLoading) {
					LinearProgressIndicator(
						modifier = Modifier
							.fillMaxWidth()
							.align(Alignment.TopCenter),
					)
				}
			}

//			if (variantUrls.isNotEmpty()) {
//				Row(
//					modifier = Modifier
//						.fillMaxWidth()
//						.padding(horizontal = 5.dp, vertical = 4.dp),
//					horizontalArrangement = Arrangement.SpaceEvenly
//				) {
//					Button(
//						onClick = {
//							val target = dayUrl ?: return@Button
//							currentLabel = "day"
//							webView?.loadUrl(target)
//						},
//						modifier = Modifier
//							.weight(1f)
//							.height(50.dp)
//							.padding(4.dp),
//						shape = RoundedCornerShape(12.dp),
//						colors = ButtonDefaults.buttonColors(
//							containerColor = if (currentLabel == "day") Color.Blue else Color.Gray,
//							contentColor = Color.White
//						),
//						enabled = dayUrl != null
//					) {
//						Text("Day", fontSize = 18.sp, fontWeight = FontWeight.Bold)
//					}
//
//					Button(
//						onClick = {
//							val target = monthUrl ?: return@Button
//							currentLabel = "month"
//							webView?.loadUrl(target)
//						},
//						modifier = Modifier
//							.weight(1f)
//							.height(50.dp)
//							.padding(4.dp),
//						shape = RoundedCornerShape(12.dp),
//						colors = ButtonDefaults.buttonColors(
//							containerColor = if (currentLabel == "month") Color.Blue else Color.Gray,
//							contentColor = Color.White
//						),
//						enabled = monthUrl != null
//					) {
//						Text("Month", fontSize = 18.sp, fontWeight = FontWeight.Bold)
//					}
//				}
//			}
		}
	}
}
