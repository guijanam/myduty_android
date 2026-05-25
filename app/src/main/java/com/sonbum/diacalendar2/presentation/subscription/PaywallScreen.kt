package com.sonbum.diacalendar2.presentation.subscription

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.PurchaseParams
import org.koin.androidx.compose.koinViewModel

private const val TERMS_URL = "https://blog.naver.com/developergui7/224253740394"
private const val PRIVACY_URL = "https://blog.naver.com/developergui7/224253752675"

@Composable
fun PaywallScreen(
    onSubscribed: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: PaywallViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is PaywallEvent.SubscriptionSuccess -> onSubscribed()
                is PaywallEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 커피 한 잔 감성 헤더 ---
//            Box(
//                modifier = Modifier
//                    .size(84.dp)
//                    .clip(RoundedCornerShape(24.dp)),
//                contentAlignment = Alignment.Center
//            ) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .clip(RoundedCornerShape(24.dp)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.LocalCafe,
//                        contentDescription = null,
//                        modifier = Modifier.size(72.dp),
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                }
//            }
//
//            Spacer(Modifier.height(10.dp))

            Text(
                text = "개발자에게 커피 한 잔 ☕",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "이 앱은 한 명의 개발자가 꾸준히 만들고 고치고 있어요.\n" +
                    "커피 한 잔 값의 작은 후원이 앱을 계속 살아 있게 합니다.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // --- 후원 시 제공되는 혜택 카드 ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "후원하면 이런 게 열려요",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    BenefitRow(
                        icon = Icons.Default.Groups,
                        title = "동료근무 기능",
                        description = "동료들의 근무를 그룹별로 달력에서 한눈에 확인"
                    )
                    BenefitRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "추후 프리미엄 기능",
                        description = "앞으로 추가되는 프리미엄 기능을 추가 비용 없이 모두 이용"
                    )
                    BenefitRow(
                        icon = Icons.Default.LocalCafe,
                        title = "개발자 응원",
                        description = "유지보수와 새 기능 개발을 이어 갈 힘이 됩니다"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "결제 안내",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    BillingStepRow(
                        step = "1",
                        text = "지금 가입하면 1개월간 무료로 모든 기능 이용"
                    )
                    BillingStepRow(
                        step = "2",
                        text = if (state.priceText.isNotEmpty()) {
                            "무료 1개월이 끝나면 ${state.priceText}/월 자동 결제 시작"
                        } else {
                            "무료 1개월이 끝나면 매월 자동 결제 시작"
                        }
                    )
                    BillingStepRow(
                        step = "3",
                        text = "무료 기간 중 해지하면 요금이 청구되지 않아요"
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            when {
                state.isLoading -> CircularProgressIndicator()
                else -> {
                    Button(
                        onClick = {
                            val pkg = state.availablePackage ?: return@Button
                            val activity = context as? Activity ?: return@Button
                            // RevenueCat이 자동으로 최적 offer(무료 체험 포함) 선택
                            val purchaseParams = PurchaseParams.Builder(activity, pkg).build()
                            Purchases.sharedInstance.purchase(
                                purchaseParams,
                                object : PurchaseCallback {
                                    override fun onCompleted(
                                        storeTransaction: StoreTransaction,
                                        customerInfo: com.revenuecat.purchases.CustomerInfo
                                    ) {
                                        if (customerInfo.entitlements["coffee"]?.isActive == true) {
                                            viewModel.onPurchaseSuccess()
                                        }
                                    }
                                    override fun onError(
                                        error: PurchasesError,
                                        userCancelled: Boolean
                                    ) {
                                        if (!userCancelled) {
                                            viewModel.onPurchaseError(error.message)
                                        }
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = state.availablePackage != null
                    ) {
                        Text(
                            text = "☕ 1개월 무료로 시작하기",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (state.priceText.isNotEmpty()) {
                            "지금은 0원 · 1개월 후 ${state.priceText}/월 자동 결제 · 언제든 해지 가능"
                        } else {
                            "지금은 0원 · 1개월 후 매월 자동 결제 · 언제든 해지 가능"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.restorePurchases() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isRestoring
                    ) {
                        if (state.isRestoring) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            }
                        } else {
                            Text("구매 복원")
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "언제든지 PLAY STORE 설정에서 구독을 취소할 수 있습니다.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "구독을 시작하면 아래 약관에 동의하는 것으로 간주됩니다.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            LegalLinksRow()

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onDismiss) {
                Text("다음에 사줄게요", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BillingStepRow(
    step: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegalLinksRow() {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary
    val textStyle = MaterialTheme.typography.bodyMedium

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL)))
            },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                text = "이용약관",
                style = textStyle,
                color = linkColor,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline
            )
        }

        Text(
            text = "·",
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TextButton(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
            },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                text = "개인정보 처리방침",
                style = textStyle,
                color = linkColor,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}
