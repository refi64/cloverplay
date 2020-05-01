package com.refi64.cloverplay

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import io.trialy.library.Constants.*
import io.trialy.library.Trialy
import java.time.Instant
import java.time.temporal.ChronoUnit

const val TRIAL_ORIGIN = "trial_origin"

class TrialyTrialProvider(private val context: Context) : TrialProvider() {
  private val trialy = Trialy(context, BuildConfig.TRIAL_KEY)

  override fun provide(callback: (valid: State) -> Unit) {
    trialy.startTrial(BuildConfig.APPLICATION_ID) { _, _, _ ->
      trialy.checkTrial(BuildConfig.APPLICATION_ID) { status, _, _ ->
        val prefs = context.getSharedPreferences("trial_info", Context.MODE_PRIVATE)
        if (!prefs.contains(TRIAL_ORIGIN)) {
          prefs.edit {
            val days = if (BuildConfig.APPLICATION_ID.endsWith(".ltrial")) 14L else 2L
            putLong(TRIAL_ORIGIN, Instant.now().plus(days, ChronoUnit.DAYS).toEpochMilli())
            commit()
          }
        }

        val expired = when (status) {
          STATUS_TRIAL_JUST_STARTED, STATUS_TRIAL_RUNNING -> false
          STATUS_TRIAL_JUST_ENDED, STATUS_TRIAL_OVER -> true
          else -> {
            Log.i("TrialyTrialProvider", "Unexpected Trialy API response: $status")
            true
          }
        }

        val state =
            State(expired,
                expiration = Instant.ofEpochMilli(prefs.getLong(TRIAL_ORIGIN, 0)))
        callback(state)
      }
    }
  }
}