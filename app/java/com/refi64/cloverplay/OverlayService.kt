package com.refi64.cloverplay

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.*
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.forEach
import androidx.core.view.updateLayoutParams

class OverlayService : AccessibilityService() {
  companion object {
    private const val NOTIFICATION_CHANNEL_ID = "reactivate"
  }

  private enum class Profile { Stadia, Xcloud }

  private var overlayView: View? = null
  private var cloverService = CloverService()

  private val windowManager get() = getSystemService(WindowManager::class.java)!!
  private val notificationManager get() = getSystemService(NotificationManager::class.java)!!

  private val overlayLayout get() = overlayView?.findViewById<ConstraintLayout>(R.id.layout)

  private val screenStateReceiver = object : BroadcastReceiver() {
    override fun onReceive(ctx: Context?, intent: Intent?) {
      intent ?: return

      when (intent.action) {
        Intent.ACTION_SCREEN_ON -> updateState()
        Intent.ACTION_SCREEN_OFF -> deactivate()
      }
    }
  }

  override fun onServiceConnected() {
    super.onServiceConnected()

    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_SCREEN_ON)
      addAction(Intent.ACTION_SCREEN_OFF)
    }
    registerReceiver(screenStateReceiver, filter)

    cloverService.start(this)
  }

  private enum class Orientation { PORTRAIT, LANDSCAPE }

  private fun getWindowOrientation(window: AccessibilityWindowInfo): Orientation {
    val bounds = Rect()
    window.getBoundsInScreen(bounds)

    return if (bounds.right > bounds.bottom) Orientation.LANDSCAPE else Orientation.PORTRAIT
  }

  private fun <V> attachMappedTouchListeners(view: View,
                                             listenerFactory: (item: V) -> View.OnTouchListener,
                                             vararg pairs: Pair<Int, V>) {
    for (pair in pairs) {
      view.findViewById<View>(pair.first).setOnTouchListener(listenerFactory(pair.second))
    }
  }

  @SuppressLint("InflateParams")
  private fun activateProfile(profile: Profile) {
    if (overlayView != null) {
      return
    }

    val (theme, controller, extraButtons) = when (profile) {
      Profile.Stadia -> Triple(R.style.StadiaOverlayTheme,
          Protos.Controller.STADIA,
          arrayOf(R.id.button_more to Protos.Button.BUTTON_SELECT,
              R.id.button_menu to Protos.Button.BUTTON_START,
              R.id.button_assistant to Protos.Button.BUTTON_STADIA_ASSISTANT,
              R.id.button_screenshot to Protos.Button.BUTTON_STADIA_SCREENSHOT))
      Profile.Xcloud -> Triple(R.style.XcloudOverlayTheme,
          Protos.Controller.XBOX360,
          arrayOf(R.id.button_view to Protos.Button.BUTTON_SELECT,
              R.id.button_xmenu to Protos.Button.BUTTON_START))
    }

    cloverService.controller = controller

    val inflater = LayoutInflater.from(ContextThemeWrapper(this, theme))
    val view = inflater.inflate(R.layout.overlay, null)

    if (profile == Profile.Xcloud) {
      val toShow = listOf(R.id.button_view, R.id.button_xmenu)
      val toHide =
          listOf(R.id.button_assistant, R.id.button_screenshot, R.id.button_more, R.id.button_menu)

      for (id in toHide) {
        view.findViewById<View>(id).visibility = View.INVISIBLE
      }

      for (id in toShow) {
        view.findViewById<View>(id).visibility = View.VISIBLE
      }
    }

    view.findViewById<View>(R.id.hide_button).setOnLongClickListener {
      toggleViewVisibility(show = false)
      true
    }

    view.findViewById<View>(R.id.show_button).setOnLongClickListener {
      toggleViewVisibility(show = true)
      true
    }

    attachMappedTouchListeners(view,
        cloverService::createButtonTouchListener,
        R.id.button_a to Protos.Button.BUTTON_A,
        R.id.button_b to Protos.Button.BUTTON_B,
        R.id.button_x to Protos.Button.BUTTON_X,
        R.id.button_y to Protos.Button.BUTTON_Y,
        R.id.button_l1 to Protos.Button.BUTTON_L1,
        R.id.button_l3 to Protos.Button.BUTTON_L3,
        R.id.button_r1 to Protos.Button.BUTTON_R1,
        R.id.button_r3 to Protos.Button.BUTTON_R3,
        R.id.button_home to Protos.Button.BUTTON_HOME,
        *extraButtons)

    attachMappedTouchListeners(view,
        cloverService::createDpadTouchListener,
        R.id.button_up to Protos.DpadDirection.DPAD_NORTH,
        R.id.button_down to Protos.DpadDirection.DPAD_SOUTH,
        R.id.button_right to Protos.DpadDirection.DPAD_EAST,
        R.id.button_left to Protos.DpadDirection.DPAD_WEST)

    attachMappedTouchListeners(view,
        cloverService::createTriggerTouchListener,
        R.id.button_l2 to Protos.Trigger.TRIGGER_LEFT,
        R.id.button_r2 to Protos.Trigger.TRIGGER_RIGHT)

    view.findViewById<JoystickCanvasView>(R.id.joystick_view).joystickEventListener =
        cloverService.createJoystickEventListener()

    windowManager.addView(view, WindowManager.LayoutParams().apply {
      type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
      format = PixelFormat.TRANSLUCENT
      flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

      gravity = Gravity.BOTTOM

      width = WindowManager.LayoutParams.MATCH_PARENT
      height = WindowManager.LayoutParams.MATCH_PARENT
    })
    overlayView = view
  }

  private fun deactivate() {
    overlayView?.let { view ->
      windowManager.removeView(view)
      overlayView = null
    }
  }

  private fun toggleViewVisibility(show: Boolean) {
    val (currentState, nextState, layoutDimens) = if (show) {
      Triple(View.GONE, View.VISIBLE, WindowManager.LayoutParams.MATCH_PARENT)
    } else {
      Triple(View.VISIBLE, View.GONE, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    val view = overlayView!!
    val layout = overlayLayout!!
    layout.forEach { child ->
      if (child.visibility == currentState) {
        child.visibility = nextState
      } else if (child.id == R.id.show_button) {
        child.visibility = currentState
      }
    }

    view.updateLayoutParams<WindowManager.LayoutParams> {
      width = layoutDimens
      height = layoutDimens
    }

    // Avoid an awkward animation.
    windowManager.removeView(view)
    windowManager.addView(view, view.layoutParams)
  }

  override fun onAccessibilityEvent(p0: AccessibilityEvent?) = updateState()

  private fun updateState() {
    val window = windows.firstOrNull { win ->
      win.type == AccessibilityWindowInfo.TYPE_APPLICATION
    } ?: return

    val visibleProfile = window.root?.let { root ->
      val profile = when (root.packageName) {
        "com.google.stadia.android" -> Profile.Stadia
        "com.microsoft.xcloud" -> Profile.Xcloud
        else -> null
      }
      root.recycle()
      profile
    }

    if (visibleProfile != null) {
      when (getWindowOrientation(window)) {
        Orientation.LANDSCAPE -> activateProfile(visibleProfile)
        Orientation.PORTRAIT -> deactivate()
      }
    } else {
      deactivate()
    }
  }

  override fun onInterrupt() {}

  override fun onDestroy() {
    deactivate()
    unregisterReceiver(screenStateReceiver)
    cloverService.destroy()
    notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)

    super.onDestroy()
  }
}