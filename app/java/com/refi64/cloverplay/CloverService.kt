package com.refi64.cloverplay

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PointF
import android.os.*
import android.util.Log
import android.view.MotionEvent
import android.view.View
import io.sentry.core.Sentry
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import com.topjohnwu.superuser.ipc.RootService
import java.lang.Exception
import java.lang.RuntimeException

class CloverService {
  private class Host : RootService(), Handler.Callback {
    companion object {
      const val messageEventsKey = "events"
      const val tag = "CloverServiceHost"
    }

    private lateinit var jni: CloverServiceJNI

    private var handle = 0L

    override fun onBind(intent: Intent): IBinder {
      Log.v(tag, "onBind")

      try {
        if (!this::jni.isInitialized) {
          jni = CloverServiceJNI(this)
        }

        handle = jni.create()

        val handler = Handler(Looper.getMainLooper(), this)
        return Messenger(handler).binder
      } catch (ex: Exception) {
        Sentry.captureException(ex)
        throw ex
      }
    }

    override fun handleMessage(msg: Message): Boolean {
      Log.v(tag, "handleMessage")
      var events: String? = null

      try {
        if (handle == 0L) {
          throw RuntimeException("attempted to send events without a handle")
        }

        events = msg.data.getString(messageEventsKey)!!
        jni.sendEvents(handle, events)
      } catch (ex: Exception) {
        Sentry.captureException(ex, events)
        Log.e(tag, "Exception handling events ($events)", ex)
      }

      return false
    }

    override fun onUnbind(intent: Intent): Boolean {
      Log.v(tag, "onUnbind")

      if (handle != 0L) {
        jni.destroy(handle)
        handle = 0
      }

      return super.onUnbind(intent)
    }
  }

  enum class Controller { STADIA, XBOX }

  enum class Button {
    A, B, X, Y, L1, L3, R1, R3, HOME, SELECT, START, STADIA_ASSISTANT, STADIA_SCREENSHOT
  }

  enum class DpadDirection {
    NORTH, SOUTH, EAST, WEST
  }

  enum class Trigger {
    LEFT, RIGHT
  }

  enum class Joystick { LEFT, RIGHT }

  enum class JoystickAxis { X, Y }

  interface Event

  @Serializable
  @SerialName("button")
  data class ButtonEvent(val button: Button, val pressed: Boolean) : Event

  @Serializable
  @SerialName("joystick")
  data class JoystickEvent(val joystick: Joystick, val axis: JoystickAxis, val position: Double) :
      Event

  @Serializable
  @SerialName("trigger")
  data class TriggerEvent(val trigger: Trigger, val position: Double) : Event

  @Serializable
  @SerialName("dpad")
  data class DpadEvent(val direction: DpadDirection, val pressed: Boolean) : Event

  @Serializable
  data class Request(val controller: Controller, val events: List<Event>)

  private var messenger: Messenger? = null

  private inner class Connection : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      messenger = Messenger(service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      messenger = null
    }
  }

  private val connection = Connection()

  companion object {
    private const val TAG = "CloverService"
  }

  lateinit var controller: Controller

  val started get() = messenger != null

  fun start(context: Context) {
    if (messenger == null) {
      Log.i(TAG, "Starting host service")

      val intent = Intent(context, Host::class.java)
      RootService.bind(intent, connection)
    }
  }

  private fun sendEvent(event: Event) {
    if (!started) {
      Log.e(TAG, "Can't send event to non-started service")
      return
    }

    val module = SerializersModule {
      polymorphic(Event::class) {
        ButtonEvent::class with ButtonEvent.serializer()
        JoystickEvent::class with JoystickEvent.serializer()
        TriggerEvent::class with TriggerEvent.serializer()
        DpadEvent::class with DpadEvent.serializer()
      }
    }

    val json = Json(JsonConfiguration.Stable, context = module)

    val request = Request(controller, listOf(event))
    val requestJson = json.stringify(Request.serializer(), request)

    Log.v(TAG, "Generated JSON message: $requestJson")

    val message = Message.obtain()
    message.data.putString(Host.messageEventsKey, requestJson)
    messenger!!.send(message)
  }

  private fun sendButton(button: Button, pressed: Boolean) = sendEvent(ButtonEvent(button, pressed))

  private fun sendDpad(direction: DpadDirection, pressed: Boolean) =
      sendEvent(DpadEvent(direction, pressed))

  private fun sendTrigger(trigger: Trigger, position: Double) =
      sendEvent(TriggerEvent(trigger, position))

  private fun sendJoystick(joystick: Joystick, axis: JoystickAxis, position: Double) =
      sendEvent(JoystickEvent(joystick, axis, position))

  @SuppressLint("ClickableViewAccessibility")
  private fun createPressableTouchListener(
      handler: (pressed: Boolean) -> Unit): View.OnTouchListener {
    return View.OnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> handler(true)
        MotionEvent.ACTION_UP -> handler(false)
      }

      false
    }
  }

  fun createButtonTouchListener(button: Button): View.OnTouchListener {
    return createPressableTouchListener { pressed -> sendButton(button, pressed) }
  }

  fun createDpadTouchListener(direction: DpadDirection): View.OnTouchListener {
    return createPressableTouchListener { pressed -> sendDpad(direction, pressed) }
  }

  fun createTriggerTouchListener(trigger: Trigger): View.OnTouchListener {
    return createPressableTouchListener { pressed ->
      sendTrigger(trigger, if (pressed) 1.0 else 0.0)
    }
  }

  fun createJoystickEventListener(): OnJoystickEventListener {
    return { event ->
      val joystick = when (event.joystick.side) {
        com.refi64.cloverplay.Joystick.Side.LEFT -> Joystick.LEFT
        com.refi64.cloverplay.Joystick.Side.RIGHT -> Joystick.RIGHT
      }

      val position = when (event.state) {
        com.refi64.cloverplay.JoystickEvent.State.ACTIVE -> event.joystick.relativePosition.scaled(1 / event.joystick.radius)
        com.refi64.cloverplay.JoystickEvent.State.RELEASED -> PointF(0.0f, 0.0f)
      }

      sendJoystick(joystick, JoystickAxis.X, position.x.toDouble())
      sendJoystick(joystick, JoystickAxis.Y, position.y.toDouble())
    }
  }

  fun destroy() {
    RootService.unbind(connection)
  }
}