package nl.folderen.android

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions


// The parts related to Google Maps, and to the Fused Location Client were based on this helpful tutorial:
// https://www.raywenderlich.com/230-introduction-to-google-maps-api-for-android-with-kotlin#toc-anchor-001

// API references:
// https://developers.google.com/maps/documentation/android-sdk/intro
// https://developers.google.com/maps/documentation/geocoding/intro



class MainActivity() :
    AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMapLongClickListener,
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var map: GoogleMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    private var tracingOn = false
    private lateinit var parkMarker : Marker

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Initialize the action bar drawer toggle instance.
        val drawerToggle:ActionBarDrawerToggle = object : ActionBarDrawerToggle
            (this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        {
            override fun onDrawerClosed(view:View){
                super.onDrawerClosed(view)
            }
            override fun onDrawerOpened(view: View){
                super.onDrawerOpened(view)
                val traceSwitch: Switch = findViewById(R.id.switch_trace)
                traceSwitch.setOnCheckedChangeListener { _: CompoundButton, state: Boolean -> run {
                    tracingOn = state
                }}
            }
        }

        // Configure the drawer layout to add listener and show icon on toolbar
        drawerToggle.isDrawerIndicatorEnabled = true
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Handle permissions before map is created.
        checkPermissions()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                if (tracingOn) {
                    placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
                }
            }
        }

        createLocationRequest()

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device,
     * the user will be prompted to install it inside the SupportMapFragment.
     * This method will only be triggered
     * once the user has installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.getUiSettings().setZoomControlsEnabled(true)
        map.setOnMarkerClickListener(this)
        map.setOnMapClickListener(this)
        map.setOnMapLongClickListener(this)

        setUpMap()
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.menu_trace -> {

            }
            R.id.nav_park -> {
                // If there's a marker from a previous park location, remove it.
                if (::parkMarker.isInitialized) {
                    parkMarker.remove()
                }
                // Put a bicycle marker on the map at the current location.
                if (::lastLocation.isInitialized) {
                    val latlng = LatLng(lastLocation.latitude, lastLocation.longitude)
                    val markerOptions = MarkerOptions()
                        .position(latlng)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.bicycle50))
                        .anchor(0.5f, 0.5f)
                    parkMarker = map.addMarker(markerOptions)
                }
            }
            R.id.nav_home -> {
                // Handle the camera action
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_tools -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        marker.title = "Clicked"
        return false
    }


    private fun placeMarkerOnMap(location: LatLng) {
        val markerOptions = MarkerOptions()
            .position(location)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.greendot12))
            .alpha (0.2f)
            .anchor(0.5f, 0.5f)
        map.addMarker(markerOptions)
    }


    private fun setUpMap() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Enable the my-location layer.
        // While enabled and the location is available,
        // It draws a light blue dot on the user’s location.
        // It the user walks, then this dot will also have an arrow to indicate the direction of movement.
        // It also adds a button to the map that, when tapped, centers the map on the user’s location.
        map.isMyLocationEnabled = true

        // The Android Maps API provides different map types to help you out:
        // MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE, MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID
        // MAP_TYPE_TERRAIN displays a more detailed view of the area, showing changes in elevation.
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        // Get the most recent location currently available.
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                val address = getAddress(currentLatLng)
                Log.d ("got address", address)

                    //placeMarkerOnMap(currentLatLng)
                /*
                val place = LatLng(52.0115205, 4.7104633)
                map.addMarker(MarkerOptions().position(place).title("My place"))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(place, 12.0f))
                */
                // Zoom level 0 corresponds to the fully zoomed-out world view.
                // Most areas support zoom levels up to 20,
                // while more remote areas only support zoom levels up to 13.
                // A zoom level of 14 is a nice in-between value that shows enough detail without getting crazy-close.
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
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

        val permissionsToCheck = info.requestedPermissions

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
        ActivityCompat.requestPermissions(this, permissionsToRequestArray, LOCATION_PERMISSION_REQUEST_CODE)

        // Indicate to the caller that one or more permissions have not yet been granted.
        return false
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        Log.d("onRequestPermissions", requestCode.toString())

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

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

            val permissionsToCheck = info.requestedPermissions

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
                    .setPositiveButton("Ok", DialogInterface.OnClickListener { _, _ ->
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

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            return;
        }
    }


    private fun getAddress(latLng: LatLng): String {
        // Create a Geocoder object to turn a latitude and longitude coordinate into an address and vice versa.
        val geocoder = Geocoder(this)
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try {
            // Ask the geocoder to get the address from the location passed to the method.
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            // If the response contains any address, then append it to a string and return.
            if (null != addresses && !addresses.isEmpty()) {
                address = addresses[0]
                Log.d ("folderen", address.toString())
                for (i in 0 until address.maxAddressLineIndex) {
                    addressText += if (i == 0) address.getAddressLine(i) else "\n" + address.getAddressLine(i)
                }
            }
        } catch (e: Exception) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        return addressText
    }


    private fun startLocationUpdates() {

        // Check on permissions for fine location access granted.
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Request for location updates.
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)

    }


    private fun createLocationRequest() {
        // Create an instance of LocationRequest,
        // add it to an instance of LocationSettingsRequest.Builder
        // and retrieve and handle any changes to be made
        // based on the current state of the user’s location settings.
        locationRequest = LocationRequest()
        // Specify the rate at which the app will like to receive updates.
        locationRequest.interval = 10000
        // Specify the fastest rate at which the app can handle updates.
        // Setting the fastestInterval rate places a limit on how fast updates will be sent to the app.
        locationRequest.fastestInterval = 5000

        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // Create a settings client and a task to check location settings.
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        // A task success means all is well and it can go ahead and initiate a location request.
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            // A task failure means the location settings have some issues which can be fixed.
            // This could be as a result of the user’s location settings turned off.
            // You fix this by showing the user a dialog as shown below:
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this@MainActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    // Override AppCompatActivity’s onActivityResult() method
    // and start the update request if it has a RESULT_OK result for a REQUEST_CHECK_SETTINGS request.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }


    override fun onPause() {
        super.onPause()
        Log.d ("folderen", tracingOn.toString()); // Todo
        if (!tracingOn) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }


    override fun onMapClick(point: LatLng) {
        Log.d("Map_Tag", "CLICK")
    }


    override fun onMapLongClick(point: LatLng) {
        Log.d("Map_Tag", "LONG CLICK")
    }

}