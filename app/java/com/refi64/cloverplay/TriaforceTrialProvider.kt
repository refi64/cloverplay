package com.refi64.cloverplay

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import io.sentry.core.Sentry
import io.sentry.core.SentryEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.time.LocalDateTime
import java.time.ZoneOffset

class TriaforceTrialProvider(val contentResolver: ContentResolver) : TrialProvider() {
  @Serializable
  data class TrialResponse(val active: Boolean, val expiration: String)

  companion object {
    const val TAG = "TriaforceTrialProvider"

    const val SCHEME = "https"
    const val URL = "triaforce.refi64.com"
    const val PROJECT = "cloverplay"
    const val STATUS_PATH = "status"
  }

  @SuppressLint("HardwareIds")
  val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

  private class ResponseCallback(val callback: (state: State) -> Unit) : Callback {
    override fun onFailure(call: Call, e: IOException) {
      Log.e(TAG, "Failed to call triaforce server: $e", e)
      Sentry.captureException(e, "Calling triaforce server")
      callback(State(true, null))
    }

    override fun onResponse(call: Call, response: Response) {
      val json = Json(JsonConfiguration.Stable)
      try {
        if (!response.isSuccessful) {
          throw Exception("Server returned code ${response.code}")
        }

        response.body!!.use { body ->
          val trialResponse = json.parse(TrialResponse.serializer(), body.string())
          val instant =
              LocalDateTime.parse(trialResponse.expiration).atOffset(ZoneOffset.UTC).toInstant()
          callback(State(expired = !trialResponse.active, expiration = instant))
        }
      } catch (ex: Exception) {
        Log.e(TAG, "Failed to get triaforce server response", ex)
        callback(State(true, null))
      }
    }
  }

  override fun provide(callback: (state: State) -> Unit) {
    val client = OkHttpClient()

    val url = HttpUrl.Builder().apply {
      scheme(SCHEME)
      host(URL)

      addPathSegment(STATUS_PATH)
      addPathSegment(PROJECT)
      addPathSegment(androidId)
    }.build()

    val request = Request.Builder().url(url).build()

    val handler = Handler(Looper.getMainLooper())
    client.newCall(request).enqueue(ResponseCallback { state ->
      handler.post { callback(state) }
    })
  }
}