package com.vscodeandroid.editor.terminal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object TermuxIntegration {
    const val TERMUX_PACKAGE = "com.termux"
    const val TERMUX_ACTIVITY = "com.termux.app.TermuxActivity"
    const val TERMUX_RUN_COMMAND = "com.termux.RUN_COMMAND"
    val TERMUX_BIN = "/data/data/com.termux/files/usr/bin"

    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openTermuxAtPath(context: Context, path: String) {
        try {
            // Try to open Termux with a cd command
            val intent = Intent().apply {
                setClassName(TERMUX_PACKAGE, TERMUX_ACTIVITY)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            // Run cd command after a delay via RUN_COMMAND
            sendCommandToTermux(context, "cd '${path.replace("'", "\\'")}' && clear")
        } catch (e: Exception) {
            // Fallback: open Termux without path
            openTermux(context)
        }
    }

    fun openTermux(context: Context) {
        try {
            val intent = Intent().apply {
                setClassName(TERMUX_PACKAGE, TERMUX_ACTIVITY)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Termux not installed or error
        }
    }

    fun sendCommandToTermux(context: Context, command: String) {
        try {
            val intent = Intent(TERMUX_RUN_COMMAND).apply {
                setClassName(TERMUX_PACKAGE, "com.termux.app.RunCommandService")
                data = Uri.parse("$TERMUX_PACKAGE.command")
                putExtra("com.termux.execute.arguments", arrayOf("-c", command))
                putExtra("com.termux.execute.background", false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("TermuxIntegration", "Error sending command: ${e.message}")
        }
    }

    fun openTermuxInstallPage(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$TERMUX_PACKAGE")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://f-droid.org/packages/$TERMUX_PACKAGE/")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }
}
