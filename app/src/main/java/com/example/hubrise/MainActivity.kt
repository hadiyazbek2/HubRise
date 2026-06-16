package com.example.hubrise

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.local.UserPreferences
import com.example.hubrise.ui.auth.login.LoginActivity
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    // Nav item containers
    private lateinit var navHome: LinearLayout
    private lateinit var navExplore: LinearLayout
    private lateinit var navCreate: LinearLayout
    private lateinit var navHubs: LinearLayout
    private lateinit var navProfile: LinearLayout

    // Icons
    private lateinit var icHome: ImageView
    private lateinit var icExplore: ImageView
    private lateinit var icHubs: ImageView
    private lateinit var icProfile: ImageView

    // Labels
    private lateinit var labelHome: TextView
    private lateinit var labelExplore: TextView
    private lateinit var labelHubs: TextView
    private lateinit var labelProfile: TextView

    // Map destination ID → (icon, label, nav container)
    private data class NavItem(val icon: ImageView, val label: TextView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RetrofitClient.init(this)

        val userPreferences = UserPreferences(this)
        val hasToken = try {
            runBlocking { userPreferences.hasToken() }
        } catch (e: Exception) {
            false  // On any DataStore error, treat as logged-out
        }
        if (!hasToken) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        bindViews()
        setupClickListeners()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateSelection(destination.id)
        }
    }

    private fun bindViews() {
        navHome = findViewById(R.id.nav_home)
        navExplore = findViewById(R.id.nav_explore)
        navCreate = findViewById(R.id.nav_create)
        navHubs = findViewById(R.id.nav_hubs)
        navProfile = findViewById(R.id.nav_profile)

        icHome = findViewById(R.id.ic_home)
        icExplore = findViewById(R.id.ic_explore)
        icHubs = findViewById(R.id.ic_hubs)
        icProfile = findViewById(R.id.ic_profile)

        labelHome = findViewById(R.id.label_home)
        labelExplore = findViewById(R.id.label_explore)
        labelHubs = findViewById(R.id.label_hubs)
        labelProfile = findViewById(R.id.label_profile)
    }

    private fun setupClickListeners() {
        navHome.setOnClickListener { navController.navigate(R.id.homeFragment) }
        navExplore.setOnClickListener { navController.navigate(R.id.exploreFragment) }
        navCreate.setOnClickListener { navController.navigate(R.id.createPostFragment) }
        navHubs.setOnClickListener { navController.navigate(R.id.hubsFragment) }
        navProfile.setOnClickListener { navController.navigate(R.id.profileFragment) }
    }

    private fun updateSelection(destinationId: Int) {
        val blue = ContextCompat.getColor(this, R.color.blue_primary)
        val gray = ContextCompat.getColor(this, R.color.text_secondary)

        val items = mapOf(
            R.id.homeFragment to NavItem(icHome, labelHome),
            R.id.exploreFragment to NavItem(icExplore, labelExplore),
            R.id.hubsFragment to NavItem(icHubs, labelHubs),
            R.id.profileFragment to NavItem(icProfile, labelProfile)
        )

        items.forEach { (id, item) ->
            val selected = destinationId == id
            val color = if (selected) blue else gray
            item.icon.setColorFilter(color)
            item.label.setTextColor(color)
        }
    }
}
