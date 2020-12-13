package com.apolis.wenzhao.reminderapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.apolis.wenzhao.reminderapplication.model.DataClass
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.Builder
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, View.OnClickListener {

    //Data class object
    private val dataClass: DataClass = DataClass()

    //UI Components
    private lateinit var editTextName: EditText
    private lateinit var spinner: Spinner
    private lateinit var buttonSubmit: Button
    private lateinit var buttonSetLocation: Button
    private lateinit var buttonEditFajar: Button
    private lateinit var buttonEditAsar: Button
    private lateinit var buttonEditIsha: Button
    private lateinit var buttonEditZohar: Button
    private lateinit var buttonEditMagrib: Button
    private lateinit var buttonEditJummah: Button

    //Variables for input value
    lateinit var selectedArea: String
    lateinit var currentLatitude: String
    lateinit var currentLongitude: String

    //Google Fused Location API
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var mLastLocation: Location
    private lateinit var mLocationRequest: LocationRequest

    //Constants
    companion object {
        private const val INTERVAL: Long = 2000
        private const val FASTEST_INTERVAL: Long = 1000
        private const val REQUEST_PERMISSION_LOCATION = 10
    }

    //TimePicker
    private lateinit var timePickerDialog: TimePickerDialog
    private val calendar: Calendar = Calendar.getInstance()
    private val mHour = calendar.get(Calendar.HOUR_OF_DAY)
    private val mMinute = calendar.get(Calendar.MINUTE)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpButtonClickListener()

        setUpSpinner()

        setUpLocationRequest()

    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    private fun setUpButtonClickListener() {
        buttonEditFajar = findViewById(R.id.button_edit_fajar)
        buttonEditAsar = findViewById(R.id.button_edit_asar)
        buttonEditIsha = findViewById(R.id.button_edit_isha)
        buttonEditZohar = findViewById(R.id.button_edit_zohar)
        buttonEditMagrib = findViewById(R.id.button_edit_magrib)
        buttonEditJummah = findViewById(R.id.button_edit_jummah)
        buttonSetLocation = findViewById(R.id.button_set_location)
        buttonSubmit = findViewById(R.id.button_submit)

        buttonEditFajar.setOnClickListener(this)
        buttonEditAsar.setOnClickListener(this)
        buttonEditIsha.setOnClickListener(this)
        buttonEditZohar.setOnClickListener(this)
        buttonEditMagrib.setOnClickListener(this)
        buttonEditJummah.setOnClickListener(this)
        buttonSetLocation.setOnClickListener(this)
        buttonSubmit.setOnClickListener(this)
    }

    private fun setUpSpinner() {
        spinner = findViewById<Spinner>(R.id.spinner_area)
        ArrayAdapter.createFromResource(
            this,
            R.array.area_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedArea = parent!!.getItemAtPosition(position).toString()

        Toast.makeText(parent.context, "Selected: $selectedArea", Toast.LENGTH_LONG).show()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Toast.makeText(parent?.context, "Please Select An Area", Toast.LENGTH_LONG).show()
    }

    private fun setUpLocationRequest() {
        mLocationRequest = LocationRequest()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    , 11
                )
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.cancel()
                finish()
            }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private fun startLocationUpdates() {
        // Create the location request to start receiving updates

        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = Companion.INTERVAL
        mLocationRequest.fastestInterval = FASTEST_INTERVAL

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        mFusedLocationProviderClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // do work here
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }

    fun onLocationChanged(location: Location) {
        // New location has now been determined
        mLastLocation = location

        // You can now create a LatLng Object for use with maps
        currentLatitude = mLastLocation.latitude.toString()
        currentLongitude = mLastLocation.longitude.toString()
        //Toast.makeText(this, "lat: $currentLatitude; lng: $currentLongitude", Toast.LENGTH_SHORT).show()
    }

    private fun stopLocationUpdates() {
        mFusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                goToSettings()
            }
        }
    }

    private fun goToSettings() {
        val myAppSettings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        myAppSettings.data = uri
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT)
        myAppSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivityForResult(myAppSettings, REQUEST_PERMISSION_LOCATION)
        Toast.makeText(this@MainActivity, "Go to Permissions and grant permissions.", Toast.LENGTH_LONG).show()
    }

    private fun checkPermissionForLocation(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                // Show the permission request
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSION_LOCATION
                )
                false
            }
        } else {
            true
        }

    }

    @SuppressLint("SetTextI18n")
    private fun setUpTimePickerDialog(button: Button, ) {
        timePickerDialog = TimePickerDialog(
            this,
            { view, hourOfDay, minute ->
                var h = hourOfDay.toString()
                var m = minute.toString()
                //Convert "0" to "00"
                if(hourOfDay<10) h = "0$h"
                if(minute<10) m = "0$m"
                //set text to be selected time
                button.text = "$h:$m"

            },
            mHour, mMinute, true
        )
        timePickerDialog.show()
    }

    //Handle all click events
    override fun onClick(view: View?) {
        when(view) {
            buttonEditFajar -> {
                setUpTimePickerDialog(buttonEditFajar)
            }
            buttonEditAsar -> {
                setUpTimePickerDialog(buttonEditAsar)
            }
            buttonEditIsha -> {
                setUpTimePickerDialog(buttonEditIsha)
            }
            buttonEditZohar -> {
                setUpTimePickerDialog(buttonEditZohar)
            }
            buttonEditMagrib -> {
                setUpTimePickerDialog(buttonEditMagrib)
            }
            buttonEditJummah -> {
                setUpTimePickerDialog(buttonEditJummah)
            }
            buttonSetLocation -> {
                Log.d("MYTAG", "Button Set Location Clicked")
                if (checkPermissionForLocation(this))
                    startLocationUpdates()
            }
            buttonSubmit -> {
                Log.d("MYTAG", "Button Submit clicked ")
                assignValuesToDataClassObject()
                uploadDataToDataBase()
            }
        }
    }

    private fun assignValuesToDataClassObject() {
        editTextName = findViewById(R.id.edit_text_name)
        dataClass.name = editTextName.text.toString()
        dataClass.area = selectedArea
        dataClass.latitude = currentLatitude
        dataClass.longitude = currentLongitude
        dataClass.time1 = if(buttonEditFajar.text == "Edit") "null" else buttonEditFajar.text.toString()
        dataClass.time2 = if(buttonEditAsar.text == "Edit") "null" else buttonEditAsar.text.toString()
        dataClass.time3 = if(buttonEditIsha.text == "Edit") "null" else buttonEditIsha.text.toString()
        dataClass.time4 = if(buttonEditZohar.text == "Edit") "null" else buttonEditZohar.text.toString()
        dataClass.time5 = if(buttonEditMagrib.text == "Edit") "null" else buttonEditMagrib.text.toString()
        dataClass.time6 = if(buttonEditJummah.text == "Edit") "null" else buttonEditJummah.text.toString()
    }

    private fun uploadDataToDataBase() {
        var databaseReference = FirebaseDatabase.getInstance().getReference("table")
        databaseReference.child(dataClass.name!!).setValue(dataClass)
    }
}

