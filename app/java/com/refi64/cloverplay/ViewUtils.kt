package com.refi64.cloverplay

import android.graphics.Point
import android.graphics.Rect
import android.view.View

val View.locationOnScreen: Point get() {
  val array = IntArray(2)
  getLocationOnScreen(array)
  return Point(array[0], array[1])
}

val View.rectOnScreen: Rect get() {
  val location = locationOnScreen
  return Rect(location.x, location.y, location.x + width, location.y + height)
}
