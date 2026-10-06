package com.sosence.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {

    private var mapView: MapView? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var userMarker: Marker? = null
    private var tvStatusSub: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx)
        tvStatusSub = view.findViewById(R.id.tvMapStatusSub)

        setupMap(view)
        setupPoiChips(view)
        setupMapActionCards(view)

        view.findViewById<View>(R.id.btnMapSettings)?.setOnClickListener {
            startActivity(Intent(activity, MapSettingsActivity::class.java))
        }

        view.findViewById<View>(R.id.btnMapRecenter)?.setOnClickListener {
            fetchAndCenterLocation()
        }

        fetchAndCenterLocation()
    }

    private fun setupMap(root: View) {
        mapView = root.findViewById(R.id.osmMapView)
        mapView?.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            // Default center
            val defaultPoint = GeoPoint(13.0827, 80.2707)
            controller.setCenter(defaultPoint)
        }
    }

    private fun setupPoiChips(root: View) {
        root.findViewById<View>(R.id.chipNearbyPolice)?.setOnClickListener {
            val intent = Intent(activity, NearbyHelpActivity::class.java).apply {
                putExtra("FILTER_TYPE", "police")
            }
            startActivity(intent)
        }

        root.findViewById<View>(R.id.chipNearbyHospitals)?.setOnClickListener {
            val intent = Intent(activity, NearbyHelpActivity::class.java).apply {
                putExtra("FILTER_TYPE", "hospital")
            }
            startActivity(intent)
        }

        root.findViewById<View>(R.id.chipNearbyPharmacies)?.setOnClickListener {
            val intent = Intent(activity, NearbyHelpActivity::class.java).apply {
                putExtra("FILTER_TYPE", "pharmacy")
            }
            startActivity(intent)
        }

        root.findViewById<View>(R.id.chipNearbyFire)?.setOnClickListener {
            val intent = Intent(activity, NearbyHelpActivity::class.java).apply {
                putExtra("FILTER_TYPE", "fire")
            }
            startActivity(intent)
        }
    }

    private fun setupMapActionCards(root: View) {
        root.findViewById<View>(R.id.cardMapSafeRoutes)?.setOnClickListener {
            startActivity(Intent(activity, SafeRouteActivity::class.java))
        }

        root.findViewById<View>(R.id.cardMapShareLoc)?.setOnClickListener {
            startActivity(Intent(activity, ShareLocationActivity::class.java))
        }

        root.findViewById<View>(R.id.cardMapSafeZones)?.setOnClickListener {
            startActivity(Intent(activity, SafeMapActivity::class.java))
        }

        root.findViewById<View>(R.id.cardMapLocHistory)?.setOnClickListener {
            startActivity(Intent(activity, LocationHistoryActivity::class.java))
        }
    }

    private fun fetchAndCenterLocation() {
        val ctx = context ?: return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    ?.addOnSuccessListener { loc: Location? ->
                        if (loc != null && isAdded) {
                            val userGeo = GeoPoint(loc.latitude, loc.longitude)
                            mapView?.controller?.animateTo(userGeo)
                            tvStatusSub?.text = "GPS Fix: %.4f, %.4f".format(loc.latitude, loc.longitude)

                            mapView?.let { mv ->
                                if (userMarker == null) {
                                    userMarker = Marker(mv).apply {
                                        title = "Your Location"
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    }
                                    mv.overlays.add(userMarker)
                                }
                                userMarker?.position = userGeo
                                mv.invalidate()
                            }
                        }
                    }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        fetchAndCenterLocation()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDetach()
        mapView = null
    }
}
