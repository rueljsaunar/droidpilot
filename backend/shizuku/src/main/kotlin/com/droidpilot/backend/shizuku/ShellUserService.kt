package com.droidpilot.backend.shizuku

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * Runs inside the Shizuku privileged server process (adb shell user).
 * ProcessBuilder here has shell-level access.
 */
public class ShellUserService : IShellService.Stub() {

    override fun destroy() {
        System.exit(0)
    }

    override fun run(command: String): String {
        val proc = ProcessBuilder("sh", "-c", command)
            .redirectErrorStream(true)
            .start()
        val output = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        return output
    }

    override fun screencap(): ByteArray {
        val proc = ProcessBuilder("screencap", "-p").start()
        val pngBytes = proc.inputStream.readBytes()
        proc.waitFor()
        // Compress PNG→JPEG to stay under the 1 MB binder transaction limit.
        val bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
            ?: return pngBytes
        return ByteArrayOutputStream().also { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            bitmap.recycle()
        }.toByteArray()
    }

    override fun logcat(): String {
        // -t 500 caps output to the last 500 lines to stay under the binder limit.
        val proc = ProcessBuilder("logcat", "-d", "-t", "500").start()
        val text = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        return text
    }
}
