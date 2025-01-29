package com.github.aar0u.quickhub.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.github.aar0u.android.ui.theme.Theme1
import com.github.aar0u.quickhub.android.HttpRunner.isBusy
import com.github.aar0u.quickhub.android.HttpRunner.isServerRunning
import com.github.aar0u.quickhub.android.HttpRunner.startServer
import com.github.aar0u.quickhub.android.HttpRunner.stopServer
import com.github.aar0u.quickhub.model.Config
import java.io.File

private const val tag = "quick-hub"
private val toggleState = mutableStateOf(false)

class MainActivity : ComponentActivity() {
    private val logViewModel: LogViewModel by viewModels()
    private lateinit var folderPickerLauncher: ActivityResultLauncher<Uri?>
    private lateinit var permissionLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Theme1 {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ComposeMainColumn1(modifier = Modifier.padding(innerPadding),
                        logList = logViewModel.logList,
                        onCheckStoragePermissions = { checkStoragePermissions() },
                        onRequestStoragePermissions = { requestStoragePermissions() },
                        onReceiveApk = { installApk(file = it) },
                        onOpenFolderPicker = { folderPickerLauncher.launch(null) },
                        onGetSerConfig = { getSerConfig(it) })
                }
            }
        }

        logViewModel.startLogCapture()
        initFolderPickerLauncher()
        initPermissionLauncher()
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            permissionLauncher.launch(intent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 1
            )
        }
    }

    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permissions are granted by default on older Android versions
        }
    }

    private fun installApk(file: File) {
        if (file.path.endsWith(".apk")) {
            val apkUri = FileProvider.getUriForFile(
                this, "${this.packageName}.FileProvider", file
            )
            Log.i(tag, "Install APK: $file")
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                setDataAndType(
                    apkUri, "application/vnd.android.package-archive"
                )
            }
            this.startActivity(installIntent)
        }
    }

    private fun initPermissionLauncher() {
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                Log.i(tag, "Activity result: $result")
                // Sync toggleState
                toggleState.value = checkStoragePermissions()
            }
    }

    private fun initFolderPickerLauncher() {
        folderPickerLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
                uri?.let {
                    val folderPath = getAbsolutePathFromUri(it)
                    Log.i(tag, "Root folder: $folderPath")
                    applicationContext.getSharedPreferences("_preferences", MODE_PRIVATE).edit()
                        .putString("path", folderPath).commit()
                }
            }
    }

    private fun getAbsolutePathFromUri(uri: Uri): String? {
        return uri.path?.let { it ->
            it.split(":").takeIf { it[0].endsWith("primary") }?.let {
                Environment.getExternalStorageDirectory().absolutePath + "/" + it[1]
            }
        }
    }

    private fun getSerConfig(toggleState: Boolean): Config? {
        val rootFolder =
            Environment.getExternalStorageDirectory().absolutePath.takeIf { toggleState }
                ?: applicationContext.getSharedPreferences("_preferences", MODE_PRIVATE)
                    .getString("path", null)
        return rootFolder?.let { Config(it, 3006, overwrite = true) }
    }
}

@Composable
fun ComposeMainColumn1(
    modifier: Modifier = Modifier,
    logList: List<String>,
    onCheckStoragePermissions: () -> Boolean = { true },
    onRequestStoragePermissions: () -> Unit = {},
    onReceiveApk: (File) -> Unit = {},
    onOpenFolderPicker: () -> Unit = {},
    onGetSerConfig: (Boolean) -> Config? = { null }
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val modifierBtn = Modifier
            .size(60.dp)
            .clip(CircleShape)

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    handleServerButtonClick(
                        toggleState.value, onGetSerConfig, onReceiveApk
                    )
                },
                modifier = modifierBtn,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(),
                enabled = !isBusy.value
            ) {
                Icon(
                    imageVector = if (isServerRunning.value) Icons.Rounded.Close else Icons.Rounded.PlayArrow,
                    contentDescription = if (isServerRunning.value) "Stop Server" else "Start Server",
                )
            }

            Button(
                onClick = onOpenFolderPicker,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(),
                enabled = !toggleState.value,
                modifier = modifierBtn,
            ) {
                Text("...")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = toggleState.value,
                    onCheckedChange = {

                        if (it && !onCheckStoragePermissions()) {
                            onRequestStoragePermissions()
                        }
                        toggleState.value = it
                    },
                )
                Text("SD Card")
            }
        }

        AutoScrollingLog(logList)
    }
}

private fun handleServerButtonClick(
    toggleSd: Boolean, onGetSerConfig: (Boolean) -> Config?, onReceiveApk: (File) -> Unit
) {
    if (isBusy.value) return
    isBusy.value = true

    if (isServerRunning.value) {
        Log.i(tag, "Stopping HTTP server")
        stopServer()
    } else {
        Log.i(tag, "Starting HTTP server")
        onGetSerConfig(toggleSd)?.let { serverConfig ->
            startServer(serverConfig) { file -> onReceiveApk(file) }
        } ?: run {
            Log.e(tag, "Configure root folder first")
            isBusy.value = false
        }
    }
}

@Composable
fun AutoScrollingLog(logList: List<String>) {
    val listState = rememberLazyListState()

    LaunchedEffect(logList.size) {
        if (logList.isNotEmpty()) {
            listState.animateScrollToItem(logList.size - 1)
        }
    }

    LazyColumn(
        state = listState, modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(logList) { index, log ->
            val keywords = listOf("error", "exception")
            val rowColor = when {
                keywords.any { log.contains(it, ignoreCase = true) } -> Color.Red
                index % 2 == 0 -> Color.LightGray // Even rows
                else -> Color.White // Odd rows
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

@Preview(showBackground = true)
@Composable
fun Column1Preview() {
    Theme1 {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            ComposeMainColumn1(
                modifier = Modifier.padding(innerPadding), logList = """
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
    """.lines()
            )
        }
    }
}
