package com.refi64.cloverplay

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import com.google.android.material.button.MaterialButton

class SmallRectControllerButtonView(context: Context, attrs: AttributeSet) :
    LinearLayout(context, attrs) {
  private var text: String? = null
  private var drawable: Drawable? = null
  private var size: String? = null

  init {
    context.theme.obtainStyledAttributes(attrs, R.styleable.SmallRectControllerButtonView, 0, 0)
        .apply {
          try {
            text = getString(R.styleable.SmallRectControllerButtonView_r_text)
            drawable = getDrawable(R.styleable.SmallRectControllerButtonView_r_icon)
            size = getString(R.styleable.SmallRectControllerButtonView_r_size)
          } finally {
            recycle()
          }
        }

    val inflater = context.getSystemService(LayoutInflater::class.java)!!
    inflater.inflate(R.layout.small_rect_controller_button_view, this)
  }

  override fun setOnTouchListener(l: OnTouchListener?) {
    findViewById<MaterialButton>(R.id.button).setOnTouchListener(l)
  }

  override fun onFinishInflate() {
    super.onFinishInflate()

    if (text != null) {
      findViewById<TextView>(R.id.text).text = text
    }

    if (drawable != null) {
      findViewById<ImageView>(R.id.icon).setImageDrawable(drawable)
    }

    if (size != null) {
      val button = findViewById<MaterialButton>(R.id.button)
      val metrics = context.resources.displayMetrics

      when (size) {
        "l1" -> button.updateLayoutParams {
          width = (112.0f * metrics.density).toInt()
          height = (40.0 * metrics.density).toInt()
        }

        "l2" -> button.updateLayoutParams {
          width = (112.0f * metrics.density).toInt()
          height = (56.0 * metrics.density).toInt()
        }

        "diagonal" -> button.updateLayoutParams {
          width = (80.0f * metrics.density).toInt()
          height = (50.0f * metrics.density).toInt()
        }
      }
    }
  }
}