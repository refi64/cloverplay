package com.refi64.cloverplay

class NullTrialProvider : TrialProvider() {
  override fun provide(callback: (state: State) -> Unit) {
    callback(State(expired = false, expiration = null))
  }
}
