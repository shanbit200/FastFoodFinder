package com.iceteaviet.fastfoodfinder.ui.main.map


import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.iceteaviet.fastfoodfinder.App
import com.iceteaviet.fastfoodfinder.R
import com.iceteaviet.fastfoodfinder.data.remote.routing.model.MapsDirection
import com.iceteaviet.fastfoodfinder.data.remote.store.model.Store
import com.iceteaviet.fastfoodfinder.ui.main.map.model.NearByStore
import com.iceteaviet.fastfoodfinder.ui.main.map.storeinfo.StoreInfoDialog
import com.iceteaviet.fastfoodfinder.ui.routing.MapRoutingActivity
import com.iceteaviet.fastfoodfinder.utils.*
import com.iceteaviet.fastfoodfinder.utils.Constant.DEFAULT_ZOOM_LEVEL
import com.iceteaviet.fastfoodfinder.utils.ui.animateMarker
import com.iceteaviet.fastfoodfinder.utils.ui.getStoreIcon
import kotlinx.android.synthetic.main.fragment_main_map.*


/**
 * Main fragment that display a map with near by stores
 */
class MainMapFragment : Fragment(), MainMapContract.View, GoogleApiClient.ConnectionCallbacks, LocationListener {
    override lateinit var presenter: MainMapContract.Presenter

    lateinit var mNearStoreRecyclerView: RecyclerView
    lateinit var mBottomSheetContainer: LinearLayout

    private var mLocationRequest: LocationRequest? = null
    private var googleMap: GoogleMap? = null

    private var mMapFragment: SupportMapFragment? = null
    private var nearByStoreAdapter: NearByStoreAdapter? = null
    private var googleApiClient: GoogleApiClient? = null


    override fun onActivityCreated(@Nullable savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mMapFragment = inflateSupportMapFragment()
    }

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_map, container, false)
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupLocationServices()
    }

    override fun onResume() {
        super.onResume()
        presenter.subscribe()
    }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
    }

    override fun onStart() {
        super.onStart()
        googleApiClient!!.connect()
    }

    override fun onStop() {
        googleApiClient!!.disconnect()
        super.onStop()
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.onLocationPermissionGranted()
                } else {
                    Toast.makeText(context!!, R.string.permission_denied, Toast.LENGTH_SHORT).show()
                }
                return
            }

            else -> {
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onConnected(@Nullable bundle: Bundle?) {
        if (isLocationPermissionGranted(context!!)) {
            presenter.onLocationPermissionGranted()
        } else {
            requestLocationPermission(this)
        }
    }

    override fun onConnectionSuspended(i: Int) {
        Toast.makeText(activity, R.string.cannot_connect_location_service, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    override fun setMyLocationEnabled(enabled: Boolean) {
        googleMap?.isMyLocationEnabled = enabled
    }

    @SuppressLint("MissingPermission")
    override fun requestLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this)
    }

    override fun animateMapCamera(location: LatLng, zoomToDetail: Boolean) {
        val zoomLevel = if (zoomToDetail) Constant.DETAILED_ZOOM_LEVEL else DEFAULT_ZOOM_LEVEL
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel))
    }

    @SuppressLint("MissingPermission")
    override fun getLastLocation() {
        val lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
        if (lastLocation != null) {
            presenter.onCurrLocationChanged(lastLocation.latitude, lastLocation.longitude)
        } else if (googleMap != null) {
            val cameraPos = googleMap!!.cameraPosition.target
            presenter.onCurrLocationChanged(cameraPos.latitude, cameraPos.longitude)
        }
    }

    override fun showWarningMessage(stringId: Int) {
        Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show()
    }

    override fun addMarkersToMap(storeList: MutableList<Store>) {
        if (googleMap == null)
            return

        // Clear old markers
        googleMap!!.clear()

        for (i in storeList.indices) {
            val store = storeList[i]
            val marker = googleMap!!.addMarker(MarkerOptions().position(store.getPosition()) // addMarker 30ms
                    .title(store.title)
                    .snippet(store.address)
                    .icon(getStoreIcon(resources, store.type, -1, -1))) // fromBitmap 25 -> 100ms
            marker.tag = store
            presenter.onMapMarkerAdd(store.id, marker)
        }
    }

    @SuppressLint("MissingPermission")
    override fun setupMap() {
        if (googleMap != null)
            return

        mMapFragment!!.getMapAsync { googleMap ->
            this.googleMap = googleMap
            googleMap.isBuildingsEnabled = true

            //Animate marker icons when camera move
            googleMap.setOnCameraMoveListener {
                presenter.onMapCameraMove(this.googleMap!!.cameraPosition.target,
                        this.googleMap!!.projection.visibleRegion.latLngBounds)
            }

            presenter.onGetMapAsync()
        }
    }

    override fun setupMapEventHandlers() {
        setMarkersListener(googleMap)
    }

    override fun showMapRoutingView(currStore: Store, mapsDirection: MapsDirection) {
        val intent = Intent(context, MapRoutingActivity::class.java)
        val extras = Bundle()
        extras.putParcelable(MapRoutingActivity.KEY_ROUTE_LIST, mapsDirection)
        extras.putParcelable(MapRoutingActivity.KEY_DES_STORE, currStore)
        intent.putExtras(extras)
        startActivity(intent)
    }

    override fun animateMapMarker(marker: Marker?, storeType: Int) {
        animateMarker(resources, marker, storeType)
    }

    override fun setNearByStores(nearbyStores: List<NearByStore>) {
        nearByStoreAdapter!!.setStores(nearbyStores)
    }

    override fun clearNearByStores() {
        nearByStoreAdapter!!.clearData()
    }

    private fun inflateSupportMapFragment(): SupportMapFragment? {
        val fragmentManager = childFragmentManager
        var fragment = fragmentManager.findFragmentById(R.id.maps_container)
        var mapFragment: SupportMapFragment?

        if (fragment === null) {
            val cameraPosition = CameraPosition.builder()
                    .target(Constant.DEFAULT_MAP_TARGET)
                    .zoom(16f)
                    .build()
            val options = GoogleMapOptions()
            options.mapType(GoogleMap.MAP_TYPE_NORMAL)
                    .camera(cameraPosition)
                    .compassEnabled(true)
                    .rotateGesturesEnabled(true)
                    .zoomGesturesEnabled(true)
                    .tiltGesturesEnabled(true)
            mapFragment = SupportMapFragment.newInstance(options)
            fragmentManager.beginTransaction().replace(R.id.map_placeholder, mapFragment as Fragment).commit() // TODO: Check
            fragmentManager.executePendingTransactions()
        } else {
            mapFragment = fragment as SupportMapFragment
        }

        return mapFragment
    }


    private fun setupUI() {
        mNearStoreRecyclerView = rv_bottom_sheet
        mBottomSheetContainer = ll_bottom_sheet

        nearByStoreAdapter = NearByStoreAdapter()

        initBottomSheet()
    }

    private fun setupLocationServices() {
        mLocationRequest = createLocationRequest()
        googleApiClient = GoogleApiClient.Builder(context!!)
                .addConnectionCallbacks(this@MainMapFragment)
                .addOnConnectionFailedListener { e(TAG, getString(R.string.cannot_get_curr_location)) }
                .addApi(LocationServices.API)
                .build()
    }


    private fun initBottomSheet() {
        BottomSheetBehavior.from(mBottomSheetContainer)
        mNearStoreRecyclerView.adapter = nearByStoreAdapter
        mNearStoreRecyclerView.layoutManager = LinearLayoutManager(context)

        nearByStoreAdapter!!.setOnStoreListListener(object : NearByStoreAdapter.StoreListListener {
            override fun onItemClick(store: Store) {
                presenter.onDirectionNavigateClick(store)
            }
        })
    }


    private fun setMarkersListener(googleMap: GoogleMap?) {
        googleMap?.setOnMarkerClickListener { marker ->
            // Handle store marker click click here
            val store = marker.tag as Store?

            if (store != null)
                showDialogStoreInfo(store)

            false
        }
    }


    override fun showDialogStoreInfo(store: Store) {
        val dialog = StoreInfoDialog.newInstance(store)
        dialog.setDialogListen(object : StoreInfoDialog.StoreDialogActionListener {
            override fun onDirection(store: Store?) {
                presenter.onDirectionNavigateClick(store!!)
            }

            override fun onAddToFavorite(storeId: Int) {
                //TODO lưu vào danh sách yêu thích
                Toast.makeText(activity, R.string.fav_stores_added, Toast.LENGTH_SHORT).show()
            }
        })
        dialog.show(activity?.supportFragmentManager!!, "dialog-info")
    }

    override fun onLocationChanged(location: Location) {
        presenter.onLocationChanged(location.latitude, location.longitude)
    }

    companion object {
        private val TAG = MainMapFragment::class.java.simpleName


        fun newInstance(): MainMapFragment {

            val args = Bundle()

            val fragment = MainMapFragment()
            fragment.arguments = args
            fragment.presenter = MainMapPresenter(App.getDataManager(), fragment)
            return fragment
        }
    }
}
