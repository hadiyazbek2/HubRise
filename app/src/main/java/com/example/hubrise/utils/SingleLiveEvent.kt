package com.example.hubrise.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SingleLiveEvent<T> : MutableLiveData<T>() {

    private var pending = false

    override fun observe(owner: androidx.lifecycle.LifecycleOwner, observer: androidx.lifecycle.Observer<in T>) {
        super.observe(owner) { t ->
            if (pending) {
                pending = false
                observer.onChanged(t)
            }
        }
    }

    override fun setValue(value: T?) {
        pending = true
        super.setValue(value)
    }
}
