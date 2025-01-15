package com.github.aar0u.quickhub.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.aar0u.quickhub.model.Config
import com.github.aar0u.quickhub.service.HttpService
import com.github.aar0u.android.ui.theme.Theme1
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Environment
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private const val tag = "quick-hub"

class MainActivity : ComponentActivity() {
    private val logViewModel: WorkerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Theme1 {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column1(
                        modifier = Modifier.padding(innerPadding),
                        logText = logViewModel.logText.value,
                        onCheckStoragePermissions = { checkStoragePermissions() },
                        onRequestStoragePermissions = { requestStoragePermissions() }
                    )
                }
            }
        }
        logViewModel.startLogCapture()
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1
            )
        }
    }

    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permissions are granted by default on older Android versions
        }
    }
}

@Composable
fun Column1(
    modifier: Modifier = Modifier,
    logText: String,
    onCheckStoragePermissions: () -> Boolean,
    onRequestStoragePermissions: () -> Unit = {},
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = {
                if (onCheckStoragePermissions()) {
                    Log.i(tag, "Starting HTTP server")
                    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                        HttpService(
                            Config(
                                workingDir = Environment.getExternalStorageDirectory().absolutePath,
                                port = 3000
                            ), listener = { file ->
                                if (file.path.endsWith(".apk")) {
                                    val apkUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.FileProvider",
                                        file
                                    )
                                    Log.i(tag, "Install APK: $file")
                                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        setDataAndType(
                                            apkUri,
                                            "application/vnd.android.package-archive"
                                        )
                                    }
                                    context.startActivity(installIntent)
                                }
                            }
                        ).start()
                    }
                } else {
                    onRequestStoragePermissions()
                }
            },
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.Blue),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
            )
        }
        AutoScrollingLog(
            logText = logText.lines()
        )
    }
}

@Composable
fun AutoScrollingLog(logText: List<String>) {
    val listState = rememberLazyListState()

    LaunchedEffect(logText.size) {
        listState.animateScrollToItem(logText.size - 1)
    }

    LazyColumn(
        state = listState,
    ) {
        itemsIndexed(logText) { index, log ->
            val rowColor =
                if (log.lowercase().contains("error") or log.lowercase()
                        .contains("exception")
                ) {
                    Color.Red
                } else if (index % 2 == 0) {
                    Color.LightGray // Even rows
                } else {
                    Color.White // Odd rows
                }
            Text(
                log,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 3.dp)
                    .background(rowColor),
                fontSize = 12.sp
            )
        }
    }
}

class WorkerViewModel : ViewModel() {
    var logText = mutableStateOf("")
    private var logJob: Job? = null

    fun startLogCapture() {
        logJob?.cancel() // 防止重复任务
        logJob = viewModelScope.launch(Dispatchers.IO) {
            val process = ProcessBuilder(
                "logcat",
                "-T", "0", // 从头开始读取
                "-v", "time", // 使用时间戳格式
                "-s", "quick-hub:V"
            ).start()
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.contains("beginning of main")) return@forEach
                    val cleanedLine =
                        line.substringBefore(".") + " " + line.substringAfter("):")
                    withContext(Dispatchers.Main) {
                        logText.value += "$cleanedLine\n"
                    }
                }
            }
        }
    }

    override fun onCleared() {
        logJob?.cancel() // ViewModel销毁时停止日志
    }
}

@Preview(showBackground = true)
@Composable
fun Column1Preview() {
    Theme1 {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column1(
                modifier = Modifier.padding(innerPadding),
                logText = """
        This is a very long text that will demonstrate the scrolling behavior.
        It should be long enough to go beyond the screen's boundaries.
        This is a very long text that will demonstrate the scrolling behavior.
        It should be long enough to go beyond the screen's boundaries.
        This is a very long text that will demonstrate the scrolling behavior.
        It should be long enough to go beyond the screen's boundaries.
        This is a very long text that will demonstrate the scrolling behavior.
        It should be long enough to go beyond the screen's boundaries.
        This is a very long text that will demonstrate the scrolling behavior.
        It should be long enough to go beyond the screen's boundaries.
        This is a very long text that will demonstrate the scrolling behavior.
        It should be long enough to go beyond the screen's boundaries.
        This is a very long text that will demonstrate the scrolling behavior.
        It should be long enough to go beyond the screen's boundaries.
        This is a very long text that will demonstrate the scrolling behavior.
        It should be long enough to go beyond the screen's boundaries.
        This is a very long text that will demonstrate the scrolling behavior.
        It should be long enough to go beyond the screen's boundaries.
    """.trimIndent(),
                onCheckStoragePermissions = { true }
            )
        }
    }
}
