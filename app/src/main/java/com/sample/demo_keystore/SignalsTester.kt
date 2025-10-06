package com.sample.demo_keystore
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest


class SignalsTester {
    private var appContext: Context
    private var packageManager: PackageManager

    constructor(context: Context, pm: PackageManager) {
        this.appContext = context
        this.packageManager = pm
    }

    fun getPackageName(): String? {
        return appContext.packageName
    }

    fun getCertificateHash(): String? {
        return try {

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(appContext.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(appContext.packageName, PackageManager.GET_SIGNATURES)
            }

            // should replace with "SystemProperties.getInt("ro.build.version.sdk", 0);"
            // and then add string obfuscation
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            if (signatures == null) {
                return null
            }

            // TODO: think about adding amount of signatures as signal
            val certBytes = signatures[0].toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            // return as hexstring
            md.digest(certBytes).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun getInstallationSource(): String {
        var installerName: String? = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val sourceInfo = packageManager.getInstallSourceInfo(appContext.packageName)
            installerName = sourceInfo.installingPackageName
            if (installerName != sourceInfo.initiatingPackageName) {
                return "NAME_MOD"
            }
        }
        else {
            @Suppress("DEPRECATION")
            installerName = packageManager.getInstallerPackageName(appContext.packageName)
        }
        if (installerName == null) {
            return "UNK"
        }
        return installerName
    }
}
