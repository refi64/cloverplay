package com.refi64.cloverplay

import android.app.Application
import android.util.Log
import io.sentry.android.core.SentryAndroid
import java.io.File

class CloverApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    // Bazel's StubApplication messes with the library directories if we have our own incremental
    // libs, so Sentry won't work.
    if (!applicationInfo.className.endsWith(".StubApplication")) {
      // old shitty attempt to make things still work
      //      val incrementalLib = File(applicationInfo.dataDir, "incrementallib")
      //
      //      for (file in File(applicationInfo.nativeLibraryDir).listFiles() ?: arrayOf<File>()) {
      //        Log.i("CloverApplication", "${file.path}: ${file.extension}")
      //        if (file.extension != "so") {
      //          continue
      //        }
      //
      //        file.copyTo(File(incrementalLib, file.name))
      //      }
      SentryAndroid.init(this)
    }
  }
}