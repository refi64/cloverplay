package com.refi64.cloverplay

import android.graphics.PointF
import kotlin.math.*

fun PointF.scale(value: Float) {
  x *= value
  y *= value
}

fun PointF.scaled(value: Float): PointF {
  val new = PointF(x, y)
  new.scale(value)
  return new
}

class Joystick(val center: PointF, val radius: Float, val side: Side) {
  enum class Side { LEFT, RIGHT }

  var relativePosition = PointF(0.0f, 0.0f)
    private set

  val absolutePosition: PointF
    get() = PointF(relativePosition.x, relativePosition.y).apply { offset(center.x, center.y) }

  fun moveTo(abs: PointF) {
    relativePosition = PointF(abs.x, abs.y).apply {
      offset(-center.x, -center.y)

      val dist = sqrt(x * x + y * y)
      if (dist > radius) {
        scale(radius / dist)
      }
    }
  }
}
