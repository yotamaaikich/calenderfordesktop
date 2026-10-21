plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "jp.yotamaaikich.notificationcalendar"
    compileSdk = 35

    defaultConfig {
        applicationId = "jp.yotamaaikich.notificationcalendar"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
