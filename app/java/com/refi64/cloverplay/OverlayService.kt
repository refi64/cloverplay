package com.refi64.cloverplay

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.*
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo

class OverlayService : AccessibilityService() {
  private var overlayView: View? = null
  private var cloverService = CloverService()

  private val windowManager get() = getSystemService(WindowManager::class.java)!!

  private val screenStateReceiver = object : BroadcastReceiver() {
    override fun onReceive(ctx: Context?, intent: Intent?) {
      intent ?: return

      when (intent.action) {
        Intent.ACTION_SCREEN_ON -> updateState()
        Intent.ACTION_SCREEN_OFF -> removeOverlay()
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
  private fun addOverlay() {
    if (overlayView != null) {
      return
    }

    val inflater = LayoutInflater.from(ContextThemeWrapper(this, R.style.StadiaOverlayTheme))
    val view = inflater.inflate(R.layout.overlay, null)

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
        R.id.button_more to Protos.Button.BUTTON_START,
        R.id.button_menu to Protos.Button.BUTTON_SELECT,
        R.id.button_home to Protos.Button.BUTTON_HOME,
        R.id.button_assistant to Protos.Button.BUTTON_STADIA_ASSISTANT,
        R.id.button_screenshot to Protos.Button.BUTTON_STADIA_SCREENSHOT)

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

  private fun removeOverlay() {
    overlayView?.let { view ->
      windowManager.removeView(view)
      overlayView = null
    }
  }

  override fun onAccessibilityEvent(p0: AccessibilityEvent?) = updateState()

  private fun updateState() {
    var isGamingAppVisible = false
    val window = windows.firstOrNull { win ->
      win.type == AccessibilityWindowInfo.TYPE_APPLICATION
    } ?: return

    window.root?.let { root ->
      isGamingAppVisible = "com.google.stadia.android".contentEquals(root.packageName)
      root.recycle()
    }

    if (isGamingAppVisible) {
      when (getWindowOrientation(window)) {
        Orientation.LANDSCAPE -> addOverlay()
        Orientation.PORTRAIT -> removeOverlay()
      }
    } else if (!isGamingAppVisible) {
      removeOverlay()
    }
  }

  override fun onInterrupt() {}

  override fun onDestroy() {
    removeOverlay()
    unregisterReceiver(screenStateReceiver)
    cloverService.destroy()

    super.onDestroy()
  }
}