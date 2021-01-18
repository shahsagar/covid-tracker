package com.sagar.assignment1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sagar.assignment1.APIClient.client
import com.sagar.assignment1.AppDatabase.Companion.DATABASE_NAME
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class SymptomsActivity : AppCompatActivity() {
    //database
    private var database: AppDatabase? = null
    private var signDao: SignDao? = null

    // symptom ratings
    private val symptomRatings = mutableMapOf<String, Float>()
    private var ratingBar: RatingBar? = null

    // rates
    private var respiratoryRate: Double? = 0.0
    private var heartRate: Double? = 0.0

    // location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val PERMISSION_ID = 1010
    private var location: Location? = null

    lateinit var apiInterface: APIInterface

    companion object {
        private val TAG = SymptomsActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resources.getStringArray(R.array.symptoms_array).forEach { symptomRatings[it] = 0f }

        heartRate = intent.extras?.getDouble("heartRate")
        respiratoryRate = intent.extras?.getDouble("respiratoryRate")

        setContentView(R.layout.activity_symptoms)

        //database
        database = AppDatabase.getDatabase(application)
        signDao = database?.signDao()

        apiInterface = client!!.create(APIInterface::class.java)

        // Create persistent LocationManager reference
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        Log.d(TAG, "isLocationEnabled: ${checkPermission()}")
        Log.d(TAG, "checkPermission: ${isLocationEnabled()}")
        requestPermission()

        val spinner: Spinner = findViewById(R.id.spinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.symptoms_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        ratingBar = findViewById(R.id.rating_bar)
        ratingBar?.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { ratingBar, rating, fromUser ->
                val symptom = spinner.selectedItem.toString()
                symptomRatings[symptom] = rating
                Log.i(TAG, "onCreate: Updating rating of $symptom to $rating")
            }

        val button: Button = findViewById(R.id.save_data)
        button.setOnClickListener {
            Log.d(TAG, "onCreate: heartRate=$heartRate, respiratoryRate=$respiratoryRate")
            val message: String = if (heartRate != 0.0 && respiratoryRate != 0.0) {
                lifecycleScope.launch {
                    updateLastLocation()
                    signDao?.insert(
                        Sign.create(heartRate!!, respiratoryRate!!, symptomRatings, location)
                    )
                }
                "Saved to database!"
            } else "Please record both heart rate and respiratory rate!"

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        val upload: Button = findViewById(R.id.upload_db)
        upload.setOnClickListener { uploadDB() }

        signDao?.selectAll()?.observe(this, {
            Log.d(TAG, "onCreate: Symptoms has $it rows")
        })
        signDao?.latestEntry()?.observe(this, {
            Log.d(TAG, "onCreate: Latest Entry $it")
        })
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_ID
        )
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateLastLocation() {
        if (!checkPermission()) requestPermission()
        else if (!isLocationEnabled())
            Toast.makeText(
                this,
                "Please Turn on Your device Location",
                Toast.LENGTH_SHORT
            ).show()
        else if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        else
            fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                location = task.result
                Log.d(TAG, "getLastLocation: ${location?.latitude}, ${location?.longitude}")
            }
    }

    private fun uploadDB() {
        val parts: MutableList<MultipartBody.Part> = mutableListOf()

        val filenames = listOf(DATABASE_NAME, "$DATABASE_NAME-shm", "$DATABASE_NAME-wal")
        for (filename in filenames) {
            val requestFileUpload = MultipartBody.Part.createFormData(
                filename,
                filename,
                RequestBody.create(
                    MediaType.parse("file"),
                    File(getDatabasePath(filename).absolutePath)
                )
            )
            parts.add(requestFileUpload)
        }
        val description = RequestBody.create(
            MultipartBody.FORM,
            "Uploading Database to the NGinx Server..."
        )
        val call: Call<String> = apiInterface.uploadDB(description, parts)
        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String?>?, response: Response<String?>) {
                val message = "Successfully uploaded DB to Server"
                Toast.makeText(this@SymptomsActivity, message, Toast.LENGTH_LONG).show()
                Log.d(TAG, "onResponse: Response=${response.body()}")
            }

            override fun onFailure(call: Call<String?>?, t: Throwable) {
                val message = "Error in uploading DB, Reason: ${t.message}"
                Toast.makeText(this@SymptomsActivity, message, Toast.LENGTH_LONG).show()
                Log.d(TAG, "onFailure: ${t.message}")
            }
        })
    }
}