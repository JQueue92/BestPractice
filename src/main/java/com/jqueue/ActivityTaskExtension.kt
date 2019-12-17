package com.jqueue

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * 清空任务栈
 */
fun clearTasks(context: Context) {
    val activityManager = (context?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        activityManager?.appTasks?.forEach { it.finishAndRemoveTask() }
    }
}

/**
 * 跳转android系统设置详情
 */
fun getAppDetailSettingIntent(context: Context): Boolean {
    val intent = Intent()
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (Build.VERSION.SDK_INT >= 9) {
        intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
        intent.data = Uri.fromParts("package", context.packageName, null)
    } else if (Build.VERSION.SDK_INT <= 8) {
        intent.action = Intent.ACTION_VIEW
        intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails")
        intent.putExtra("com.android.settings.ApplicationPkgName", context.packageName)
    }
    try {
        context.startActivity(intent)
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }

}