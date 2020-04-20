package com.refi64.cloverplay

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.CompoundButton
import android.widget.PopupMenu
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.topjohnwu.superuser.Shell
import de.psdev.licensesdialog.LicensesDialog
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : AppCompatActivity() {
  companion object {
    init {
      Shell.Config.verboseLogging(BuildConfig.DEBUG)
      Shell.Config.setTimeout(10)
    }
  }

  private lateinit var serviceControlToggle: SwitchMaterial

  private val serviceName: String
    get() {
      return "${BuildConfig.APPLICATION_ID}/com.refi64.cloverplay.OverlayService"
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    serviceControlToggle = findViewById(R.id.toggle_control)
    serviceControlToggle.setOnCheckedChangeListener { button, isChecked ->
      toggleService(button, isChecked)
    }

    val trialText = findViewById<TextView>(R.id.trial_state)
    trialText.setText(R.string.trial_check)

    TrialProvider.getProvider(this).provide { state ->
      if (state.expired) {
        trialText.setText(R.string.trial_expired)
      } else {
        if (state.expiration != null) {
          val expiration = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
              .withZone(ZoneId.systemDefault()).format(state.expiration)
          trialText.text = resources.getString(R.string.trial_expires_soon, expiration)
        } else {
          trialText.text = ""
        }

        checkRootAccess()
        updateServiceState()
      }
    }
  }

  fun onMenuButtonClick(view: View?) {
    val popup = PopupMenu(this, view)
    popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)

    popup.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.prefs -> {
          val intent = Intent(this, SettingsActivity::class.java)
          startActivity(intent)
        }
        R.id.licenses -> LicensesDialog.Builder(this).apply {
          setNotices(R.raw.notices)
          setIncludeOwnLicense(true)

          build().show()
        }
        R.id.about -> {
          val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cloverplay.app/"))
          startActivity(intent)
        }
      }

      true
    }

    popup.show()
  }

  private fun getEnabledServices(): List<String> = Settings.Secure.getString(contentResolver,
      Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).split(':')

  private fun updateServiceState() = serviceControlToggle.apply {
    isChecked = getEnabledServices().contains(serviceName)
    isEnabled = true
  }

  private fun toggleService(button: CompoundButton, isChecked: Boolean) {
    if (!button.isPressed) {
      return
    }

    serviceControlToggle.isEnabled = false

    val services = ArrayList(getEnabledServices())
    if (isChecked && !services.contains(serviceName)) {
      services.add(serviceName)
    } else if (!isChecked) {
      services.remove(serviceName)
    }

    val servicesString = services.joinToString(":")
    Shell.su("settings put secure enabled_accessibility_services $servicesString")
        .submit { result ->
          updateServiceState()

          if (!result.isSuccess) {
            AlertDialog.Builder(this).apply {
              setTitle(R.string.service_set_failure_title)
              setMessage(R.string.service_set_failure_message)
              setCancelable(true)
              setPositiveButton(R.string.ok) { _, _ -> finish() }
              show()
            }
          }
        }
  }

  private fun checkRootAccess() {
    if (!Shell.rootAccess()) {
      AlertDialog.Builder(this).apply {
        setTitle(R.string.root_failure_title)
        setMessage(R.string.root_failure_message)
        setCancelable(false)
        setPositiveButton(R.string.root_failure_quit) { _, _ -> finish() }

        show()
      }
    }
  }
}
