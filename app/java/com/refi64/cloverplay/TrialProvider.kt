package com.refi64.cloverplay

import android.content.Context
import java.time.Instant

abstract class TrialProvider {
  data class State(val expired: Boolean, val expiration: Instant?)

  abstract fun provide(callback: (state: State) -> Unit)

  companion object {
    fun getProvider(context: Context): TrialProvider =
        if (BuildConfig.APPLICATION_ID.endsWith(".trial")) {
          TriaforceTrialProvider(context.contentResolver)
        } else {
          NullTrialProvider()
        }
  }
}
