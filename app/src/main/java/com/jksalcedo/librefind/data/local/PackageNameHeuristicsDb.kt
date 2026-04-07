package com.jksalcedo.librefind.data.local

class PackageNameHeuristicsDb {

    private val aospSystemPackages = setOf(
        // AOSP system apps
        "com.android.settings",
        "com.android.dialer",
        "com.android.contacts",
        "com.android.phone",
        "com.android.mms",
        "com.android.messaging",
        "com.android.calendar",
        "com.android.camera2",
        "com.android.calculator2",
        "com.android.gallery3d",
        "com.android.deskclock",
        "com.android.documentsui",
        "com.android.fileexplorer",
        "com.android.inputmethod.latin",
        "com.android.nfc",
        "com.android.bluetooth",
        "com.android.providers.contacts",
        "com.android.providers.calendar",
        "com.android.providers.media",
        "com.android.providers.downloads",
        "com.android.providers.telephony",
        "com.android.systemui",
        "com.android.launcher3",
        "com.android.wallpaper",
        "com.android.browser",
        "com.android.soundrecorder",
        "com.android.traceur",
        "com.android.musicfx",
        "com.android.cellbroadcastreceiver",
        "com.android.se",
        "com.android.printspooler",
        "com.android.storagemanager",
        "com.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.android.shell",
        "com.android.vpndialogs",
        "com.android.captiveportallogin",
        "com.android.networkstack",
        "com.android.wallpaperpicker"
    )

    fun isAospSystemPackageName(packageName: String) = packageName in aospSystemPackages
}
