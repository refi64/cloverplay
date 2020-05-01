package com.refi64.cloverplay

import android.content.Context
import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.io.BufferedReader
import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule

class CloverService {
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

//  @Serializable
//  sealed class Event {
//    @Serializable
//    @SerialName("button")
//    data class ButtonEvent(val button: Button, val pressed: Boolean) : Event()
//
//    @Serializable
//    @SerialName("joystick")
//    data class JoystickEvent(val joystick: Joystick, val axis: JoystickAxis, val position: Double) :
//        Event()
//
//    @Serializable
//    @SerialName("trigger")
//    data class TriggerEvent(val trigger: Trigger, val position: Double) : Event()
//
//    @Serializable
//    @SerialName("dpad")
//    data class DpadEvent(val direction: DpadDirection, val pressed: Boolean) : Event()
//  }

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
//  }

  @Serializable
  data class Request(val controller: Controller, val events: List<Event>)

  @Serializable
  data class Reply(val error: String = "")

  private var process: Process? = null
  private var processStdin: BufferedWriter? = null
  private var processStdout: BufferedReader? = null

  private var tag = "CloverService"

  lateinit var controller: Controller

  val started get() = process != null

  fun start(context: Context) {
    assert(process == null)

    val exeName = "clover_service"
    val exePath = Paths.get(context.filesDir.absolutePath, exeName)

    val serviceAsset = context.assets.open(exeName)
    Files.copy(serviceAsset, exePath, StandardCopyOption.REPLACE_EXISTING)

    exePath.toFile().setExecutable(true)

    val builder = ProcessBuilder("su", "-c", exePath.toString())
    builder.redirectError(ProcessBuilder.Redirect.INHERIT)

    process = builder.start().apply {
      processStdin = outputStream.bufferedWriter()
      processStdout = inputStream.bufferedReader()
    }
  }

  private fun sendEvent(event: Event) {
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

    Log.v(tag, "Generated JSON message: $requestJson")
    processStdin!!.apply {
      appendln(requestJson)
      flush()
    }

    val replyJson = processStdout!!.readLine()
    Log.v(tag, "JSON response: $replyJson")

    val reply = json.parse(Reply.serializer(), replyJson)
    if (reply.error.isNotEmpty()) {
      Log.e(tag, "Server returned: ${reply.error}")
    }
  }

  private fun sendButton(button: Button, pressed: Boolean) =
      sendEvent(ButtonEvent(button, pressed))

  private fun sendDpad(direction: DpadDirection, pressed: Boolean) =
      sendEvent(DpadEvent(direction, pressed))

  private fun sendTrigger(trigger: Trigger, position: Double) =
      sendEvent(TriggerEvent(trigger, position))

  private fun sendJoystick(joystick: Joystick, axis: JoystickAxis, position: Double) =
      sendEvent(JoystickEvent(joystick, axis, position))

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
    process?.apply {
      processStdin!!.apply {
        appendln("q")
        flush()
      }

      waitFor(1, TimeUnit.SECONDS)
      destroyForcibly()
    }
  }
}