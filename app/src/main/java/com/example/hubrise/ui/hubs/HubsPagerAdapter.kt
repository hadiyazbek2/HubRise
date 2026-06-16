package com.example.hubrise.ui.hubs

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class HubsPagerAdapter(parent: Fragment) : FragmentStateAdapter(parent) {
    override fun getItemCount() = 2
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> MyHubsFragment()
        else -> ExploreHubsFragment()
    }
}
