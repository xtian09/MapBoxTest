package com.njdc.mapkt.view

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.njdc.mapkt.R
import com.njdc.mapkt.extens.visible
import kotlinx.android.synthetic.main.search_bar.view.*

class SearchBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var listener: SearchBarListener? = null

    init {
        inflate(context, R.layout.search_bar, this)
        flSearchBarLeft.setOnClickListener {
            listener?.onBack()
        }
        etSearchBar.setOnFocusChangeListener { _, hasFocus ->
            listener?.onSearchFocus(hasFocus)
        }
        etSearchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                listener?.onTextChanged(s)
            }
        })
    }

    fun setFocus() {
        etSearchBar.requestFocus()
    }

    fun showLoading(show: Boolean) {
        pbSearchBar.visible(show)
    }

    fun showBack(show: Boolean) {
        ivSearchBarMenu.visible(!show)
        ivSearchBarBack.visible(show)
    }

    fun setSearchBarListener(listener: SearchBarListener) {
        this.listener = listener
    }

    interface SearchBarListener {
        fun onBack()
        fun onSearchFocus(hasFocus: Boolean)
        fun onTextChanged(s: CharSequence?)
    }
}
