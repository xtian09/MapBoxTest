package com.njdc.mapkt.view

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.njdc.mapkt.R
import com.njdc.mapkt.extens.formatFloorName

class FloorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    private var viewSize: Int

    private var linearLayout: LinearLayout? = null

    private var listener: FloorViewListener? = null

    init {
        isVerticalScrollBarEnabled = false
        viewSize = context.resources.getDimension(R.dimen.floor_button_size).toInt()
        linearLayout = LinearLayout(context)
        linearLayout!!.layoutParams = LinearLayout.LayoutParams(
            viewSize,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        linearLayout!!.orientation = LinearLayout.VERTICAL
        linearLayout!!.setBackgroundColor(Color.TRANSPARENT)
        linearLayout!!.setVerticalGravity(Gravity.BOTTOM)
        linearLayout!!.layoutTransition = LayoutTransition()
        layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        linearLayout!!.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        addView(linearLayout)
    }

    private fun onFloorChange(floor: Double) {
        for (i in 0 until linearLayout!!.childCount) {
            val tv = linearLayout!!.getChildAt(i) as TextView
            val tvValue = tv.getTag(R.id.tag_key)
            if (floor == tvValue) {
                tv.setBackgroundResource(R.drawable.ic_floor_selected)
                listener?.onFloorChange(tvValue)
            } else {
                tv.setBackgroundResource(R.drawable.ic_floor_normal)
            }
        }
    }

    fun onFloorsChange(floors: List<Double>) {
        linearLayout!!.removeAllViews()
        for (value in floors) {
            val b = TextView(context)
            val params = LinearLayout.LayoutParams(
                viewSize, viewSize
            )
            params.setMargins(0, 5, 0, 5)
            b.elevation = 4f
            b.layoutParams = params
            b.text = formatFloorName(value.toString())
            b.setTag(R.id.tag_key, value)
            b.gravity = Gravity.CENTER
            b.setBackgroundResource(R.drawable.ic_floor_normal)
            b.setOnClickListener {
                onFloorChange(value)
            }
            linearLayout!!.addView(b)
        }
        onFloorChange(floors.last())
    }

    fun setFloorViewListener(listener: FloorViewListener) {
        this.listener = listener
    }

    interface FloorViewListener {

        fun onFloorChange(floor: Double)
    }

}