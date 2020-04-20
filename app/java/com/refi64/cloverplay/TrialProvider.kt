package com.refi64.cloverplay

import android.content.Context
import java.time.Instant
import kotlin.reflect.full.companionObjectInstance

abstract class TrialProvider {
  data class State(val expired: Boolean, val expiration: Instant?)

  abstract fun provide(callback: (state: State) -> Unit)

  abstract class Factory {
    abstract fun getProvider(context: Context): TrialProvider

    companion object {
      val instance: Factory by lazy {
        @Suppress("ConstantConditionIf") val provider =
            if (BuildConfig.TRIAL_KEY != "") "Trialy" else "Null"
        val cls = Class.forName("com.refi64.cloverplay.${provider}TrialProvider")
        cls.kotlin.companionObjectInstance as Factory
      }
    }
  }

  companion object {
    fun getProvider(context: Context): TrialProvider = Factory.instance.getProvider(context)
  }
}
