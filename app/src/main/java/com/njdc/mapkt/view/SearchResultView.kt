package com.njdc.mapkt.view

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.njdc.mapkt.R
import com.njdc.mapkt.adapter.SearchResultAdapter
import com.njdc.mapkt.extens.visible
import com.njdc.mapkt.map.InterestPoint
import kotlinx.android.synthetic.main.search_result.view.*

class SearchResultView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var listener: SearchResultListListener? = null
    private var searchSearchResultAdapter: SearchResultAdapter? = null
    private var data: List<InterestPoint>? = null

    init {
        inflate(context, R.layout.search_result, this)
        searchSearchResultAdapter = SearchResultAdapter()
        searchSearchResultAdapter!!.setOnItemClickListener { _, _, position ->
            listener!!.onSearchResult(data!![position])
        }
        rlvSearchResult.adapter = searchSearchResultAdapter
        rlvSearchResult.addItemDecoration(SearchDecoration(context))
    }

    private fun showNoResultCard() {
        cvNoResult.visible(true)
    }

    private fun hideNoResultCard() {
        cvNoResult.visible(false)
    }

    fun initData(objects: List<InterestPoint>) {
        data = objects
        searchSearchResultAdapter!!.setNewData(data)
        if (objects.isEmpty()) {
            showNoResultCard()
        } else {
            hideNoResultCard()
        }
    }

    fun setSearchResultListListener(listener: SearchResultListListener) {
        this.listener = listener
    }

    interface SearchResultListListener {
        fun onSearchResult(InterestPoint: InterestPoint)
    }
}
