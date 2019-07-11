package nl.folderen.android

import android.content.DialogInterface
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*


// Based on this tutorial:
// https://www.raywenderlich.com/230-introduction-to-google-maps-api-for-android-with-kotlin#toc-anchor-001

class MapsActivity :
    AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var timer: Timer
    private lateinit var timerTask: TimerTask

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Handle permissions before map is created.
        checkPermissions()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        timer = Timer()
        // timerTask = TimerTask()
        //timer.scheduleAtFixedRate(timerTask, 1000, 1000)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        map = googleMap

        val place = LatLng(52.0115205, 4.7104633)
        map.addMarker(MarkerOptions().position(place).title("My place"))
        // Zoom level 0 corresponds to the fully zoomed-out world view.
        // Most areas support zoom levels up to 20,
        // while more remote areas only support zoom levels up to 13.
        // A zoom level of 14 is a nice in-between value that shows enough detail without getting crazy-close.
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(place, 12.0f))

        map.getUiSettings().setZoomControlsEnabled(true)
        map.setOnMarkerClickListener(this)
    }

    override fun onMarkerClick(p0: Marker?) = false
    /*
    override fun onMarkerClick(p0: Marker?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    */

    private fun runTimerTask() {
        timerTask = object : TimerTask() {
            override fun run() {

                runOnUiThread { Log.d("Folderen", "Folderen") }

                runOnUiThread { printLocation() }

            }
        }
    }

    private fun printLocation() {
        Log.d ("folderen", "printLocation")
        /*
        var permissionsGranted = true
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsGranted = false
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsGranted = false
        }
        if (!permissionsGranted) {
            Log.d("Folderen", "No permissions to access device's location")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener(
            this
        ) { location ->
            if (location != null) {
                Log.d("Location", location!!.toString())
            } else {
                Log.d("Location", "is null")
            }
        }
        */
    }

    private fun checkPermissions(): Boolean {
        // https://developer.android.com/training/permissions/requesting

        // The permissions to check for whether they have been granted.
        val context = applicationContext
        lateinit var info: PackageInfo
        try {
            info = packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            // There's no package info.
            // This implies there's no permissions to request for.
            // So: Done.
            return true
        }

        val permissionsToCheck = info!!.requestedPermissions

        // Check if the permissions have been granted.
        // Collect the permissions to ask the user to grant.
        val permissionsToRequest = ArrayList<String>()
        for (permission in permissionsToCheck) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        // If all permissions have been granted by the user already: Done.
        if (permissionsToRequest.isEmpty()) {
            return true
        }

        // One or more permissions are not yet granted: Request them.
        val permissionsToRequestArray = permissionsToRequest.toArray(arrayOfNulls<String>(permissionsToRequest.size))
        ActivityCompat.requestPermissions(this, permissionsToRequestArray, 0)

        // Indicate to the caller that one or more permissions have not yet been granted.
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.d("onRequestPermissions", requestCode.toString())

        // The permissions to check for whether they have been granted by the user.
        val context = applicationContext
        lateinit var info: PackageInfo
        try {
            info = packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            // There's no package info.
            // This implies there's no permissions to check for.
            // So: Done.
            return
        }

        val permissionsToCheck = info!!.requestedPermissions

        // Check if the permissions have been granted.
        var alertUser = false
        for (permission in permissionsToCheck) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                alertUser = true
            }
        }

        // If not all permissions have been granted, alert the user.
        if (alertUser) {
            AlertDialog.Builder(this)
                .setTitle("Permissions needed")
                .setMessage("Please grant all permissions for the app to work as designed")
                .setPositiveButton("Ok", DialogInterface.OnClickListener { dialogInterface, i ->
                    val toast = Toast.makeText(
                        applicationContext,
                        "The app is running with reduced functionality",
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                })
                .create()
                .show()
        }
    }

}

