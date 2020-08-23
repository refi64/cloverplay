package com.refi64.cloverplay

import android.content.Context
import java.io.File

class CloverServiceJNI(context: Context) {
  init {
    // XXX: So Bazel names the libs over the APK filename which we don't have, so it's easiest to
    // just scan the library folder.
    val libDir = File(context.applicationInfo.nativeLibraryDir)
    val lib = libDir.listFiles { file -> file.name.startsWith("libcloverplay") }!!.first()
    System.loadLibrary(lib.name.removePrefix("lib").removeSuffix(".so"))
  }

  external fun create(): Long
  external fun sendEvents(handle: Long, request: String)
  external fun destroy(handle: Long)
}
