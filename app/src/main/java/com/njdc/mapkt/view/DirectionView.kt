package com.njdc.mapkt.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.njdc.mapkt.R
import com.njdc.mapkt.extens.visible
import com.njdc.mapkt.map.InterestPoint
import kotlinx.android.synthetic.main.search_direction.view.*

class DirectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var listener: SearchDirectionListener? = null
    private var fromInterestPoint: InterestPoint? = null
    private var toInterestPoint: InterestPoint? = null

    init {
        inflate(context, R.layout.search_direction, this)
        ivDirectionBack.setOnClickListener {
            backClick()
            listener?.onBackClick()
        }
        tvFrom.setOnClickListener {
            listener?.onFromSearch()
        }
        tvTo.setOnClickListener {
            listener?.onToSearch()
        }
        ivDirectionSwap.setOnClickListener {
            listener?.onRouteClick(fromInterestPoint!!, toInterestPoint!!)
        }
    }

    fun backClick() {
        fromInterestPoint = null
        toInterestPoint = null
        tvTo.text = ""
        tvFrom.text = ""
        listener?.onBackClick()
    }

    fun showProcess(show: Boolean) {
        pbDirection.visible(show)
    }

    fun setFormDirection(fromInterestPoint: InterestPoint) {
        this.fromInterestPoint = fromInterestPoint
        tvFrom.text = fromInterestPoint.text
        toInterestPoint?.let {
            ivDirectionSwap.visible(true)
        } ?: let {
            ivDirectionSwap.visibility = View.INVISIBLE
        }
    }

    fun setToDirection(toInterestPoint: InterestPoint) {
        this.toInterestPoint = toInterestPoint
        tvTo.text = toInterestPoint.text
        fromInterestPoint?.let {
            ivDirectionSwap.visible(true)
        } ?: let {
            ivDirectionSwap.visibility = View.INVISIBLE
        }
    }

    fun setSearchDirectionListener(listener: SearchDirectionListener) {
        this.listener = listener
    }

    interface SearchDirectionListener {
        fun onFromSearch()
        fun onToSearch()
        fun onBackClick()
        fun onRouteClick(from: InterestPoint, to: InterestPoint)
    }
}