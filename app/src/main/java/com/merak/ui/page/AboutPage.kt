package com.merak.ui.page

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.merak.ui.components.MiuixBackButton
import com.merak.x.BuildConfig
import com.merak.x.R
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * 应用信息卡片
 */
@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 28.sp,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MiuixTheme.colorScheme.primary,
                            MiuixTheme.colorScheme.secondary
                        )
                    ),
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${stringResource(R.string.about_version_label)} ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${stringResource(R.string.about_build_time_label)} ${
                    ""/*SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date(BuildConfig.BUILD_TIMESTAMP)
                    )*/
                }",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AboutPage(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.about_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MiuixBackButton(
                        modifier = Modifier.padding(start = 16.dp),
                        onClick = onBack
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .overScrollVertical()
        ) {
            item {
                AppInfoCard()
            }

            item {
                SmallTitle(stringResource(R.string.about_contributors))
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    SuperArrow(
                        title = "MerakXingChen",
                        summary = stringResource(R.string.about_developer),
                        startAction = {
                        },
                        onClick = {
                        }
                    )
                }
            }

            item {
                SmallTitle(stringResource(R.string.about_feedback))
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    SuperArrow(
                        title = stringResource(R.string.about_feedback),
                        summary = stringResource(R.string.about_feedback_desc),
                        onClick = {
                        }
                    )
                }
            }

            item {
                SmallTitle(stringResource(R.string.about_support))
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    val sponsorToast = stringResource(R.string.about_sponsor_toast)
                    SuperArrow(
                        title = stringResource(R.string.about_sponsor),
                        summary = stringResource(R.string.about_sponsor_summary),
                        onClick = {
                            Toast.makeText(
                                context,
                                sponsorToast,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            item {
                SmallTitle(stringResource(R.string.about_others))
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    SuperArrow(
                        title = stringResource(R.string.about_github),
                        summary = stringResource(R.string.about_github_desc),
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MerakXingChen/ThemeStore"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.about_open_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

