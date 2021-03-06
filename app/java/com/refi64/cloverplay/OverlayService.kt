package com.refi64.cloverplay

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.*
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.forEach
import androidx.core.view.updateLayoutParams
import androidx.preference.PreferenceManager

class OverlayService : AccessibilityService() {
  companion object {
    private const val NOTIFICATION_CHANNEL_ID = "reactivate"
    private const val TAG = "OverlayService"
  }

  private enum class Profile { Stadia, Xcloud }

  private val abxyButtons = listOf(R.id.button_a, R.id.button_b, R.id.button_x, R.id.button_y)

  private val largeRoundButtons = listOf(R.id.button_a,
      R.id.button_b,
      R.id.button_x,
      R.id.button_y,
      R.id.button_l3,
      R.id.button_r3)

  private val leftButtons = listOf(R.id.button_left,
      R.id.button_right,
      R.id.button_up,
      R.id.button_down,
      R.id.button_l2,
      R.id.button_l3,
      R.id.show_button,
      R.id.hide_button)

  private val rightButtons = listOf(R.id.button_a,
      R.id.button_b,
      R.id.button_x,
      R.id.button_y,
      R.id.button_r2,
      R.id.button_r3,
      R.id.show_button,
      R.id.hide_button)

  private val bottomButtons =
      // Only button A, because BXY are all relative to A
      listOf(R.id.button_up,
          R.id.button_down,
          R.id.button_left,
          R.id.button_right,
          R.id.button_a,
          R.id.show_button,
          R.id.hide_button,
          R.id.button_l3,
          R.id.button_r3)

  private var overlayView: View? = null
  private var cloverService = CloverService()

  private val windowManager get() = getSystemService(WindowManager::class.java)!!
  private val notificationManager get() = getSystemService(NotificationManager::class.java)!!
  private val preferences get() = PreferenceManager.getDefaultSharedPreferences(this)

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

    PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_SCREEN_ON)
      addAction(Intent.ACTION_SCREEN_OFF)
    }
    registerReceiver(screenStateReceiver, filter)

    TrialProvider.getProvider(this).provide { state ->
      if (state.expired) {
        disableSelf()
      } else {
        cloverService.start(this)
      }
    }
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

  private fun getScaledSize(key: String): Int =
      (preferences.getInt(key, 0) * resources.displayMetrics.density).toInt()

  @SuppressLint("InflateParams")
  private fun activateProfile(profile: Profile) {
    if (overlayView != null || !cloverService.started) {
      if (!cloverService.started) {
        Log.i(TAG, "Ignoring activate, because service is not available yet")
      }

      return
    }

    TrialProvider.getProvider(this).provide { state ->
      if (state.expired) {
        Log.i(TAG, "Trial has expired, so disabling self")
        disableSelf()
      }
    }

    val (theme, controller, extraButtons) = when (profile) {
      Profile.Stadia -> Triple(R.style.StadiaOverlayTheme,
          CloverService.Controller.STADIA,
          arrayOf(R.id.button_more to CloverService.Button.SELECT,
              R.id.button_menu to CloverService.Button.START,
              R.id.button_assistant to CloverService.Button.STADIA_ASSISTANT,
              R.id.button_screenshot to CloverService.Button.STADIA_SCREENSHOT))
      Profile.Xcloud -> Triple(R.style.XcloudOverlayTheme,
          CloverService.Controller.XBOX,
          arrayOf(R.id.button_view to CloverService.Button.SELECT,
              R.id.button_xmenu to CloverService.Button.START))
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
        R.id.button_l1 to CloverService.Button.L1,
        R.id.button_l3 to CloverService.Button.L3,
        R.id.button_r1 to CloverService.Button.R1,
        R.id.button_r3 to CloverService.Button.R3,
        R.id.button_home to CloverService.Button.HOME,
        *extraButtons)

    val abxyListenerFactory = if (preferences.getBoolean("multi_press", false)) {
      Log.d(TAG, "multi_press is enabled")

      MultiPressController(abxyButtons.toSet(), view as ViewGroup).let { multiPressController ->
        { button: CloverService.Button ->
          val listener = cloverService.createButtonTouchListener(button)
          multiPressController.getListener(delegate = listener)
        }
      }
    } else cloverService::createButtonTouchListener

    attachMappedTouchListeners(view,
        abxyListenerFactory,
        R.id.button_a to CloverService.Button.A,
        R.id.button_b to CloverService.Button.B,
        R.id.button_x to CloverService.Button.X,
        R.id.button_y to CloverService.Button.Y)

    attachMappedTouchListeners(view,
        cloverService::createDpadTouchListener,
        R.id.button_up to CloverService.DpadDirection.NORTH,
        R.id.button_down to CloverService.DpadDirection.SOUTH,
        R.id.button_right to CloverService.DpadDirection.EAST,
        R.id.button_left to CloverService.DpadDirection.WEST)

    attachMappedTouchListeners(view,
        cloverService::createTriggerTouchListener,
        R.id.button_l2 to CloverService.Trigger.LEFT,
        R.id.button_r2 to CloverService.Trigger.RIGHT)

    view.findViewById<JoystickCanvasView>(R.id.joystick_view).apply {
      joystickEventListener = cloverService.createJoystickEventListener()
      radius = preferences.getInt("joystick_radius", DEFAULT_RADIUS.toInt()).toFloat()
    }

    for (id in leftButtons) {
      view.findViewById<View>(id).updateLayoutParams<ConstraintLayout.LayoutParams> {
        leftMargin += getScaledSize("left_margin")
      }
    }

    for (id in rightButtons) {
      view.findViewById<View>(id).updateLayoutParams<ConstraintLayout.LayoutParams> {
        rightMargin += getScaledSize("right_margin")
      }
    }

    for (id in largeRoundButtons) {
      view.findViewById<RoundControllerButtonView>(id).apply {
        customSize = getScaledSize("round_size")

        if (id != R.id.button_l3 && id != R.id.button_r3) {
          updateLayoutParams<ConstraintLayout.LayoutParams> {
            if (id != R.id.button_a) {
              bottomMargin = getScaledSize("abxy_spacing")
            }

            if (id != R.id.button_b) {
              rightMargin = getScaledSize("abxy_spacing")
            }
          }
        }
      }
    }

    // XXX: this is ugly
    for (id in listOf(R.id.button_l2, R.id.button_r2)) {
      view.findViewById<View>(id).updateLayoutParams<ConstraintLayout.LayoutParams> {
        if (id == R.id.button_l2) {
          leftMargin = getScaledSize("l2_padding")
        } else {
          rightMargin = getScaledSize("r2_padding")
        }
      }
    }

    for (id in bottomButtons) {
      view.findViewById<View>(id).updateLayoutParams<ConstraintLayout.LayoutParams> {
        bottomMargin += getScaledSize("bottom_padding")
      }
    }

    windowManager.addView(view, WindowManager.LayoutParams().apply {
      type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
      format = PixelFormat.TRANSLUCENT
      flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

      gravity = Gravity.BOTTOM

      restoreDefaultLayoutParams(this)
    })
    overlayView = view
  }

  private fun restoreDefaultLayoutParams(params: WindowManager.LayoutParams) {
    params.width = WindowManager.LayoutParams.MATCH_PARENT
    params.height = WindowManager.LayoutParams.MATCH_PARENT
  }

  private fun deactivate() {
    overlayView?.let { view ->
      windowManager.removeView(view)
      overlayView = null
    }
  }

  private fun toggleViewVisibility(show: Boolean) {
    val (currentState, nextState) = if (show) {
      Pair(View.GONE, View.VISIBLE)
    } else {
      Pair(View.VISIBLE, View.GONE)
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
      if (show) {
        restoreDefaultLayoutParams(this)
      } else {
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
      }
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
        "com.gamepass", "com.gamepass.beta" -> Profile.Xcloud
        "com.nvidia.geforcenow" -> {
          if (preferences.getBoolean("gfn", false)) Profile.Xcloud
          else null
        }
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