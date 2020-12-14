package com.apolis.wenzhao.reminderapplication.ui

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
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.apolis.wenzhao.reminderapplication.BuildConfig
import com.apolis.wenzhao.reminderapplication.R
import com.apolis.wenzhao.reminderapplication.model.DataClass
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase
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
    private lateinit var selectedArea: String
    private lateinit var currentLatitude: String
    private lateinit var currentLongitude: String

    //Google Fused Location API
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest

    //Constants
    companion object {
        private const val INTERVAL: Long = 10*1000
        private const val FASTEST_INTERVAL: Long = 10*1000
        private const val REQUEST_PERMISSION_LOCATION = 10
    }

    //TimePicker
    private lateinit var timePickerDialog: TimePickerDialog
    private val calendar: Calendar = Calendar.getInstance()
    private val mHour = calendar.get(Calendar.HOUR_OF_DAY)
    private val mMinute = calendar.get(Calendar.MINUTE)

    //Firebase Real-time Database
    private val databaseReference = FirebaseDatabase.getInstance().getReference("table")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpButtonClickListener()

        setUpSpinner()

        checkGPSProviderEnabled()
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
        spinner = findViewById(R.id.spinner_area)
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
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}


    private fun checkGPSProviderEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes")
            { dialog, id ->
                startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 11)
            }
            .setNegativeButton("No")
            { dialog, id ->
                dialog.cancel()
                finish()
            }
        val alert: AlertDialog = builder.create()
        alert.show()
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

    private fun startLocationUpdates() {
        // Create the location request to start receiving updates
        mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = INTERVAL
        mLocationRequest.fastestInterval = FASTEST_INTERVAL

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

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
        mFusedLocationProviderClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // You can now create a LatLng Object for use with maps
            currentLatitude = locationResult.lastLocation.latitude.toString()
            currentLongitude = locationResult.lastLocation.longitude.toString()
            Toast.makeText(this@MainActivity, "Your current location has been updated!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
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
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_PERMISSION_LOCATION
                )
                false
            }
        } else {
            true
        }
    }


    @SuppressLint("SetTextI18n", "ResourceAsColor")
    private fun setUpTimePickerDialog(button: Button) { //Can determine the pickerDialog for which button
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
                button.setBackgroundColor(R.color.purple_custom)
            },
            mHour, mMinute, true
        )
        timePickerDialog.show()
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
        databaseReference.child(dataClass.name!!).setValue(dataClass)
        Toast.makeText(this, "Your data has been stored in the database!", Toast.LENGTH_LONG).show()
        goToConfirmationActivity()
    }

    private fun goToConfirmationActivity() {
        startActivity(Intent(this,ConfirmationActivity::class.java))
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
                if (checkPermissionForLocation(this))
                    startLocationUpdates()
            }
            buttonSubmit -> {
                assignValuesToDataClassObject()
                uploadDataToDataBase()
            }
        }
    }
}

