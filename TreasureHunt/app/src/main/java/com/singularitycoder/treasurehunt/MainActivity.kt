package com.singularitycoder.treasurehunt

import android.Manifest
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.singularitycoder.treasurehunt.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


// Foreground service that constantly gets lat long
// It would be nice to bind that treasure in a physical location but that is not possible. so the lat long will be fixed but the device is moving constantly.
// It would also be nice to do it in AR or VR. So the treasure are not just limited to audio, video, image, text but any file. We plant it in a location and find it in AR/VR. I doubt if its possible on android though
// U dont have to handle the file formats separately. Just pass to chrome

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

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
        getCurrentLatLong()
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
        if (hasLocationPermission().not()) return
        val gpsTracker = GpsTracker(this)
        if (gpsTracker.isGPSEnabled) {
            val stringLatitude = gpsTracker.treasureLatitude.toString()
            val stringLongitude = gpsTracker.treasureLongitude.toString()
            val country = gpsTracker.getCountryName(this)
            val city = gpsTracker.getLocality(this)
            val postalCode = gpsTracker.getPostalCode(this)
            val addressLine = gpsTracker.getAddressLine(this)
            println("""
                stringLatitude: $stringLatitude
                stringLongitude: $stringLongitude
                country: $country
                city: $city
                postalCode: $postalCode
                addressLine: $addressLine
            """.trimIndent())
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