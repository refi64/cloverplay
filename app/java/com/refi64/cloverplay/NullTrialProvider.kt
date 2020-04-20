package com.refi64.cloverplay

import android.content.Context

@Suppress("unused")
class NullTrialProvider : TrialProvider() {
  companion object : TrialProvider.Factory() {
    override fun getProvider(context: Context): TrialProvider = NullTrialProvider()
  }

  override fun provide(callback: (state: State) -> Unit) {
    callback(State(expired = false, expiration = null))
  }
}
