package com.droidpilot.backend.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.droidpilot.core.automation.BackendUnavailableError
import com.droidpilot.core.automation.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class ShizukuRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.Unavailable)
    public val state: StateFlow<ShizukuState> = _state.asStateFlow()

    @Volatile private var service: IShellService? = null

    private val binderListener = Shizuku.OnBinderReceivedListener { onShizukuAvailable() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        service = null
        _state.value = ShizukuState.Unavailable
    }

    public fun init() {
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
    }

    public fun dispose() {
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
    }

    private fun onShizukuAvailable() {
        if (Shizuku.isPreV11()) {
            _state.value = ShizukuState.Unavailable
            return
        }
        when (Shizuku.checkSelfPermission()) {
            PackageManager.PERMISSION_GRANTED -> bindUserService()
            else -> _state.value = ShizukuState.PermissionRequired
        }
    }

    public fun requestPermission(requestCode: Int) {
        Shizuku.requestPermission(requestCode)
    }

    public fun onPermissionResult(granted: Boolean) {
        if (granted) bindUserService() else _state.value = ShizukuState.PermissionDenied
    }

    private fun bindUserService() {
        _state.value = ShizukuState.Connecting
        val args = Shizuku.UserServiceArgs(
            ComponentName(context, ShellUserService::class.java),
        ).processNameSuffix("shell").version(1).debuggable(BuildConfig.DEBUG)
        Shizuku.bindUserService(args, userServiceConn)
    }

    private val userServiceConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IShellService.Stub.asInterface(binder)
            _state.value = ShizukuState.Available
        }
        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            _state.value = ShizukuState.Unavailable
        }
    }

    public suspend fun shell(command: String): ShellResult = withContext(Dispatchers.IO) {
        val output = requireService().run(command)
        ShellResult(stdout = output, stderr = "", exitCode = 0)
    }

    public suspend fun appStart(packageName: String, activity: String? = null) {
        val target = if (activity != null) "$packageName/$activity" else packageName
        shell(if (activity != null) "am start -n $target" else "monkey -p $target -c android.intent.category.LAUNCHER 1")
    }

    public suspend fun appStop(packageName: String) {
        shell("am force-stop $packageName")
    }

    public suspend fun appClear(packageName: String) {
        shell("pm clear $packageName")
    }

    public suspend fun appInstall(apkPath: String) {
        shell("pm install -r \"$apkPath\"")
    }

    public suspend fun screencap(): ByteArray = withContext(Dispatchers.IO) {
        requireService().screencap()
    }

    public suspend fun logcat(): String = withContext(Dispatchers.IO) {
        requireService().logcat()
    }

    private fun requireService(): IShellService = service
        ?: throw BackendUnavailableError(backend = "Shizuku", setupHint = SETUP_HINT)

    public companion object {
        public const val PERMISSION_REQUEST_CODE: Int = 42
        public const val SETUP_HINT: String =
            "Install Shizuku from the Play Store, enable Wireless Debugging in " +
                "Developer Options, then start Shizuku. Tap 'Set up' for step-by-step instructions."
    }
}
