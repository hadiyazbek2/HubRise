package com.example.hubrise.ui.comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hubrise.R
import com.example.hubrise.data.local.UserPreferences
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class CommentsBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var viewModel: CommentsViewModel
    private lateinit var adapter: CommentAdapter

    private lateinit var rvComments: RecyclerView
    private lateinit var etComment: EditText
    private lateinit var btnSend: ImageView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvCount: TextView

    companion object {
        private const val ARG_POST_ID = "post_id"
        private const val ARG_COMMENTS_COUNT = "comments_count"

        fun newInstance(postId: Int, commentsCount: Int = 0): CommentsBottomSheetFragment {
            return CommentsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_POST_ID, postId)
                    putInt(ARG_COMMENTS_COUNT, commentsCount)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.bottom_sheet_comments, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = arguments?.getInt(ARG_POST_ID) ?: return
        val initialCount = arguments?.getInt(ARG_COMMENTS_COUNT) ?: 0

        // Read current user ID from DataStore (needed for delete button visibility)
        val prefs = UserPreferences(requireContext())
        val currentUserId = runBlocking { prefs.userId.first() ?: -1 }

        viewModel = ViewModelProvider(this)[CommentsViewModel::class.java]

        rvComments = view.findViewById(R.id.rv_comments)
        etComment = view.findViewById(R.id.et_comment_input)
        btnSend = view.findViewById(R.id.btn_send_comment)
        pbLoading = view.findViewById(R.id.pb_comments_loading)
        tvEmpty = view.findViewById(R.id.tv_comments_empty)
        tvCount = view.findViewById(R.id.tv_comments_count)

        tvCount.text = "$initialCount comments"

        adapter = CommentAdapter(currentUserId) { comment -> viewModel.deleteComment(comment) }
        rvComments.layoutManager = LinearLayoutManager(requireContext())
        rvComments.adapter = adapter

        btnSend.setOnClickListener {
            val text = etComment.text?.toString() ?: ""
            if (text.isNotBlank()) {
                viewModel.sendComment(text)
                etComment.setText("")
            }
        }

        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            adapter.submitList(comments)
            tvEmpty.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE
            tvCount.text = "${comments.size} comments"
            if (comments.isNotEmpty()) rvComments.scrollToPosition(comments.size - 1)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.isSending.observe(viewLifecycleOwner) { sending ->
            btnSend.isEnabled = !sending
            btnSend.alpha = if (sending) 0.5f else 1f
        }

        viewModel.load(postId)
    }
}
