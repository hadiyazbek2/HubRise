package com.example.hubrise.ui.hubs

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hubrise.R
import com.example.hubrise.data.model.CompletionRequest
import com.google.android.material.tabs.TabLayout

class CompletionRequestsFragment : Fragment() {

    private lateinit var viewModel: CompletionRequestsViewModel
    private lateinit var adapter: CompletionRequestAdapter
    private lateinit var tabLayout: TabLayout

    private lateinit var rvRequests: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmpty: TextView

    private var hubId = 0

    private val statuses = listOf("pending", "approved", "rejected")
    private val emptyMessages = listOf(
        "No pending completion requests",
        "No approved completions yet",
        "No rejected completions",
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_completion_requests, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CompletionRequestsViewModel::class.java]

        rvRequests = view.findViewById(R.id.rv_requests)
        pbLoading = view.findViewById(R.id.pb_loading)
        tvEmpty = view.findViewById(R.id.tv_empty)
        tabLayout = view.findViewById(R.id.tab_layout)

        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().popBackStack() }

        adapter = CompletionRequestAdapter(
            onApprove = { request -> confirmApprove(request) },
            onReject = { request -> showRejectDialog(request) },
        )
        rvRequests.layoutManager = LinearLayoutManager(requireContext())
        rvRequests.adapter = adapter

        hubId = arguments?.getInt("hubId") ?: return

        tabLayout.addTab(tabLayout.newTab().setText("Pending"))
        tabLayout.addTab(tabLayout.newTab().setText("Approved"))
        tabLayout.addTab(tabLayout.newTab().setText("Rejected"))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tvEmpty.text = emptyMessages[tab.position]
                viewModel.load(hubId, statuses[tab.position])
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        tvEmpty.text = emptyMessages[0]
        viewModel.load(hubId, statuses[0])

        observeViewModel()
    }

    private fun confirmApprove(request: CompletionRequest) {
        AlertDialog.Builder(requireContext())
            .setTitle("Approve completion?")
            .setMessage("@${request.username} will be notified and an announcement will be posted in the hub feed.")
            .setPositiveButton("Approve") { _, _ -> viewModel.approve(request) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRejectDialog(request: CompletionRequest) {
        val editText = EditText(requireContext()).apply {
            hint = "Explain why (at least 20 characters)"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Reject completion")
            .setMessage("@${request.username} will see this note privately.")
            .setView(editText)
            .setPositiveButton("Reject") { _, _ ->
                val note = editText.text?.toString()?.trim().orEmpty()
                if (note.length < 20) {
                    Toast.makeText(requireContext(), "Note must be at least 20 characters", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.reject(request, note)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.requests.observe(viewLifecycleOwner) { requests ->
            adapter.submitList(requests)
            tvEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
            rvRequests.visibility = if (requests.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }
}
