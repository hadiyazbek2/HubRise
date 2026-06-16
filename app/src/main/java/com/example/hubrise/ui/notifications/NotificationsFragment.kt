package com.example.hubrise.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hubrise.R
import com.example.hubrise.data.model.NotificationItem
import com.example.hubrise.ui.profile.UserProfileFragment

class NotificationsFragment : Fragment() {

    private lateinit var viewModel: NotificationViewModel
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_notifications, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[NotificationViewModel::class.java]

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            findNavController().navigateUp()
        }

        adapter = NotificationAdapter { notification -> handleTap(notification) }

        val rv = view.findViewById<RecyclerView>(R.id.rv_notifications)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        val pbLoading = view.findViewById<ProgressBar>(R.id.pb_loading)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_empty)

        viewModel.notifications.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            rv.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.load()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun handleTap(n: NotificationItem) {
        when (n.type) {
            "follow" -> {
                val bundle = Bundle().apply { putInt(UserProfileFragment.ARG_USER_ID, n.senderId) }
                findNavController().navigate(R.id.userProfileFragment, bundle)
            }
            "like", "comment" -> {
                n.postId?.let { postId ->
                    val bundle = Bundle().apply { putInt(UserProfileFragment.ARG_USER_ID, n.senderId) }
                    findNavController().navigate(R.id.userProfileFragment, bundle)
                }
            }
        }
    }
}
