package nl.folderen.android

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color.*
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
import androidx.core.graphics.ColorUtils
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil.interpolate
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule


// The parts related to Google Maps, and to the Fused Location Client were based on this helpful tutorial:
// https://www.raywenderlich.com/230-introduction-to-google-maps-api-for-android-with-kotlin#toc-anchor-001

// API references:
// https://developers.google.com/maps/documentation/android-sdk/intro
// https://developers.google.com/maps/documentation/geocoding/intro
// https://developer.android.com/training/location/display-address
// GeoCoder requires a network connection to work.
// So that is not suitable for this app.
// http://googlemaps.github.io/android-maps-utils/javadoc/
// https://developers.google.com/maps/documentation/android-sdk/polygon-tutorial
// https://developer.android.com/reference/android/location/Location.html


class MainActivity() :
    AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener,
    OnMarkerDragListener,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMapLongClickListener,
    NavigationView.OnNavigationItemSelectedListener
{

    private lateinit var map: GoogleMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    private var tracingOn = false
    private lateinit var parkMarker : Marker

    private lateinit var farleftMarker : Marker
    private lateinit var farrightMarker : Marker
    private lateinit var nearleftMarker : Marker
    private lateinit var nearrightMarker : Marker
    private lateinit var leftcenterMarker : Marker
    private lateinit var rightcenterMarker : Marker
    private lateinit var farcenterMarker : Marker
    private lateinit var nearcenterMarker : Marker
    private lateinit var readyPolygon : Polygon

    private lateinit var cancelMarker : Marker
    private lateinit var okayMarker : Marker

    private lateinit var closeDrawerTimerTask: TimerTask

    private var polygonsOnMap : MutableList<Polygon> = mutableListOf<Polygon>()


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
                    closeDrawerDelayed();
                }}
                val showAreasReadySwitch: Switch = findViewById(R.id.switch_show_ready)
                showAreasReadySwitch.setOnCheckedChangeListener { _: CompoundButton, state: Boolean -> run {
                    toggleShowAreasReady (state)
                    closeDrawerDelayed();
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
        map.setOnMarkerDragListener(this)
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
            R.id.nav_trace -> {

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
            R.id.nav_ready -> {
                val region = map.projection.visibleRegion;

                // Create markers at the four corners of the visible region.
                // Position them slightly off the corners towards the center.
                val farleftPosition = interpolate (region.farLeft, region.nearRight, 0.1)
                val farrightPosition = interpolate (region.farRight, region.nearLeft, 0.1)
                val nearrightPosition = interpolate (region.nearRight, region.farLeft, 0.1)
                val nearleftPosition = interpolate (region.nearLeft, region.farRight, 0.1)

                // Create another four markers in-between the markers at the corners.
                // So the total number of markers will be eight all together.
                val leftcenterPosition = interpolate (farleftPosition, nearleftPosition, 0.5)
                val rightcenterPosition = interpolate (farrightPosition, nearrightPosition, 0.5)
                val farcenterPosition = interpolate (farleftPosition, farrightPosition, 0.5)
                val nearcenterPosition = interpolate (nearleftPosition, nearrightPosition, 0.5)

                var markerOptions : MarkerOptions = MarkerOptions().draggable(true)

                markerOptions.position(farleftPosition)
                farleftMarker = map.addMarker(markerOptions)
                markerOptions.position(farrightPosition)
                farrightMarker = map.addMarker(markerOptions)
                markerOptions.position(nearleftPosition)
                nearleftMarker = map.addMarker(markerOptions)
                markerOptions.position(nearrightPosition)
                nearrightMarker = map.addMarker(markerOptions)

                markerOptions.position(leftcenterPosition)
                leftcenterMarker = map.addMarker(markerOptions)
                markerOptions.position(rightcenterPosition)
                rightcenterMarker = map.addMarker(markerOptions)
                markerOptions.position(farcenterPosition)
                farcenterMarker = map.addMarker(markerOptions)
                markerOptions.position(nearcenterPosition)
                nearcenterMarker = map.addMarker(markerOptions)

                val polygonOptions = PolygonOptions()
                    .add (
                        nearcenterMarker.position,
                        nearleftMarker.position,
                        leftcenterMarker.position,
                        farleftMarker.position,
                        farcenterMarker.position,
                        farrightMarker.position,
                        rightcenterMarker.position,
                        nearrightMarker.position
                    )
                readyPolygon = map.addPolygon((polygonOptions))
                readyPolygon.setTag("alpha");

                // Put an okay button and a cancel button on the map at about the center of the screen.
                val cancelPosition = interpolate (leftcenterPosition, rightcenterPosition, 0.3)
                val okayPosition = interpolate (rightcenterPosition, leftcenterPosition, 0.3)
                markerOptions = MarkerOptions()
                    .position(cancelPosition)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.cross50))
                    .anchor(0.5f, 0.5f)
                    .alpha(0.5f)
                cancelMarker = map.addMarker(markerOptions)
                markerOptions = MarkerOptions()
                    .position(okayPosition)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.tick50))
                    .anchor(0.5f, 0.5f)
                    .alpha(0.5f)
                okayMarker = map.addMarker(markerOptions)
            }
            /*
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
            */
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }


    override fun onMarkerClick(marker: Marker): Boolean {

        // Handle the area markup cancel button.
        if (marker.equals(cancelMarker)) {
            // Remove the area.
            finalizeReadyArea (false)
            return true
        }

        // Handle the area markup accept button.
        if (marker.equals(okayMarker)) {
            // Make the area permanent.
            finalizeReadyArea((true))
            return true
        }

        // Return false to indicate that we have not consumed the event
        // and that we wish for the default behavior to occur
        // (which is for the camera to move such that the marker is centered
        // and for the marker's info window to open, if it has one).
        return false
    }


    override fun onMarkerDragStart(marker: Marker) {
    }
    override fun onMarkerDrag(marker: Marker) {
        val positions = getReadyPositions()
        readyPolygon.points = positions;

    }
    override fun onMarkerDragEnd(marker: Marker) {
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
                // Zoom level 0 corresponds to the fully zoomed-out world view.
                // Most areas support zoom levels up to 20,
                // while more remote areas only support zoom levels up to 13.
                // A zoom level of 14 is a nice in-between value that shows enough detail without getting crazy-close.
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }

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


    private fun startLocationUpdates() {

        // Check on permissions for fine location access granted.
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Request for location updates.
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

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
        if (!tracingOn) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    public override fun onResume() {
        super.onResume()
        if (!tracingOn) {
            startLocationUpdates()
        }
    }


    override fun onMapClick(point: LatLng) {
        Log.d("Map_Tag", "CLICK")
    }


    override fun onMapLongClick(point: LatLng) {
        Log.d("Map_Tag", "LONG CLICK")
    }


    private fun getCenterOfPolygon (list : List<LatLng>) : LatLng
    {
        val centroid : DoubleArray = doubleArrayOf(0.0, 0.0)
        for (point : LatLng in list) {
            centroid[0] += point.latitude;
            centroid[1] += point.longitude;
        }
        val totalPoints = list.size
        return LatLng(centroid[0] / totalPoints, centroid[1] / totalPoints)
    }


    private fun finalizeReadyArea (accept : Boolean)
    {
        // Remove the two buttons.
        cancelMarker.remove()
        okayMarker.remove()

        // Get the positions of the markers indicating the boundary of the area that is ready.
        val positions = getReadyPositions()

        // Remove all the boundary markers.
        nearcenterMarker.remove()
        nearleftMarker.remove()
        leftcenterMarker.remove()
        farleftMarker.remove()
        farcenterMarker.remove()
        farrightMarker.remove()
        rightcenterMarker.remove()
        nearrightMarker.remove()

        // Remove the boundary polygon.
        readyPolygon.remove()

        // Handle the case that the boundary is accepted by the user.
        if (accept) {

            // Draw the area on the screen.
            drawReadyBoundary((positions))

            // Store the area in the database.
            val db = FlyeringDatabaseHelper (applicationContext)
            db.saveArea(positions)
        }
    }


    private fun getReadyPositions () : List<LatLng>
    {
        val positions : List<LatLng> = listOf (
            nearcenterMarker.position,
            nearleftMarker.position,
            leftcenterMarker.position,
            farleftMarker.position,
            farcenterMarker.position,
            farrightMarker.position,
            rightcenterMarker.position,
            nearrightMarker.position
        )
        return positions
    }


    private fun drawReadyBoundary (positions : List<LatLng>)
    {
        val color = ColorUtils.blendARGB(GREEN, WHITE, 0.5f)
        var polygonOptions = PolygonOptions()
            .addAll(positions)
            .strokeColor(color)
            .fillColor(color)

        val polygon = map.addPolygon((polygonOptions))
        polygonsOnMap.add(polygon)
    }


    private fun toggleShowAreasReady (show : Boolean)
    {
        if (show) {
            val db = FlyeringDatabaseHelper (applicationContext)
            val areas = db.getAreas ()
            for (area in areas) {
                drawReadyBoundary( (area))
            }
        } else {
            for (polygon in polygonsOnMap) {
                polygon.remove ()
            }
            polygonsOnMap.clear()
        }
    }


    private fun closeDrawerDelayed ()
    {
        // If the user has toggled a switch, normally it will then close the drawer with a delay.
        // If the user then operates another toggle switch,
        // then cancel the previous delay to close the drawer.
        // This gives the user enough time to see the new state of the toggle switches.
        if (::closeDrawerTimerTask.isInitialized) {
            closeDrawerTimerTask.cancel()
        }
        // Close the drawer after a delay.
        closeDrawerTimerTask = Timer().schedule(1500) {
            this@MainActivity.runOnUiThread(java.lang.Runnable {
                val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
                drawerLayout.closeDrawer(GravityCompat.START)
            })
        }
    }

}
