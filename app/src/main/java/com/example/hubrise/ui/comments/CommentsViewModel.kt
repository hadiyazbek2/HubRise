package com.example.hubrise.ui.comments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.Comment
import com.example.hubrise.data.repository.CommentRepository
import kotlinx.coroutines.launch

class CommentsViewModel : ViewModel() {

    private val repository = CommentRepository()

    private val _comments = MutableLiveData<List<Comment>>(emptyList())
    val comments: LiveData<List<Comment>> = _comments

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSending = MutableLiveData(false)
    val isSending: LiveData<Boolean> = _isSending

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    var postId: Int = -1

    fun load(postId: Int) {
        this.postId = postId
        viewModelScope.launch {
            _isLoading.value = true
            when (val r = repository.getComments(postId)) {
                is CommentRepository.Result.Success -> _comments.value = r.data
                is CommentRepository.Result.Error -> _error.value = r.message
            }
            _isLoading.value = false
        }
    }

    fun sendComment(content: String) {
        if (content.isBlank() || postId == -1) return
        viewModelScope.launch {
            _isSending.value = true
            when (val r = repository.createComment(postId, content.trim())) {
                is CommentRepository.Result.Success -> {
                    _comments.value = _comments.value.orEmpty() + r.data
                }
                is CommentRepository.Result.Error -> _error.value = r.message
            }
            _isSending.value = false
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch {
            when (repository.deleteComment(comment.id)) {
                is CommentRepository.Result.Success -> {
                    _comments.value = _comments.value.orEmpty().filter { it.id != comment.id }
                }
                is CommentRepository.Result.Error -> { /* silently ignore */ }
            }
        }
    }
}
