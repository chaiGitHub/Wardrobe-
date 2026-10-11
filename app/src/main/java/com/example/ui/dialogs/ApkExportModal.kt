package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.ui.components.ScrollableButtonText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.io.File

@Composable
fun ApkExportModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("apk_export_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "APK 安装包与下载",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Android Package Manager & Download",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                HorizontalDivider(color = Slate200)

                // Status Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BrandEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "安装包已就绪 · Ready to Install",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "app-debug.apk (~25 MB) · Debug Signed",
                                fontSize = 11.sp,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }

                // File Details Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFAF5FF),
                    border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("文件名称:", fontSize = 11.sp, color = Slate600)
                            Text("app-debug.apk", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("应用包名:", fontSize = 11.sp, color = Slate600)
                            Text(context.packageName, fontSize = 11.sp, color = Slate700, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("文件大小:", fontSize = 11.sp, color = Slate600)
                            Text("~25 MB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandEmerald)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("存放位置:", fontSize = 11.sp, color = Slate600)
                            Text("项目根目录 (Root Directory)", fontSize = 11.sp, color = Slate700)
                        }
                    }
                }

                // Primary Action Button: Export / Share APK
                Button(
                    onClick = {
                        try {
                            val srcApk = File(context.applicationInfo.sourceDir)
                            val dest = File(context.cacheDir, "app-debug.apk")
                            srcApk.copyTo(dest, overwrite = true)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.android.package-archive"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Smart Wardrobe App APK")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "分享 / 保存 APK 安装包"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "提取 APK 失败: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("modal_export_apk_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    ScrollableButtonText("📲 导出 / 分享 APK (Save / Share APK)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Secondary Action Button: Copy file path
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("APK Path", "app-debug.apk")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制文件名！在 AI Studio 左侧文件栏右键 'app-debug.apk' 点击 Download 即可。", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("modal_copy_apk_path_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    ScrollableButtonText("📋 复制文件名 (app-debug.apk)", fontSize = 12.sp, color = Slate800, fontWeight = FontWeight.SemiBold)
                }

                // AI Studio Download Guide
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "💡 如何在 Google AI Studio 中下载 APK：",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "1. 在 AI Studio 左侧文件树中，直接找到根目录下的 app-debug.apk 文件",
                            fontSize = 11.sp,
                            color = Slate600,
                            lineHeight = 15.sp
                        )
                        Text(
                            text = "2. 鼠标右键点击该文件，选择「Download」即可直接下载到手机或电脑",
                            fontSize = 11.sp,
                            color = Slate600,
                            lineHeight = 15.sp
                        )
                        Text(
                            text = "3. 或点击 AI Studio 右上角的设置/齿轮图标 ->「Download APK」",
                            fontSize = 11.sp,
                            color = Slate600,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "⚡【免频繁自动打包已开启】：后续每次对话调整功能时，不会再自动耗时重新编译 APK。只有当您明确要求「生成/打包 APK」时才会为您编译，大幅节省等待时间！",
                            fontSize = 11.sp,
                            color = BrandPurple,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text("关闭 (Close)", fontSize = 12.sp, color = Slate600)
                }
            }
        }
    }
}
