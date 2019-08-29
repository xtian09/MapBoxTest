package com.njdc.mapkt.ui

import android.os.Bundle
import android.os.Environment
import com.njdc.mapkt.R
import com.njdc.mapkt.base.BaseActivity
import com.njdc.mapkt.base.RxBus
import com.njdc.mapkt.event.Poi
import com.njdc.mapkt.extens.async
import com.njdc.mapkt.extens.bindLifeCycle
import com.njdc.mapkt.extens.toast
import com.njdc.mapkt.map.InterestPoint
import com.njdc.mapkt.map.MapManager
import com.njdc.mapkt.view.SearchBarView
import com.njdc.mapkt.view.SearchResultView
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_search.*
import java.io.File

class SearchActivity : BaseActivity() {

    companion object {
        const val goSearch = 1
        const val FromSearch = 2
        const val ToSearch = 3
    }

    private val type by lazy {
        intent.getSerializableExtra("Type") as Int
    }

    // DataBase
    private val dbDir =
        Environment.getExternalStorageDirectory().toString() + File.separator + "IDMap" + File.separator
    private val dbName = "2f2.sqlite"
    private val dbPath: String = dbDir + File.separator + dbName
    private var mapManager: MapManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        mapManager = MapManager(dbPath)
        initListener()
    }

    private fun initListener() {
        sbvSearch.showBack(true)
        sbvSearch.setSearchBarListener(object : SearchBarView.SearchBarListener {

            override fun onBack() {
                finish()
            }

            override fun onSearchFocus(hasFocus: Boolean) {

            }

            override fun onTextChanged(s: CharSequence?) {
                if (s.isNullOrEmpty()) {
                    sbvSearch.showLoading(false)
                } else {
                    sbvSearch.showLoading(true)
                    getSearchResult(s.toString())
                }
            }
        })
        srvResult.setSearchResultListListener(object : SearchResultView.SearchResultListListener {
            override fun onSearchResult(InterestPoint: InterestPoint) {
                RxBus.getInstance().post(Poi(type, InterestPoint))
                finish()
            }
        })
        sbvSearch.setFocus()
    }

    private fun getSearchResult(query: String) {
        Observable.create<List<InterestPoint>> { submmit ->
            mapManager?.let {
                submmit.onNext(it.searchPoi(query))
            } ?: submmit.onError(Throwable("Query POI Error!"))
        }.async().bindLifeCycle(this).subscribe({
            sbvSearch.showLoading(false)
            srvResult.initData(it)
        }, {
            toast(it.message!!)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mapManager?.close()
    }
}