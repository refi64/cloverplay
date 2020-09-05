package com.refi64.cloverplay

import android.annotation.SuppressLint
import android.graphics.Point
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import java.util.*

class MultiPressController(private val group: Set<Int>, private val root: ViewGroup) {
  companion object {
    const val TAG = "MultiPressController"
  }

  private inner class Listener(private val delegate: View.OnTouchListener? = null) :
      View.OnTouchListener {
    private val siblingsByPointer = hashMapOf<Int, List<View>>()
    private val ignoreEvents = WeakHashMap<MotionEvent, Unit>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(target: View, event: MotionEvent): Boolean {
      Log.d(TAG, "onTouch for ${target.id}: ${event.actionMasked}")

      if (event.actionButton == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
        val targetPoint = target.locationOnScreen
        val eventPoint = Point(targetPoint.x + event.x.toInt(), targetPoint.y + event.y.toInt())

        val siblings = group.map { id -> root.findViewById<View>(id) }.filter { sibling ->
          sibling != target && sibling.rectOnScreen.contains(eventPoint.x, eventPoint.y)
        }

        Log.d(TAG, "onTouch: forwarding to ${siblings.size} sibling(s)")
        siblingsByPointer[event.actionIndex] = siblings
      }

      if (ignoreEvents.putIfAbsent(event, Unit) != null) {
        Log.d(TAG, "Event was marked as ignored")
        return true
      }

      try {
        siblingsByPointer[event.actionIndex]?.forEach { sibling -> sibling.dispatchTouchEvent(event) }
      } finally {
        ignoreEvents.remove(event)

        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_POINTER_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
          siblingsByPointer.remove(event.actionIndex)
        }
      }

      delegate?.onTouch(target, event)

      return false
    }
  }

  fun getListener(delegate: View.OnTouchListener? = null): View.OnTouchListener = Listener(delegate)
}
