package com.example.hubrise.utils

import android.content.Intent
import android.net.Uri
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.hubrise.data.model.Post
import com.example.hubrise.data.repository.CommentRepository
import kotlinx.coroutines.launch

/** Mental / Physical / Gift support actions shown on a challenge-completion announcement post. */
class PostSupportHelper(private val fragment: Fragment) {

    private val repository = CommentRepository()

    fun showMentalSupportDialog(post: Post) {
        val emoji = "❤️"
        val editText = EditText(fragment.requireContext()).apply {
            hint = "Add an encouraging word (optional)"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Mental Support")
            .setMessage("Send ${post.authorUsername} some encouragement")
            .setView(editText)
            .setPositiveButton("Send $emoji") { _, _ ->
                val note = editText.text?.toString()?.trim().orEmpty()
                val content = if (note.isNotEmpty()) "$emoji $note" else emoji
                postComment(post.id, content)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showPhysicalSupportDialog(post: Post) {
        val editText = EditText(fragment.requireContext()).apply {
            hint = "What can you offer? e.g. \"I'll join your next run\""
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Physical Support")
            .setMessage("Offer ${post.authorUsername} hands-on help")
            .setView(editText)
            .setPositiveButton("Send") { _, _ ->
                val content = editText.text?.toString()?.trim().orEmpty()
                if (content.isNotEmpty()) postComment(post.id, content)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun handleGiftClick(post: Post) {
        val url = post.authorWishlistUrl?.trim().orEmpty()
        if (url.isEmpty()) {
            Toast.makeText(
                fragment.requireContext(),
                "${post.authorUsername} hasn't added a wishlist link yet",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        try {
            fragment.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(fragment.requireContext(), "Couldn't open wishlist link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun postComment(postId: Int, content: String) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            when (val r = repository.createComment(postId, content)) {
                is CommentRepository.Result.Success ->
                    Toast.makeText(fragment.requireContext(), "Sent!", Toast.LENGTH_SHORT).show()
                is CommentRepository.Result.Error ->
                    Toast.makeText(fragment.requireContext(), r.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
