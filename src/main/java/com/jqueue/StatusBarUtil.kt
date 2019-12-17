package com.jqueue

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.res.Resources

import java.lang.reflect.Method

object StatusBarUtil {

    fun getStatusBarHeight(context: Context): Int {
        var height = 0
        val resourceId = context.applicationContext.resources.getIdentifier(
            "status_bar_height",
            "dimen",
            "android"
        )
        if (resourceId > 0) {
            height = context.applicationContext.resources.getDimensionPixelSize(resourceId)
        }
        return height
    }

    /**
     * 获取是否存在NavigationBar
     */
    fun checkDeviceHasNavigationBar(context: Context): Boolean {
        var hasNavigationBar = false
        try {
            val rs = context.resources
            val id = rs.getIdentifier("config_showNavigationBar", "bool", "android")
            if (id > 0) {
                hasNavigationBar = rs.getBoolean(id)
            }
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val m = systemPropertiesClass.getMethod("get", String::class.java)
            val navBarOverride = m.invoke(systemPropertiesClass, "qemu.hw.mainkeys") as String
            if ("1" == navBarOverride) {
                hasNavigationBar = false
            } else if ("0" == navBarOverride) {
                hasNavigationBar = true
            }
        } catch (e: Exception) {

        }

        return hasNavigationBar
    }

    /**
     * 获取底部导航栏的高度
     *
     * @param activity
     * @return
     */
    fun getNavigationBarHeight(activity: Context): Int {
        val resources = activity.resources
        val resourceId = resources.getIdentifier(
            "navigation_bar_height",
            "dimen", "android"
        )
        //获取NavigationBar的高度
        return resources.getDimensionPixelSize(resourceId)
    }

    /**
     * 获取显示屏缺口的高度
     * 显示屏缺口高度与状态栏同高
     * @param activity
     * @return
     */
    @TargetApi(28)
    fun getDisplayCutoutHeight(activity: Activity): Int {
        return getStatusBarHeight(activity) + 10
    }

}
