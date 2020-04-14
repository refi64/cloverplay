package com.refi64.cloverplay

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.*
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

class OverlayService : AccessibilityService() {
  private enum class Profile { Stadia, Xcloud }

  private var overlayView: View? = null
  private var cloverService = CloverService()

  private val windowManager get() = getSystemService(WindowManager::class.java)!!

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
          arrayOf(R.id.button_back to Protos.Button.BUTTON_SELECT,
              R.id.button_start to Protos.Button.BUTTON_START))
    }

    cloverService.controller = controller

    val inflater = LayoutInflater.from(ContextThemeWrapper(this, theme))
    val view = inflater.inflate(R.layout.overlay, null)

    val constraints = ConstraintSet()
    val layout = view.findViewById<ConstraintLayout>(R.id.layout)

    if (profile == Profile.Xcloud) {
      val toShow = listOf(R.id.button_back, R.id.button_start)
      val toHide =
          listOf(R.id.button_assistant, R.id.button_screenshot, R.id.button_more, R.id.button_menu)

      for (id in toHide) {
        view.findViewById<View>(id).visibility = View.GONE
      }

      for (id in toShow) {
        view.findViewById<View>(id).visibility = View.VISIBLE
      }

      constraints.apply {
        clone(layout)

        connect(R.id.button_l1, ConstraintSet.END, R.id.button_back, ConstraintSet.START)
        connect(R.id.button_l2, ConstraintSet.END, R.id.button_back, ConstraintSet.START)

        connect(R.id.button_r1, ConstraintSet.START, R.id.button_start, ConstraintSet.END)
        connect(R.id.button_r2, ConstraintSet.START, R.id.button_start, ConstraintSet.END)

        applyTo(layout)
      }
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

    super.onDestroy()
  }
}