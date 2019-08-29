package com.njdc.mapkt.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.core.app.ActivityCompat
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.expressions.Expression.FormatOption.formatFontScale
import com.mapbox.mapboxsdk.style.expressions.Expression.FormatOption.formatTextColor
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM
import com.mapbox.mapboxsdk.style.layers.Property.TEXT_ANCHOR_CENTER
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.mapbox.turf.TurfJoins
import com.njdc.mapkt.BuildConfig
import com.njdc.mapkt.R
import com.njdc.mapkt.base.BaseActivity
import com.njdc.mapkt.base.RxBus
import com.njdc.mapkt.event.Poi
import com.njdc.mapkt.extens.*
import com.njdc.mapkt.map.BuildingInfo
import com.njdc.mapkt.map.InterestPoint
import com.njdc.mapkt.map.MapManager
import com.njdc.mapkt.map.NavigationSimulation
import com.njdc.mapkt.view.DirectionView
import com.njdc.mapkt.view.FloorView
import com.njdc.mapkt.view.SearchBarView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Field
import java.util.*

class MainActivity : BaseActivity() {

    companion object {
        // DataBase
        private const val PERMISSION_RequestCode_MapManager = 1
        private val PERMISSIONS_RequestExternalStorage = arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )
        // MapBox
        private const val BUILDING_SOURCE_ID = "building_source_id"
        private const val SYMBOL_SOURCE_ID = "symbol_source_id"
        private const val ROUTE_SOURCE_ID = "route_source_id"
        private const val ROUTE_POINT_SOURCE_ID = "route_point_source_id"
        private const val BUILDING_FILL_ID = "building_fill_id"
        private const val BUILDING_LINE_ID = "building_line_id"
        private const val BUILDING_SYMBOL_ID = "building_symbol_id"
        private const val ROUTE_LINE_ID = "route_line_id"
        private const val ROUTE_POINT_ID = "route_point_id"
    }

    // DataBase
    private val dbDir =
        Environment.getExternalStorageDirectory().toString() + File.separator + "IDMap" + File.separator
    private val dbName = "2f2.sqlite"
    private val dbPath: String = dbDir + File.separator + dbName
    private var mapManager: MapManager? = null

    // MapBox
    private var boundaryPointList: MutableList<List<Point>>? = null
    private var buildingSource: GeoJsonSource? = null
    private var symbolSource: GeoJsonSource? = null
    private var routeSource: GeoJsonSource? = null
    private var routePointSource: GeoJsonSource? = null
    private var buildingFillLayer: FillLayer? = null
    private var buildingLineLayer: LineLayer? = null
    private var buildingSymbolLayer: SymbolLayer? = null
    private var routeLineLayer: LineLayer? = null
    private var routePointLayer: SymbolLayer? = null

    // Observable
    private var mapObservable = ReplaySubject.create<Style>()
    private var pointObservable = PublishSubject.create<Point>()
    private var navi: NavigationSimulation? = null
    private var path: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            //Timber.plant(Timber.DebugTree())
        }

        mapView.onCreate(savedInstanceState)
        initMapManager()
    }

    private fun loadAll() {
        updatePoint()
        initListener()
        initLayer()
        initBuilding()
        initBuildingBoundary()
        initMapStyle()
    }

    /**
     * add Listener
     */
    private fun initListener() {
        sbvMain.setSearchBarListener(object : SearchBarView.SearchBarListener {
            override fun onBack() {

            }

            override fun onSearchFocus(hasFocus: Boolean) {
                if (hasFocus) {
                    showDirectionUI()
                }
            }

            override fun onTextChanged(s: CharSequence?) {

            }
        })

        dvMain.setSearchDirectionListener(object : DirectionView.SearchDirectionListener {
            override fun onFromSearch() {
                startActivity(
                    Intent(this@MainActivity, SearchActivity::class.java).putExtra(
                        "Type",
                        SearchActivity.FromSearch
                    )
                )
            }

            override fun onToSearch() {
                startActivity(
                    Intent(this@MainActivity, SearchActivity::class.java).putExtra(
                        "Type",
                        SearchActivity.ToSearch
                    )
                )
            }

            override fun onBackClick() {
                showDefaultUi()
            }

            override fun onRouteClick(from: InterestPoint, to: InterestPoint) {
                dvMain.showProcess(true)
                Observable.create<String> { emit ->
                    mapManager?.let {
                        val json = it.searchPath(from.coordinate, to.coordinate)
                        if (json.isNullOrEmpty()) {
                            emit.onError(Exception("null route data!"))
                        } else {
                            emit.onNext(json)
                        }
                    } ?: emit.onError(Exception("route database error!"))
                }.async()
                    .doOnNext {
                        dvMain.showProcess(false)
                        path = it
                        routeSource!!.setGeoJson(it)
                    }.doOnError {
                        dvMain.showProcess(false)
                    }.flatMap {
                        mapObservable
                    }.bindLifeCycle(this@MainActivity).subscribe({ style ->
                        if (navi != null) {
                            navi!!.stop()
                        } else {
                            style.addSource(routeSource!!)
                            style.addLayer(routeLineLayer!!)
                            style.addSource(routePointSource!!)
                            style.addLayerAbove(routePointLayer!!, BUILDING_SYMBOL_ID)
                        }
                        navi =
                            NavigationSimulation(path) { completed: Boolean, coordinate: DoubleArray ->
                                if (completed) {
                                    navi?.stop()
                                } else {
                                    pointObservable.onNext(
                                        Point.fromLngLat(
                                            coordinate[0],
                                            coordinate[1]
                                        )
                                    )
                                }
                            }
                        navi!!.start()
                    }, {
                        toast(it.toString())
                    })
            }
        })

        fvMain.setFloorViewListener(object : FloorView.FloorViewListener {
            override fun onFloorChange(floor: Double) {
                buildingFillLayer?.setFilter(eq((get("Floor")), literal(floor)))
                buildingLineLayer?.setFilter(eq((get("Floor")), literal(floor)))
                buildingSymbolLayer?.setFilter(eq((get("Floor")), literal(floor)))
            }
        })
        RxBus.getInstance().toObservable(Poi::class.java).sync().bindLifeCycle(this).subscribe {
            if (dvMain.visibility == View.VISIBLE) {
                when (it.type) {
                    SearchActivity.FromSearch -> dvMain.setFormDirection(it.interestPoint)
                    SearchActivity.ToSearch -> dvMain.setToDirection(it.interestPoint)
                    else -> {
                        toast("search result loaded")
                    }
                }
            }
        }
    }

    private fun updatePoint() {
        routeSource = GeoJsonSource(ROUTE_SOURCE_ID)
        routePointSource = GeoJsonSource(ROUTE_POINT_SOURCE_ID)
        pointObservable.async().bindLifeCycle(this@MainActivity)
            .subscribe({
                routePointSource!!.setGeoJson(it)
            }, { toast(it.toString()) })
    }

    /**
     * init all layer
     */
    private fun initLayer() {
        buildingFillLayer = FillLayer(BUILDING_FILL_ID, BUILDING_SOURCE_ID).withProperties(
            fillColor(
                interpolate(
                    exponential(1f), get("Color"),
                    stop(5, literal("#CEE9CF")),
                    stop(10, literal("#A4A2A3")),
                    stop(200, literal("#A4DEEB"))
                )
            ),
            fillOpacity(
                interpolate(
                    exponential(1f), zoom(),
                    stop(16f, 0f),
                    stop(16.5f, 0.5f),
                    stop(17f, 1f)
                )
            )
        )
        buildingLineLayer = LineLayer(BUILDING_LINE_ID, BUILDING_SOURCE_ID).withProperties(
            lineColor(Color.parseColor("#50667F")),
            lineWidth(0.5f),
            lineOpacity(
                interpolate(
                    exponential(1f), zoom(),
                    stop(16f, 0f),
                    stop(16.5f, 0.5f),
                    stop(17f, 1f)
                )
            )
        )
        val format = format(
            formatEntry(
                get("Text"),
                formatFontScale(0.8),
                formatTextColor(Color.parseColor("#50667F"))
            )
        )
        buildingSymbolLayer =
            SymbolLayer(BUILDING_SYMBOL_ID, SYMBOL_SOURCE_ID).withProperties(
                textField(format),
                textAnchor(TEXT_ANCHOR_CENTER),
                textOpacity(
                    interpolate(
                        exponential(1f), zoom(),
                        stop(16f, 0f),
                        stop(16.5f, 0.5f),
                        stop(17f, 1f)
                    )
                ),
                iconAnchor(ICON_ANCHOR_BOTTOM),
                iconSize(0.5f),
                iconOffset(arrayOf(0f, -15f)),
                iconImage("{PicId}")
            )
        routeLineLayer = LineLayer(ROUTE_LINE_ID, ROUTE_SOURCE_ID).withProperties(
            lineColor(Color.parseColor("#F74E4E")),
            lineWidth(3f)
        )
        routePointLayer = SymbolLayer(ROUTE_POINT_ID, ROUTE_POINT_SOURCE_ID).withProperties(
            iconOffset(arrayOf(0f, -15f)),
            iconImage("point"),
            iconIgnorePlacement(true),
            iconAllowOverlap(true)
        )
    }

    /**
     * request data and build map
     */
    private fun initBuilding() {
        Observable.create<BuildingInfo> { emit ->
            mapManager?.let {
                emit.onNext(it.getBuildingInfo("1"))
            } ?: emit.onError(Throwable("MapManager load BuildingInfo Error!"))
        }.async().doOnNext {
            it.geoJsonString?.let { string ->
                buildingSource = GeoJsonSource(BUILDING_SOURCE_ID, string)
            }
            it.poiJsonString?.let { string ->
                symbolSource = GeoJsonSource(SYMBOL_SOURCE_ID, string)
            }
            it.floorIds?.let { list ->
                fvMain.onFloorsChange(list.reversed())
            }
        }.doOnError {
            toast("load buildingData exception!")
        }.flatMap {
            mapObservable
        }.bindLifeCycle(this@MainActivity).subscribe({ style ->
            buildingSource?.let {
                style.addSource(it)
                style.addLayer(buildingFillLayer!!)
                style.addLayer(buildingLineLayer!!)
            }
            symbolSource?.let {
                style.addSource(it)
                style.addLayer(buildingSymbolLayer!!)
            }
        }, {
            toast("style add buildingData exception!")
        })
    }

    /**
     * set Building Boundary
     */
    private fun initBuildingBoundary() {
        val zzBoundary = ArrayList<Point>()
        zzBoundary.add(Point.fromLngLat(113.835590, 34.558662))
        zzBoundary.add(Point.fromLngLat(113.835512, 34.558344))
        zzBoundary.add(Point.fromLngLat(113.837218, 34.558338))
        zzBoundary.add(Point.fromLngLat(113.837157, 34.558033))
        boundaryPointList = ArrayList()
        boundaryPointList!!.add(zzBoundary)
    }

    /**
     * map style
     */
    private fun initMapStyle() {
        mapView.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(
                Style.Builder().fromUri(Style.LIGHT)
                    .withImage(
                        "pid140002",
                        BitmapUtils.getBitmapFromDrawable(getDrawable(R.mipmap.pid140002))!!
                    )
                    .withImage(
                        "point",
                        BitmapUtils.getBitmapFromDrawable(getDrawable(R.drawable.ic_location_on_red_24dp))!!
                    )
            )
            {
                mapObservable.onNext(it)
                hideMapboxLogo()
                mapboxMap.addOnCameraMoveListener {
                    if (mapboxMap.cameraPosition.zoom > 16) {
                        if (TurfJoins.inside(
                                Point.fromLngLat(
                                    mapboxMap.cameraPosition.target.longitude,
                                    mapboxMap.cameraPosition.target.latitude
                                ), Polygon.fromLngLats(boundaryPointList!!)
                            )
                        ) {
                            fvMain.visibility = View.VISIBLE
                        }
                    } else {
                        fvMain.visibility = View.GONE
                    }
                }
            }

            btnZLocation.setOnClickListener {
                val position = CameraPosition.Builder()
                    .target(LatLng(34.55821, 113.83627)) // Sets the new camera position
                    .zoom(17.0) // Sets the zoom
                    .bearing(360.0) // Rotate the camera
                    .tilt(60.0) // Set the camera tilt
                    .build() // Creates a CameraPosition from the builder

                mapboxMap.animateCamera(
                    CameraUpdateFactory
                        .newCameraPosition(position), 7000
                )
            }
        }
    }

    private fun showDirectionUI() {
        sbvMain.visible(false)
        dvMain.visible(true)
    }

    private fun showDefaultUi() {
        sbvMain.visible(true)
        dvMain.visible(false)
    }

    /**
     * hide Mapbox Logo
     */
    private fun hideMapboxLogo() {
        try {
            val logoFil: Field? = mapView::class.java.getDeclaredField("logoView")
            logoFil?.let {
                logoFil.isAccessible = true
                val logoView = logoFil.get(mapView!!) as View
                logoView.visible(false)
            }
            val attrFil: Field? = mapView::class.java.getDeclaredField("attrView")
            attrFil?.let {
                attrFil.isAccessible = true
                val attrView = attrFil.get(mapView!!) as View
                attrView.visible(false)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onBackPressed() {
        if (dvMain.visibility == View.VISIBLE) {
            dvMain.backClick()
            return
        }
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        mapManager?.close()
        navi?.stop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_RequestCode_MapManager) {
            var granted = true
            for (grant in grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
            }
            if (granted) {
                Timber.d("Request external storage permission granted.")
                initDatabase()
                mapManager = MapManager(dbPath)
                loadAll()
            } else {
                finish()
            }
        }
    }

    private fun initMapManager() {
        Timber.d("initMapManager")
        if (verifyStoragePermissions()) {
            initDatabase()
            mapManager = MapManager(dbPath)
            loadAll()
        }
    }

    private fun initDatabase() {
        Timber.d("initDatabase")
        val dbFile = File(dbPath)
        if (!dbFile.exists()) {
            val dbDir = File(dbDir)
            if (!dbDir.exists()) {
                val succeed = dbDir.mkdirs()
                Timber.d("Create dbDir: $succeed")
            }

            dbFile.deleteOnExit()
            Timber.d("Delete the old db file: $dbPath")

            try {
                dbFile.createNewFile()
                val inputStream = assets.open(dbName)
                val fos = FileOutputStream(dbFile)
                val buffer = ByteArray(1024)
                var count: Int = inputStream.read(buffer)
                while (count > 0) {
                    fos.write(buffer, 0, count)
                    count = inputStream.read(buffer)
                }
                inputStream.close()
                fos.close()
                Timber.d("Copy the db file")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun verifyStoragePermissions(): Boolean {
        Timber.d("verifyStoragePermissions")
        val grantResult =
            ActivityCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE")

        return if (grantResult != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_RequestExternalStorage,
                PERMISSION_RequestCode_MapManager
            )
            false
        } else {
            true
        }
    }
}
