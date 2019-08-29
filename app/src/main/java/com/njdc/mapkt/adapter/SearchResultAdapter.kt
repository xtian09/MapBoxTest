package com.njdc.mapkt.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.njdc.mapkt.R
import com.njdc.mapkt.map.InterestPoint

class SearchResultAdapter :
    BaseQuickAdapter<InterestPoint, BaseViewHolder>(R.layout.search_result_item) {

    override fun convert(helper: BaseViewHolder, item: InterestPoint?) {
        helper.setText(R.id.suggestions_item_title, item!!.text)
    }

}