package com.singularitycoder.treasurehunt

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.singularitycoder.treasurehunt.data.PlayServicesAvailabilityChecker
import com.singularitycoder.treasurehunt.databinding.ActivityMainBinding
import com.singularitycoder.treasurehunt.helpers.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


// It would be nice to bind that treasure in a physical location but that is not possible with sockets. so the lat long will be fixed but the device is moving constantly.
// It would also be nice to do it in AR or VR. So the treasure are not just limited to audio, video, image, text but any file. We plant it in a location and find it in AR/VR. I doubt if its possible on android though
// As soon as the lat long matches the files present in that location all the devices must automatically add them to "My Treasures" in db
// Explore only shows the files that match the lat long in real time
// For sockets u should do some sort of geo fence else getting the treasure will become next to impossible with moving targets. U wont have a problem with centralised server in this case
// Setting geo fence without google places should be possible.

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var playServicesAvailabilityChecker: PlayServicesAvailabilityChecker

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    private val tabNamesList = listOf(
        Tab.EXPLORE.value,
        Tab.MY_TREASURES.value,
    )

    var lastUpdatedLocation: Location? = null

    // FIXME This is not working
    private val locationToggleStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BroadcastKey.LOCATION_TOGGLE_STATUS) return
            val isLocationToggleOn = intent.getBooleanExtra(IntentKey.LOCATION_TOGGLE_STATUS, false)
            if (isLocationToggleOn.not()) {
                showLocationToggleDialog()
            }
        }
    }

    private val permissionsResult = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions: Map<String, @JvmSuppressWildcards Boolean>? ->
        permissions?.entries?.forEach { it: Map.Entry<String, @JvmSuppressWildcards Boolean> ->
            val permission = it.key
            val isGranted = it.value
            when {
                isGranted -> Unit
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> {
                    // Permission denied but not permanently, tell user why you need it. Ideally provide a button to request it again and another to dismiss
                }
                else -> {
                    // permission permanently denied. Show settings dialog
                }
            }
        }
    }

    private val locationPermissionResult = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isPermissionGranted: Boolean? ->
        isPermissionGranted ?: return@registerForActivityResult
        if (isPermissionGranted.not()) {
            val accessFineLocationNeedsRationale = shouldShowRationaleFor(Manifest.permission.ACCESS_FINE_LOCATION)
            if (accessFineLocationNeedsRationale) {
                showLocationPermissionDialog()
            }
            return@registerForActivityResult
        }
        val accessFineLocationGranted = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val isLocationToggleEnabled = isLocationToggleEnabled()
        println("isLocationToggleEnabled: $isLocationToggleEnabled")
        if (accessFineLocationGranted && isLocationToggleEnabled) {
            viewModel.toggleLocationUpdates()
        } else {
            showLocationToggleDialog()
        }
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
        binding.setUpViewPager()
        setLocationToggleListener()
        observeForData()
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, ForegroundLocationService::class.java)
        bindService(serviceIntent, viewModel, BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()
        grantLocationPermissions()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver()
    }

    override fun onStop() {
        super.onStop()
        unbindService(viewModel)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.viewpagerHome.unregisterOnPageChangeCallback(viewPager2PageChangeListener)
    }

    private fun ActivityMainBinding.setupUI() {
        tvLatLong.setOnClickListener {
            clipboard()?.text = binding.tvLatLong.text
            binding.root.showSnackBar("Copied location: ${clipboard()?.text}")
        }
    }

    private fun ActivityMainBinding.setUpViewPager() {
        viewpagerHome.apply {
            adapter = HomeViewPagerAdapter(fragmentManager = supportFragmentManager, lifecycle = lifecycle)
            registerOnPageChangeCallback(viewPager2PageChangeListener)
        }
        tabLayoutHome.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) = Unit
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
        TabLayoutMediator(tabLayoutHome, viewpagerHome) { tab, position ->
            tab.text = tabNamesList[position]
        }.attach()
    }

    // https://developer.android.com/reference/android/location/LocationManager#registerGnssStatusCallback(android.location.GnssStatus.Callback,%20android.os.Handler)
    private fun setLocationToggleListener() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            locationManager.registerGnssStatusCallback(object : GnssStatus.Callback() {
                override fun onStopped() {
                    super.onStopped()
                    showLocationToggleDialog()
                    println("location toggle: stopped")
                }

                override fun onStarted() {
                    super.onStarted()
                    println("location toggle: started")
                }

                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    super.onSatelliteStatusChanged(status)
                    println("location toggle: ${status.satelliteCount}")
                }
            }, null)
        }
    }

    private fun observeForData() {
        collectLatestLifecycleFlow(flow = viewModel.lastLocation) { lastLocation: Location? ->
            println("lastLocation: ${lastLocation?.latitude}, ${lastLocation?.longitude}")
            lastUpdatedLocation = lastLocation
            if (playServicesAvailabilityChecker.isGooglePlayServicesAvailable()) {
                binding.tvLatLong.text = if (lastLocation != null) {
                    getString(
                        R.string.location_lat_lng,
                        lastLocation.latitude,
                        lastLocation.longitude
                    )
                } else {
                    getString(R.string.waiting_for_location)
                }
            } else {
                binding.tvLatLong.text = getString(R.string.play_services_unavailable)
            }
        }
    }

    private fun grantLocationPermissions() {
        locationPermissionResult.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun registerReceiver() {
        registerReceiver(locationToggleStatusReceiver, IntentFilter(BroadcastKey.LOCATION_TOGGLE_STATUS))
    }

    private fun unregisterReceiver() {
        unregisterReceiver(locationToggleStatusReceiver)
    }

    private fun showLocationToggleDialog() {
        MaterialAlertDialogBuilder(
            this@MainActivity,
            com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog
        ).apply {
            setCancelable(false)
            setTitle("Location toggle disabled")
            setMessage("Turn on the location toggle to hunt treasures. Swipe down notifications drawer -> Location")
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.alert_dialog_bg)
            setPositiveButton("Ok") { dialog, int ->
                grantLocationPermissions()
            }
            create()
            show()
        }
    }

    private fun showLocationPermissionDialog() {
        MaterialAlertDialogBuilder(
            this@MainActivity,
            com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog
        ).apply {
            setCancelable(false)
            setTitle(R.string.permission_rationale_dialog_title)
            setMessage(R.string.permission_rationale_dialog_message)
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.alert_dialog_bg)
            setPositiveButton("Ok") { dialog, int ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            }
            setNegativeButton("Cancel") { dialog, int ->
            }
            create()
            show()
        }
    }

    inner class HomeViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = tabNamesList.size
        override fun createFragment(position: Int): Fragment = TreasureFragment.newInstance(tab = tabNamesList[position])
    }
}