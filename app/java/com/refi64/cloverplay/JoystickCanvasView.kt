package com.refi64.cloverplay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import android.view.View
import java.util.*

class JoystickEvent(val joystick: Joystick, val state: State) {
  enum class State { ACTIVE, RELEASED }
}

typealias OnJoystickEventListener = (event: JoystickEvent) -> Unit

const val DEFAULT_RADIUS = 64.0f

class JoystickCanvasView(context: Context, attrs: AttributeSet) : View(context, attrs) {
  private val positionRadius = 16.0f
  private val joystickStrokeWidth = 2.0f

  private var size = Size(0, 0)

  private var joysticks = EnumMap<Joystick.Side, Joystick>(Joystick.Side::class.java)
  private var pointerIdsToSides = TreeMap<Int, Joystick.Side>()

  var joystickEventListener: OnJoystickEventListener? = null
  var radius = DEFAULT_RADIUS

  private val joystickPaints = arrayOf(Paint().apply {
    style = Paint.Style.FILL
    color = Color.BLACK
  }, Paint().apply {
    style = Paint.Style.STROKE
    color = Color.WHITE
  })

  private val positionPaint = Paint().apply {
    style = Paint.Style.FILL
    color = Color.WHITE
  }

  private val density: Float by lazy { resources.displayMetrics.density }

  private fun getJoystickSide(point: PointF) =
      if (point.x < size.width / 2) Joystick.Side.LEFT else Joystick.Side.RIGHT

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent?): Boolean {
    var handled = false

    when (event!!.actionMasked) {
      MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
        val index = event.actionIndex
        val id = event.getPointerId(index)
        if (id in pointerIdsToSides) {
          return false
        }

        val point = PointF(event.getX(index), event.getY(index))
        val side = getJoystickSide(point)
        if (side in joysticks) {
          return false
        }

        val joystick = Joystick(point, radius * density, side)
        joysticks[side] = joystick
        pointerIdsToSides[id] = side

        joystickEventListener?.invoke(JoystickEvent(joystick, JoystickEvent.State.ACTIVE))

        handled = true
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
        val index = event.actionIndex
        val side = pointerIdsToSides.remove(event.getPointerId(index))
        if (side == null || side !in joysticks) {
          return false
        }

        val joystick = joysticks.remove(side)!!
        joystickEventListener?.invoke(JoystickEvent(joystick, JoystickEvent.State.RELEASED))

        handled = true
      }
      MotionEvent.ACTION_MOVE -> {
        for (index in 0 until event.pointerCount) {
          val side = pointerIdsToSides[event.getPointerId(index)]
          if (side == null || side !in joysticks) {
            return false
          }

          val joystick = joysticks[side]!!
          joystick.moveTo(PointF(event.getX(index), event.getY(index)))
          joystickEventListener?.invoke(JoystickEvent(joystick, JoystickEvent.State.ACTIVE))

          handled = true
        }
      }
    }

    if (handled) {
      invalidate()
    }

    return handled
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    size = Size(w, h)

    for (joystick in joysticks.values) {
      val abs = joystick.absolutePosition
      // Scale the size (is this even necessary?).
      joystick.moveTo(PointF(w * (abs.x / oldw), h * (abs.y / oldh)))
    }
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas!!)

    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.OVERLAY)
    for (joystick in joysticks.values) {
      for (paint in joystickPaints) {
        paint.strokeWidth = joystickStrokeWidth * density

        canvas.drawCircle(joystick.center.x, joystick.center.y, joystick.radius, paint)
      }

      val abs = joystick.absolutePosition
      canvas.drawCircle(abs.x, abs.y, positionRadius * density, positionPaint)
    }
  }
}