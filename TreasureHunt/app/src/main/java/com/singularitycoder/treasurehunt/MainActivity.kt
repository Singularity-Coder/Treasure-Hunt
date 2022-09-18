package com.singularitycoder.treasurehunt

import android.Manifest
import android.content.Intent
import android.location.Location
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.singularitycoder.treasurehunt.databinding.ActivityMainBinding
import com.singularitycoder.treasurehunt.helpers.*
import dagger.hilt.android.AndroidEntryPoint

// Foreground service that constantly gets lat long
// It would be nice to bind that treasure in a physical location but that is not possible. so the lat long will be fixed but the device is moving constantly.
// It would also be nice to do it in AR or VR. So the treasure are not just limited to audio, video, image, text but any file. We plant it in a location and find it in AR/VR. I doubt if its possible on android though
// U dont have to handle the file formats separately. Just pass to chrome
// As soon as the lat long matches the files present in that location all the devices must automatically add them to "My Treasures" in db
// Explore only shows the files that match the lat long in real time
// For sockets u should do some sort of geo fence else getting the treasure will become next to impossible with moving targets. U wont have a problem with centralised server in this case
// Setting geo fence without google places should be possible.

// TODO foreground service with timer that pings lat long every sec

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var isRationaleDialogToBeShown = false

    private val viewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    private val tabNamesList = listOf(
        Tab.EXPLORE.value,
        Tab.MY_TREASURES.value,
    )

    private val locationPermissionResult = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isPermissionGranted: Boolean? ->
        isPermissionGranted ?: return@registerForActivityResult
        if (isPermissionGranted.not()) {
            showPermissionSettings()
            return@registerForActivityResult
        }
//        getCurrentLatLong()
        startLocationService()
    }

    private fun startLocationService() {
        viewModel.toggleLocationUpdates()
        collectLatestLifecycleFlow(flow = viewModel.playServicesAvailableState) { uiState: PlayServicesAvailableState ->
//            val locationPermissionState = LocationPermissionState(this) {
//                if (it.hasPermission()) {
//                    viewModel.toggleLocationUpdates()
//                }
//            }
            when (uiState) {
                PlayServicesAvailableState.Initializing -> {
                    binding.tvLatLong.text = getString(R.string.initializing)
                }
                PlayServicesAvailableState.PlayServicesUnavailable -> {
                    binding.tvLatLong.text = getString(R.string.play_services_unavailable)
                }
                PlayServicesAvailableState.PlayServicesAvailable -> {
                    val message = when {
//                        locationPermissionState.showDegradedExperience -> {
//                            getString(R.string.please_allow_permission)
//                        }
                        viewModel.isReceivingLocationUpdates.value -> if (viewModel.lastLocation.value != null) {
                            getString(
                                R.string.location_lat_lng,
                                viewModel.lastLocation.value!!.latitude,
                                viewModel.lastLocation.value!!.longitude
                            )
                        } else {
                            binding.tvLatLong.text = getString(R.string.waiting_for_location)
                            getString(R.string.waiting_for_location)
                        }
                        else -> {
                            getString(R.string.starting)
                        }
                    }
                    binding.tvLatLong.text = message
                }
            }
        }
        collectLatestLifecycleFlow(flow = viewModel.isReceivingLocationUpdates) { isLocationOn: Boolean ->
        }
        collectLatestLifecycleFlow(flow = viewModel.lastLocation) { lastLocation: Location? ->
//            AlertDialog.Builder(this).apply {
//                setTitle(R.string.permission_rationale_dialog_title)
//                setMessage(R.string.permission_rationale_dialog_message)
//                setPositiveButton("Ok") { dialog, which ->
//                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//                    context.startActivity(intent)
//                }
//                setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
//                show()
//            }
        }
        binding.tvLatLong.text = "12.958459, 77.662461"
    }

    private val viewPager2PageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
            super.onPageScrollStateChanged(state)
            println("viewpager2: onPageScrollStateChanged")
        }

        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            println("viewpager2: onPageSelected")
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            println("viewpager2: onPageScrolled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.setupUI()
        setUpViewPager()
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, ForegroundLocationService::class.java)
        bindService(serviceIntent, viewModel, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(viewModel)
    }

    override fun onResume() {
        super.onResume()
        grantLocationPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.viewpagerHome.unregisterOnPageChangeCallback(viewPager2PageChangeListener)
    }

    private fun ActivityMainBinding.setupUI() {
        tvLatLong.setOnClickListener {
            // Copy latlong to clipboard
            binding.root.showSnackBar("Copied location: ")
        }
    }

    private fun setUpViewPager() {
        binding.viewpagerHome.apply {
            adapter = HomeViewPagerAdapter(fragmentManager = supportFragmentManager, lifecycle = lifecycle)
            registerOnPageChangeCallback(viewPager2PageChangeListener)
        }
        binding.tabLayoutHome.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) = Unit
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
        TabLayoutMediator(binding.tabLayoutHome, binding.viewpagerHome) { tab, position ->
            tab.text = tabNamesList[position]
        }.attach()
    }


    private fun getCurrentLatLong() {
        if (isLocationPermissionGranted().not()) return
        val gpsTracker = GpsTracker(this)
        if (gpsTracker.isGPSEnabled) {
            val latitude = gpsTracker.treasureLatitude.toString()
            val longitude = gpsTracker.treasureLongitude.toString()
            val country = gpsTracker.getCountryName(this)
            val city = gpsTracker.getLocality(this)
            val postalCode = gpsTracker.getPostalCode(this)
            val addressLine = gpsTracker.getAddressLine(this)
            println(
                """
                stringLatitude: $latitude
                stringLongitude: $longitude
                country: $country
                city: $city
                postalCode: $postalCode
                addressLine: $addressLine
            """.trimIndent()
            )
            binding.tvLatLong.text = "$latitude, $longitude"
        } else {
            // If can't get location GPS or Network is not enabled. Ask user to enable GPS/network in settings
            gpsTracker.showSettingsAlert()
        }
    }

    private fun grantLocationPermissions() {
        locationPermissionResult.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }


    inner class HomeViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = tabNamesList.size
        override fun createFragment(position: Int): Fragment = TreasureFragment.newInstance(tab = tabNamesList[position])
    }
}