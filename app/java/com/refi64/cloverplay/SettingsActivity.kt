package com.refi64.cloverplay

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {
  class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
      setPreferencesFromResource(R.xml.preferences, rootKey)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)

    supportFragmentManager.beginTransaction().apply {
      replace(R.id.settings, SettingsFragment())
      commit()
    }
  }
}