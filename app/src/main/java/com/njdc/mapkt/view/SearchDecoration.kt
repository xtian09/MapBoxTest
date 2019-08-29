package com.njdc.mapkt.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

import androidx.recyclerview.widget.RecyclerView

import com.njdc.mapkt.R

class SearchDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val dividerHeight: Int
    private val dividerPaddingLeft: Int
    private val dividerPaint: Paint = Paint()

    init {
        dividerPaint.color = context.resources.getColor(R.color.materialDarkGrey, null)
        dividerHeight = context.resources.getDimensionPixelSize(R.dimen.line_size)
        dividerPaddingLeft = context.resources.getDimensionPixelSize(R.dimen.line_padding_size)
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.bottom = dividerHeight
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        val left = parent.paddingLeft.plus(dividerPaddingLeft)
        val right = parent.width - parent.paddingRight

        for (i in 0 until childCount - 1) {
            val view = parent.getChildAt(i)
            val top = view.bottom.toFloat()
            val bottom = (view.bottom + dividerHeight).toFloat()
            c.drawRect(left.toFloat(), top, right.toFloat(), bottom, dividerPaint)
        }
    }
}