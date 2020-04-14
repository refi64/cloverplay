package com.refi64.cloverplay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RoundControllerButtonView(context: Context, attrs: AttributeSet) :
    LinearLayout(context, attrs) {
  private var letter: String? = null
  private var drawable: Drawable? = null
  private var mini: Boolean = false
  private var flipY: Boolean = false

  init {
    context.theme.obtainStyledAttributes(attrs, R.styleable.RoundControllerButtonView, 0, 0).apply {
      try {
        letter = getString(R.styleable.RoundControllerButtonView_letter)
        drawable = getDrawable(R.styleable.RoundControllerButtonView_b_icon)
        mini = getBoolean(R.styleable.RoundControllerButtonView_mini, false)
        flipY = getBoolean(R.styleable.RoundControllerButtonView_b_flipY, false)
      } finally {
        recycle()
      }
    }

    val inflater = context.getSystemService(LayoutInflater::class.java)!!
    inflater.inflate(R.layout.round_controller_button_view, this)
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun setOnTouchListener(l: OnTouchListener?) {
    findViewById<FloatingActionButton>(R.id.button).setOnTouchListener(l)
  }

  override fun setOnLongClickListener(l: OnLongClickListener?) {
    findViewById<FloatingActionButton>(R.id.button).setOnLongClickListener(l)
  }

  override fun onFinishInflate() {
    super.onFinishInflate()

    if (letter != null) {
      findViewById<TextView>(R.id.text).text = letter
    }

    if (drawable != null) {
      findViewById<ImageView>(R.id.icon).setImageDrawable(drawable)
    }

    if (mini) {
      findViewById<FloatingActionButton>(R.id.button).size = FloatingActionButton.SIZE_MINI
    }

    if (flipY) {
      findViewById<ImageView>(R.id.icon).scaleY = -1.0f
    }
  }
}