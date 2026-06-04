package com.mygolfswing.app.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

fun Context.isPermissionPermanentlyDenied(permission: String): Boolean {
    val activity = findActivity() ?: return false
    return ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
