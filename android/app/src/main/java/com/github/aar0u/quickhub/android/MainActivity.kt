package com.github.aar0u.quickhub.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.github.aar0u.quickhub.android.HttpRunner.isBusy
import com.github.aar0u.quickhub.android.HttpRunner.isServerRunning
import com.github.aar0u.quickhub.android.HttpRunner.startServer
import com.github.aar0u.quickhub.android.HttpRunner.stopServer
import com.github.aar0u.quickhub.model.Config
import com.github.aar0u.quickhub.service.HttpService
import java.io.File
import java.io.InputStream

private val tag = MainActivity::class.simpleName
private val versionGreater29 = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q
private val versionGreater22 = Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1
private val mainHandler = Handler(Looper.getMainLooper())

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var toggleState = false
    private lateinit var folderPickerLauncher: ActivityResultLauncher<Uri?>
    private lateinit var permissionSettingLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.init(this)

        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        initWebView()
        Log.setWebView(webView)
        initLaunchers()

        val sdPath = Environment.getExternalStorageDirectory().absolutePath
        toggleState =
            sdPath == sharedPreferences.getString("path", null) && checkStoragePermissions()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(Bridge(), "Android")
        webView.loadUrl("file:///android_asset/control.html")
    }

    private fun initLaunchers() {
        folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            uri?.let {
                val folderPath = getAbsolutePathFromUri(it)
                Log.i(tag, "Root folder: $folderPath")
                sharedPreferences.edit().putString("path", folderPath).commit()
            }
        }

        permissionSettingLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                Log.i(tag, "Activity result: $it")
                syncToggleState()
            }

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                Log.i(tag, "Activity result: $permissions")
                syncToggleState()
            }
    }

    private fun checkStoragePermissions(): Boolean {
        return if (versionGreater29) {
            Environment.isExternalStorageManager()
        } else if (versionGreater22) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestStoragePermissions() {
        if (versionGreater29) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            permissionSettingLauncher.launch(intent)
        } else if (versionGreater22) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
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
                setDataAndType(apkUri, "application/vnd.android.package-archive")
            }
            this.startActivity(installIntent)
        }
    }

    private fun updateToggleState(isChecked: Boolean) {
        if (isChecked && !syncToggleState()) {
            requestStoragePermissions()
        } else {
            toggleState = isChecked
            callJs("onToggleStateChanged($toggleState)")
        }
    }

    private fun syncToggleState(): Boolean {
        toggleState = checkStoragePermissions()
        if (toggleState) {
            val sdPath = Environment.getExternalStorageDirectory().absolutePath
            sharedPreferences.edit().putString("path", sdPath).apply()
        }
        callJs("onToggleStateChanged($toggleState)")
        return toggleState
    }

    private fun getAbsolutePathFromUri(uri: Uri): String? {
        val sdPath = Environment.getExternalStorageDirectory().absolutePath
        return uri.path?.split(":")?.takeIf {
            it.getOrNull(0)?.endsWith("primary") ?: false
        }?.let {
            sdPath + "/" + it.getOrNull(1)
        }
    }

    private fun getSerConfig(): Config? {
        val sdPath = Environment.getExternalStorageDirectory().absolutePath
        return sharedPreferences.getString("path", null)
            ?.takeIf { it != sdPath || toggleState }
            ?.let { Config(it, 3006, overwrite = true) }
    }

    private fun callJs(script: String) {
        mainHandler.post { webView.evaluateJavascript(script, null) }
    }

    private fun handleServerButtonClick() {
        if (isBusy) return
        isBusy = true
        callJs("onServerStateChanged($isServerRunning, true)")

        if (isServerRunning) {
            Log.i(tag, "Stopping HTTP server")
            stopServer()
        } else {
            Log.i(tag, "Starting HTTP server")
            getSerConfig()?.let { serverConfig ->
                startServer(serverConfig, object : HttpService.CallBackListener {
                    override fun onContentRequested(path: String): InputStream {
                        return Log.file.also {
                            Log.i(tag, "Retrieve log file via dummy url $path")
                        }.inputStream()
                    }

                    override fun onFileReceived(file: File) = installApk(file)
                })
            } ?: run {
                Log.e(tag, "An error occurred, please check permission")
                isBusy = false
                callJs("onServerStateChanged($isServerRunning, false)")
            }
        }
    }

    private val sharedPreferences by lazy {
        applicationContext.getSharedPreferences("_preferences", MODE_PRIVATE)
    }

    inner class Bridge {
        @JavascriptInterface
        fun toggleServer() = handleServerButtonClick()

        @JavascriptInterface
        fun openFolderPicker() {
            if (versionGreater29) folderPickerLauncher.launch(null)
        }

        @JavascriptInterface
        fun onToggleSd(checked: Boolean) = mainHandler.post { updateToggleState(checked) }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
