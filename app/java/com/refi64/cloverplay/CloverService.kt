package com.refi64.cloverplay

import android.content.Context
import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.google.protobuf.util.JsonFormat
import java.io.BufferedReader
import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

class CloverService {
  private var process: Process? = null
  private var processStdin: BufferedWriter? = null
  private var processStdout: BufferedReader? = null

  private var tag = "CloverService"

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

  private fun sendRequest(event: Protos.Event) {
    val request = Protos.Request.newBuilder().apply {
      controller = Protos.Controller.STADIA
      addEvents(event)
    }.build()

    val json = JsonFormat.printer().omittingInsignificantWhitespace().print(request)
    Log.v(tag, "Generated JSON message: $json")

    processStdin!!.apply {
      appendln(json)
      flush()
    }

    val replyJson = processStdout!!.readLine()
    Log.v(tag, "JSON response: $replyJson")

    val replyBuilder = Protos.Reply.newBuilder()
    JsonFormat.parser().merge(replyJson, replyBuilder)

    val reply = replyBuilder.build()
    if (reply.error.isNotEmpty()) {
      Log.e(tag, "Server returned: ${reply.error}")
    }
  }

  private fun buildAndSendEvent(build: Protos.Event.Builder.() -> Unit) {
    sendRequest(Protos.Event.newBuilder().apply(build).build())
  }

  private fun sendButton(button: Protos.Button, pressed: Boolean) {
    buildAndSendEvent {
      buttonBuilder.let {
        it.button = button
        it.pressed = pressed
      }
    }
  }

  private fun sendDpad(direction: Protos.DpadDirection, pressed: Boolean) {
    buildAndSendEvent {
      dpadBuilder.let {
        it.direction = direction
        it.pressed = pressed
      }
    }
  }

  private fun sendTrigger(trigger: Protos.Trigger, position: Double) {
    buildAndSendEvent {
      triggerBuilder.let {
        it.trigger = trigger
        it.position = position
      }
    }
  }

  private fun sendJoystick(joystick: Protos.Joystick, axis: Protos.JoystickAxis, position: Double) {
    buildAndSendEvent {
      joystickBuilder.let {
        it.joystick = joystick
        it.axis = axis
        it.position = position
      }
    }
  }

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

  fun createButtonTouchListener(button: Protos.Button): View.OnTouchListener {
    return createPressableTouchListener { pressed -> sendButton(button, pressed) }
  }

  fun createDpadTouchListener(direction: Protos.DpadDirection): View.OnTouchListener {
    return createPressableTouchListener { pressed -> sendDpad(direction, pressed) }
  }

  fun createTriggerTouchListener(trigger: Protos.Trigger): View.OnTouchListener {
    return createPressableTouchListener { pressed ->
      sendTrigger(trigger, if (pressed) 1.0 else 0.0)
    }
  }

  fun createJoystickEventListener(): OnJoystickEventListener {
    return { event ->
      val joystick = when (event.joystick.side) {
        Joystick.Side.LEFT -> Protos.Joystick.JOYSTICK_LEFT
        Joystick.Side.RIGHT -> Protos.Joystick.JOYSTICK_RIGHT
      }

      val position = when (event.state) {
        JoystickEvent.State.ACTIVE -> event.joystick.relativePosition.scaled(1 / event.joystick.radius)
        JoystickEvent.State.RELEASED -> PointF(0.0f, 0.0f)
      }

      sendJoystick(joystick, Protos.JoystickAxis.AXIS_X, position.x.toDouble())
      sendJoystick(joystick, Protos.JoystickAxis.AXIS_Y, position.y.toDouble())
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